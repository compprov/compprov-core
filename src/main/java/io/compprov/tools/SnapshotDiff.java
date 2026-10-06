package io.compprov.tools;

import io.compprov.core.Snapshot;
import io.compprov.core.operation.WrappedArgumentId;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.BiPredicate;

import static io.compprov.tools.SnapshotNavigator.VARIABLE_ORDER;

/**
 * Compares two {@link Snapshot} computational provenance graphs (CPGs), e.g. an original run and a what-if
 * replay produced via {@code copyWith()}/{@code compute()}, or two independent runs of the same code.
 * <p>
 * Comparison happens in two steps:
 * <ol>
 *     <li><b>Matching.</b> Root variables (not produced by any operation) are paired according to
 *     {@link Matching}. A produced variable is matched only if its operation has the same name and argument
 *     names, and every argument is itself matched to the corresponding argument on the other side — i.e. a match
 *     means "the same computation over matched inputs". Everything that cannot be matched is a structural
 *     difference.</li>
 *     <li><b>Classification.</b> Matched variables with different values are split into
 *     <i>input changes</i> (root variables), <i>propagated changes</i> (at least one argument value differs) and
 *     <i>unexplained changes</i> (all argument values are equal, but the result differs — non-determinism,
 *     a different library version, or a tampered snapshot).</li>
 * </ol>
 * Timestamps are never compared. Descriptor (name/meta) differences of matched nodes are reported separately
 * and do not affect matching, except for root variable names under {@link Matching#BY_STRUCTURE}.<br/>
 * <b>Note:</b> SnapshotDiff does not execute calculation, it compares snapshots as is.
 */
public class SnapshotDiff {

    /**
     * Strategy for pairing root variables of the two snapshots.
     */
    public enum Matching {
        /**
         * Roots are paired by variable ID. Suitable for replays ({@code copyWith()}/{@code compute()} keep IDs)
         * and for repeated single-threaded runs of the same code.
         */
        BY_ID,
        /**
         * Roots are paired by descriptor name (in order of appearance when names repeat). IDs are ignored, which
         * makes it suitable for runs where IDs shift: concurrent computations or added/removed steps.
         * Requires distinct root names: unnamed inputs are named after their ID, so they only match by ID.
         */
        BY_STRUCTURE
    }

    public record VariablePair(Snapshot.Variable left, Snapshot.Variable right) {
    }

    public record OperationPair(Snapshot.Operation left, Snapshot.Operation right) {
    }

    private record OperationStructKey(String name, List<WrappedArgumentId> arguments) {
    }

    private final SnapshotNavigator leftNav;
    private final SnapshotNavigator rightNav;
    private final Matching matching;
    private final BiPredicate<Object, Object> valuesEqual;

    private final Map<String, String> leftToRight = new HashMap<>();
    private final Map<String, String> rightToLeft = new HashMap<>();
    private final List<OperationPair> matchedOperations = new ArrayList<>();

    private final List<VariablePair> inputChanges = new ArrayList<>();
    private final List<VariablePair> propagatedChanges = new ArrayList<>();
    private final List<VariablePair> unexplainedChanges = new ArrayList<>();
    private final List<VariablePair> variableDescriptorChanges = new ArrayList<>();
    private final List<OperationPair> operationDescriptorChanges = new ArrayList<>();
    private final List<Snapshot.Variable> leftOnlyVariables;
    private final List<Snapshot.Variable> rightOnlyVariables;
    private final List<Snapshot.Operation> leftOnlyOperations;
    private final List<Snapshot.Operation> rightOnlyOperations;

    /**
     * Compares by ID, with values compared via {@link Comparable} if applicable and {@link Objects#equals} otherwise
     * (so {@code BigDecimal} scale does not matter).
     */
    public SnapshotDiff(Snapshot left, Snapshot right) {
        this(left, right, Matching.BY_ID, (o1, o2) -> {
            if ((o1 instanceof Comparable) && (o2 instanceof Comparable) && (o1.getClass() == o2.getClass())) {
                Comparable c1 = (Comparable) o1;
                Comparable c2 = (Comparable) o2;
                return c1.compareTo(c2) == 0;
            }
            return Objects.equals(o1, o2);
        });
    }

    /**
     * @param left        baseline snapshot
     * @param right       snapshot compared against the baseline
     * @param matching    how root variables are paired
     * @param valuesEqual value equality, e.g. {@code compareTo()}-based or tolerance-based for numbers
     * @throws IllegalArgumentException if either snapshot is inconsistent (see {@link SnapshotNavigator})
     */
    public SnapshotDiff(Snapshot left, Snapshot right, Matching matching, BiPredicate<Object, Object> valuesEqual) {
        this(new SnapshotNavigator(Objects.requireNonNull(left, "left")),
                new SnapshotNavigator(Objects.requireNonNull(right, "right")),
                matching, valuesEqual);
    }

    /**
     * Same as {@link #SnapshotDiff(Snapshot, Snapshot, Matching, BiPredicate)}, reusing existing navigators.
     */
    public SnapshotDiff(SnapshotNavigator left, SnapshotNavigator right, Matching matching,
                        BiPredicate<Object, Object> valuesEqual) {
        this.leftNav = Objects.requireNonNull(left, "left");
        this.rightNav = Objects.requireNonNull(right, "right");
        this.matching = Objects.requireNonNull(matching, "matching");
        this.valuesEqual = Objects.requireNonNull(valuesEqual, "valuesEqual");

        //match vars & ops
        matchRoots();
        matchOperations();

        //compare
        compareValues();

        //make sure vars and ops have counterparty equivalent (structural changes)
        leftOnlyVariables = leftNav.variables().stream()
                .filter(v -> !leftToRight.containsKey(v.track().getId()))
                .toList();
        rightOnlyVariables = rightNav.variables().stream()
                .filter(v -> !rightToLeft.containsKey(v.track().getId()))
                .toList();
        Set<String> matchedLeftOps = new HashSet<>();
        Set<String> matchedRightOps = new HashSet<>();
        matchedOperations.forEach(pair -> {
            matchedLeftOps.add(pair.left().track().getId());
            matchedRightOps.add(pair.right().track().getId());
        });
        leftOnlyOperations = leftNav.operations().stream()
                .filter(o -> !matchedLeftOps.contains(o.track().getId()))
                .toList();
        rightOnlyOperations = rightNav.operations().stream()
                .filter(o -> !matchedRightOps.contains(o.track().getId()))
                .toList();
    }

    private void matchRoots() {
        if (matching == Matching.BY_ID) {
            for (var leftRoot : leftNav.roots()) {
                final var id = leftRoot.track().getId();
                if (rightNav.variable(id).isPresent() && rightNav.producedBy(id).isEmpty()) {
                    match(id, id);
                }
            }
            return;
        }

        //left:  i_2 "rate", i_5 "rate", i_9 "rate"
        //right: i_3 "rate", i_6 "rate", i_8 "rate"   -> deque [i_3, i_6, i_8]
        Map<String, Deque<Snapshot.Variable>> rightRootsByName = new HashMap<>();
        rightNav.roots().forEach(v -> rightRootsByName
                .computeIfAbsent(v.track().getDescriptor().getName(), name -> new ArrayDeque<>())
                .add(v));
        for (var leftRoot : leftNav.roots()) {
            final var candidates = rightRootsByName.get(leftRoot.track().getDescriptor().getName());
            if (candidates != null && !candidates.isEmpty()) {
                match(leftRoot.track().getId(), candidates.poll().track().getId());
            }
        }
    }

    private void matchOperations() {
        Map<OperationStructKey, Deque<Snapshot.Operation>> rightByKey = new HashMap<>();
        if (matching == Matching.BY_STRUCTURE) {
            rightNav.operations().forEach(op -> rightByKey.computeIfAbsent(structKeyOf(op), key -> new ArrayDeque<>()).add(op));
        }

        // The navigator orders operations so that producers precede consumers: arguments are matched (or known to be
        // unmatched) before the operations consuming them
        for (var leftOp : leftNav.operations()) {
            final var key = mappedStructKeyOf(leftOp);
            if (key == null) {
                continue; // some argument has no counterpart, so this computation has none either
            }

            Snapshot.Operation rightOp;
            if (matching == Matching.BY_ID) {
                rightOp = rightNav.producedBy(leftOp.resultId())
                        .filter(op -> key.equals(structKeyOf(op)))
                        .orElse(null);
            } else {
                final var candidates = rightByKey.get(key);
                rightOp = candidates == null ? null : candidates.poll();
            }

            if (rightOp != null) {
                match(leftOp.resultId(), rightOp.resultId());
                matchedOperations.add(new OperationPair(leftOp, rightOp));
            }
        }
    }

    private void compareValues() {
        for (var leftRoot : leftNav.roots()) {
            final var rightId = leftToRight.get(leftRoot.track().getId());
            if (rightId != null && compareVariables(leftRoot, variable(rightNav, rightId))) {
                inputChanges.add(new VariablePair(leftRoot, variable(rightNav, rightId)));
            }
        }

        for (var opPair : matchedOperations) {
            if (!opPair.left().track().getDescriptor().equals(opPair.right().track().getDescriptor())) {
                operationDescriptorChanges.add(opPair);
            }

            final var leftResult = variable(leftNav, opPair.left().resultId());
            final var rightResult = variable(rightNav, opPair.right().resultId());
            if (compareVariables(leftResult, rightResult)) {
                final var pair = new VariablePair(leftResult, rightResult);
                if (anyArgumentChanged(opPair)) {
                    propagatedChanges.add(pair);
                } else {
                    unexplainedChanges.add(pair);
                }
            }
        }
    }

    /**
     * Records a descriptor change, if any, and returns {@code true} if the values differ.
     */
    private boolean compareVariables(Snapshot.Variable leftVar, Snapshot.Variable rightVar) {
        if (!leftVar.track().getDescriptor().equals(rightVar.track().getDescriptor())) {
            variableDescriptorChanges.add(new VariablePair(leftVar, rightVar));
        }
        return !valuesEqual.test(leftVar.value(), rightVar.value());
    }

    private boolean anyArgumentChanged(OperationPair opPair) {
        final var leftArgs = leftNav.arguments(opPair.left().track().getId());
        final var rightArgs = rightNav.arguments(opPair.right().track().getId());
        // Matched operations have the same argument keys, so arguments line up by position
        for (int i = 0; i < leftArgs.size(); i++) {
            if (!valuesEqual.test(leftArgs.get(i).value(), rightArgs.get(i).value())) {
                return true;
            }
        }
        return false;
    }

    private void match(String leftId, String rightId) {
        leftToRight.put(leftId, rightId);
        rightToLeft.put(rightId, leftId);
    }

    // The navigator has validated the snapshot, so every referenced variable exists
    private static Snapshot.Variable variable(SnapshotNavigator nav, String variableId) {
        return nav.variable(variableId).orElseThrow();
    }

    private static OperationStructKey structKeyOf(Snapshot.Operation op) {
        return new OperationStructKey(op.track().getDescriptor().getName(), op.arguments());
    }

    /**
     * Key of a left operation with argument IDs translated to their right counterparts, or null if any argument
     * is unmatched.
     */
    private OperationStructKey mappedStructKeyOf(Snapshot.Operation leftOp) {
        List<WrappedArgumentId> arguments = new ArrayList<>(leftOp.arguments().size());
        for (var argument : leftOp.arguments()) {
            final var rightId = leftToRight.get(argument.variableId());
            if (rightId == null) {
                return null;
            }
            arguments.add(new WrappedArgumentId(argument.key(), rightId));
        }
        return new OperationStructKey(leftOp.track().getDescriptor().getName(), arguments);
    }

    /**
     * Returns {@code true} if there are no value, descriptor or structural differences.
     */
    public boolean isIdentical() {
        return inputChanges.isEmpty() && propagatedChanges.isEmpty() && unexplainedChanges.isEmpty()
                && variableDescriptorChanges.isEmpty() && operationDescriptorChanges.isEmpty()
                && isStructurallyEqual();
    }

    /**
     * Returns {@code true} if every variable and operation has a counterpart on the other side.
     */
    public boolean isStructurallyEqual() {
        return leftOnlyVariables.isEmpty() && rightOnlyVariables.isEmpty()
                && leftOnlyOperations.isEmpty() && rightOnlyOperations.isEmpty();
    }

    /**
     * Matched root variables whose values differ — the inputs that were changed.
     */
    public List<VariablePair> inputChanges() {
        return Collections.unmodifiableList(inputChanges);
    }

    /**
     * Matched produced variables whose values differ because at least one argument value differs.
     */
    public List<VariablePair> propagatedChanges() {
        return Collections.unmodifiableList(propagatedChanges);
    }

    /**
     * Matched produced variables whose values differ although all argument values are equal.
     */
    public List<VariablePair> unexplainedChanges() {
        return Collections.unmodifiableList(unexplainedChanges);
    }

    /**
     * All matched variables with differing values: input, propagated and unexplained changes.
     */
    public List<VariablePair> valueChanges() {
        List<VariablePair> result = new ArrayList<>(inputChanges);
        result.addAll(propagatedChanges);
        result.addAll(unexplainedChanges);
        result.sort(Comparator.comparing(VariablePair::left, VARIABLE_ORDER));
        return result;
    }

    /**
     * Matched variables whose descriptors (name or meta) di1ffer.
     */
    public List<VariablePair> variableDescriptorChanges() {
        return Collections.unmodifiableList(variableDescriptorChanges);
    }

    /**
     * Matched operations whose descriptor meta differs.
     */
    public List<OperationPair> operationDescriptorChanges() {
        return Collections.unmodifiableList(operationDescriptorChanges);
    }

    public List<OperationPair> matchedOperations() {
        return Collections.unmodifiableList(matchedOperations);
    }

    public List<Snapshot.Variable> leftOnlyVariables() {
        return leftOnlyVariables;
    }

    public List<Snapshot.Variable> rightOnlyVariables() {
        return rightOnlyVariables;
    }

    public List<Snapshot.Operation> leftOnlyOperations() {
        return leftOnlyOperations;
    }

    public List<Snapshot.Operation> rightOnlyOperations() {
        return rightOnlyOperations;
    }

    /**
     * Returns the right counterpart of a left variable, or empty if it is unmatched.
     */
    public Optional<Snapshot.Variable> rightOf(String leftVariableId) {
        return Optional.ofNullable(leftToRight.get(leftVariableId)).flatMap(rightNav::variable);
    }

    /**
     * Returns the left counterpart of a right variable, or empty if it is unmatched.
     */
    public Optional<Snapshot.Variable> leftOf(String rightVariableId) {
        return Optional.ofNullable(rightToLeft.get(rightVariableId)).flatMap(leftNav::variable);
    }

    public Snapshot getLeft() {
        return leftNav.getSnapshot();
    }

    public Snapshot getRight() {
        return rightNav.getSnapshot();
    }

    /**
     * Human-readable summary. Input, unexplained and descriptor changes are listed in full; propagated changes
     * and structural differences are listed as counts, since they can be large.
     */
    public String toReport() {
        StringBuilder sb = new StringBuilder();
        sb.append("Diff: '").append(getLeft().descriptor().getName())
                .append("' vs '").append(getRight().descriptor().getName())
                .append("' (").append(matching).append(")\n");
        if (isIdentical()) {
            return sb.append("No differences\n").toString();
        }

        sb.append("Matched: ").append(leftToRight.size()).append(" variables, ")
                .append(matchedOperations.size()).append(" operations\n");
        appendPairs(sb, "Input changes", inputChanges);
        appendPairs(sb, "Unexplained changes (equal arguments, different result)", unexplainedChanges);
        sb.append("Propagated changes: ").append(propagatedChanges.size()).append('\n');
        if (!variableDescriptorChanges.isEmpty()) {
            sb.append("Variable descriptor changes:\n");
            variableDescriptorChanges.forEach(pair -> sb.append("  ")
                    .append(pair.left().track().getId()).append(": ")
                    .append(pair.left().track().getDescriptor()).append(" -> ")
                    .append(pair.right().track().getDescriptor()).append('\n'));
        }
        if (!operationDescriptorChanges.isEmpty()) {
            sb.append("Operation descriptor changes:\n");
            operationDescriptorChanges.forEach(pair -> sb.append("  ")
                    .append(pair.left().track().getId()).append(": ")
                    .append(pair.left().track().getDescriptor()).append(" -> ")
                    .append(pair.right().track().getDescriptor()).append('\n'));
        }
        sb.append("Only in left: ").append(leftOnlyVariables.size()).append(" variables, ")
                .append(leftOnlyOperations.size()).append(" operations\n");
        sb.append("Only in right: ").append(rightOnlyVariables.size()).append(" variables, ")
                .append(rightOnlyOperations.size()).append(" operations\n");
        return sb.toString();
    }

    private static void appendPairs(StringBuilder sb, String title, List<VariablePair> pairs) {
        sb.append(title).append(": ").append(pairs.size()).append('\n');
        pairs.forEach(pair -> sb.append("  ")
                .append(pair.left().track().getId()).append(" '")
                .append(pair.left().track().getDescriptor().getName()).append("': ")
                .append(pair.left().value()).append(" -> ").append(pair.right().value()).append('\n'));
    }
}
