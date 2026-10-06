package io.compprov.tools;

import io.compprov.core.Snapshot;
import io.compprov.core.meta.Descriptor;
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
import java.util.Optional;
import java.util.Set;
import java.util.function.Predicate;

/**
 * Navigates and queries a {@link Snapshot} computational provenance graph (CPG).
 * <p>
 * Index maps are built once at construction time, and the snapshot is validated: every argument and result must
 * reference an existing variable, every variable is produced by at most one operation, and an operation producing
 * an argument must have a lower operation ID than the operation consuming it.
 * <p>
 * All returned lists are ordered by numeric ID. Deep traversals are iterative and visit each node once, so they are
 * safe for long operation chains and heavily shared variables.
 */
public class SnapshotNavigator {

    static final Comparator<Snapshot.Variable> VARIABLE_ORDER =  Comparator.comparingInt(v -> v.track().getNumericId());
    static final Comparator<Snapshot.Operation> OPERATION_ORDER = Comparator.comparingInt(o -> o.track().getNumericId());

    private final Snapshot snapshot;
    private final List<Snapshot.Variable> roots;
    private final List<Snapshot.Variable> leaves;

    private final Map<String, Snapshot.Variable> variables;
    private final Map<String, Snapshot.Operation> operations;
    private final Map<String, Snapshot.Operation> producedBy;
    private final Map<String, List<Snapshot.Operation>> participatesIn;

    /**
     * Builds navigation indexes from the given snapshot.
     *
     * @throws IllegalArgumentException if the snapshot is inconsistent
     */
    public SnapshotNavigator(Snapshot snapshot) {
        this.snapshot = snapshot;
        variables = new HashMap<>();
        operations = new HashMap<>();
        producedBy = new HashMap<>();
        participatesIn = new HashMap<>();

        for (var variable : variables()) {
            if (variables.put(variable.track().getId(), variable) != null) {
                throw invalid("Duplicate variable " + variable.track().getId());
            }
        }

        for (var operation : operations()) {
            final var opId = operation.track().getId();
            if (operations.put(opId, operation) != null) {
                throw invalid("Duplicate operation " + opId);
            }
            if (!variables.containsKey(operation.resultId())) {
                throw invalid("Operation %s produces unknown variable %s".formatted(opId, operation.resultId()));
            }
            final var previousProducer = producedBy.put(operation.resultId(), operation);
            if (previousProducer != null) {
                throw invalid("Variable %s is produced by both %s and %s"
                        .formatted(operation.resultId(), previousProducer.track().getId(), opId));
            }
        }

        for (var operation : operations()) {
            final var opId = operation.track().getId();
            Set<String> seenArguments = new HashSet<>();
            for (var argument : operation.arguments()) {
                final var argumentId = argument.variableId();
                if (!variables.containsKey(argumentId)) {
                    throw invalid("Operation %s consumes unknown variable %s".formatted(opId, argumentId));
                }
                // Operation IDs on both sides: the producer of an argument must come before its consumer
                final var producer = producedBy.get(argumentId);
                if (producer != null && producer.track().getNumericId() >= operation.track().getNumericId()) {
                    throw invalid("Operation %s consumes %s, which is produced by later operation %s"
                            .formatted(opId, argumentId, producer.track().getId()));
                }
                // Register once per distinct argument, so multiply(x, x) is a single consumer of x
                if (seenArguments.add(argumentId)) {
                    participatesIn.computeIfAbsent(argumentId, id -> new ArrayList<>()).add(operation);
                }
            }
        }

        roots = variables().stream()
                .filter(v -> !producedBy.containsKey(v.track().getId())) // by graph, not by Kind
                .toList();
        leaves = variables().stream()
                .filter(v -> !participatesIn.containsKey(v.track().getId()))
                .toList();
    }

    private IllegalArgumentException invalid(String message) {
        return new IllegalArgumentException("Invalid snapshot '%s': %s"
                .formatted(snapshot.descriptor().getName(), message));
    }

    /**
     * Returns variables that are not consumed by any operation (terminal outputs).
     */
    public List<Snapshot.Variable> leaves() {
        return leaves;
    }

    /**
     * Returns variables not produced by any operation (inputs).
     */
    public List<Snapshot.Variable> roots() {
        return roots;
    }

    /**
     * Returns input variables that do not participate in any operation.
     */
    public List<Snapshot.Variable> unused() {
        return roots.stream()
                .filter(v -> !participatesIn.containsKey(v.track().getId()))
                .toList();
    }

    /**
     * Returns variables directly produced by operations that consume the given variable (one hop forward).
     *
     * @param variableId source variable ID
     */
    public List<Snapshot.Variable> produces(String variableId) {
        List<Snapshot.Operation> ops = participatesIn.get(variableId);
        if (ops == null) {
            return Collections.emptyList();
        }
        return ops.stream().map(Snapshot.Operation::resultId).map(this::getVariable).toList();
    }

    /**
     * Returns the operation that produced the given variable, or empty if it is a root.
     *
     * @param variableId target variable ID
     */
    public Optional<Snapshot.Operation> producedBy(String variableId) {
        return Optional.ofNullable(producedBy.get(variableId));
    }

    /**
     * Returns the argument variables of the given operation in argument order. Unlike {@link #dependsOn(String)},
     * a variable passed twice (e.g. {@code multiply(x, x)}) appears twice.
     *
     * @param operationId operation ID
     * @throws IllegalArgumentException if the operation is not found
     */
    public List<Snapshot.Variable> arguments(String operationId) {
        final var operation = operations.get(operationId);
        if (operation == null) {
            throw new IllegalArgumentException("Operation is not found: " + operationId);
        }
        return operation.arguments()
                .stream()
                .map(WrappedArgumentId::variableId)
                .map(this::getVariable)
                .toList();
    }

    /**
     * Returns the direct input variables of the operation that produced the given variable (one hop backward).
     * Returns an empty list if the variable has no producing operation.
     *
     * @param variableId target variable ID
     */
    public List<Snapshot.Variable> dependsOn(String variableId) {
        final var operation = producedBy.get(variableId);
        if (operation == null) {
            return Collections.emptyList();
        }

        return operation.arguments()
                .stream()
                .map(WrappedArgumentId::variableId)
                .map(this::getVariable)
                .distinct()
                .toList();
    }

    /**
     * Returns all variables transitively produced by the given variable (full forward closure).
     *
     * @param variableId source variable ID
     */
    public List<Snapshot.Variable> producesDeep(String variableId) {
        return producesDeep(variableId, Collections.emptySet());
    }

    /**
     * Returns the forward transitive closure from the given variable, stopping traversal at any variable in
     * {@code stopVariables}. Reached stop variables are included in the result, but not traversed further.
     *
     * @param variableId    source variable ID
     * @param stopVariables variable IDs at which traversal halts
     */
    public List<Snapshot.Variable> producesDeep(String variableId, Set<String> stopVariables) {
        Map<String, Snapshot.Variable> result = new HashMap<>();
        Set<String> expanded = new HashSet<>();
        Deque<String> queue = new ArrayDeque<>();
        queue.add(variableId);
        while (!queue.isEmpty()) {
            final var id = queue.poll();
            if (stopVariables.contains(id) || !expanded.add(id)) {
                continue;
            }
            for (var operation : participatesIn.getOrDefault(id, Collections.emptyList())) {
                final var resultId = operation.resultId();
                result.computeIfAbsent(resultId, this::getVariable);
                queue.add(resultId);
            }
        }
        return result.values().stream().sorted(VARIABLE_ORDER).toList();
    }

    /**
     * Returns all operations in the full production ancestry of the given variable.
     *
     * @param variableId target variable ID
     */
    public List<Snapshot.Operation> producedByDeep(String variableId) {
        return producedByDeep(variableId, Collections.emptySet());
    }

    /**
     * Returns ancestral operations of the given variable, stopping at variables in {@code stopVariables}.
     *
     * @param variableId    target variable ID
     * @param stopVariables variable IDs at which traversal halts
     */
    public List<Snapshot.Operation> producedByDeep(String variableId, Set<String> stopVariables) {
        Map<String, Snapshot.Operation> ops = new HashMap<>();
        walkBackward(variableId, stopVariables, ops, new HashMap<>());
        return ops.values().stream().sorted(OPERATION_ORDER).toList();
    }

    /**
     * Returns all variables the given variable transitively depends on (full backward closure).
     *
     * @param variableId target variable ID
     */
    public List<Snapshot.Variable> dependsOnDeep(String variableId) {
        return dependsOnDeep(variableId, Collections.emptySet());
    }

    /**
     * Returns transitive dependencies of the given variable, stopping at variables in {@code stopVariables}.
     * Reached stop variables are included in the result, but not traversed further.
     *
     * @param variableId    target variable ID
     * @param stopVariables variable IDs at which traversal halts
     */
    public List<Snapshot.Variable> dependsOnDeep(String variableId, Set<String> stopVariables) {
        Map<String, Snapshot.Variable> vars = new HashMap<>();
        walkBackward(variableId, stopVariables, new HashMap<>(), vars);
        return vars.values().stream().sorted(VARIABLE_ORDER).toList();
    }

    // Breadth-first walk towards the roots, collecting the operations passed and the argument variables reached
    private void walkBackward(String variableId, Set<String> stopVariables,
                              Map<String, Snapshot.Operation> ops, Map<String, Snapshot.Variable> vars) {
        Set<String> expanded = new HashSet<>();
        Deque<String> queue = new ArrayDeque<>();
        queue.add(variableId);
        while (!queue.isEmpty()) {
            final var id = queue.poll();
            if (stopVariables.contains(id) || !expanded.add(id)) {
                continue;
            }
            final var operation = producedBy.get(id);
            if (operation == null) {
                continue;
            }
            ops.put(operation.track().getId(), operation);
            for (var argument : operation.arguments()) {
                vars.computeIfAbsent(argument.variableId(), this::getVariable);
                queue.add(argument.variableId());
            }
        }
    }

    /**
     * Extracts the minimal sub-graph required to reproduce the given variable as a new {@link Snapshot}.
     * The result includes the variable itself, all its ancestral operations, and all their input variables,
     * sorted by numeric ID.
     *
     * @param variableId    target variable ID
     * @param stopVariables variable IDs at which backward traversal halts
     */
    public Snapshot cpgOf(String variableId, Set<String> stopVariables) {
        Map<String, Snapshot.Operation> ops = new HashMap<>();
        Map<String, Snapshot.Variable> vars = new HashMap<>();
        walkBackward(variableId, stopVariables, ops, vars);
        vars.put(variableId, getVariable(variableId));

        return new Snapshot(Descriptor.descriptor("CPG for variable " + variableId),
                vars.values().stream().sorted(VARIABLE_ORDER).toList(),
                ops.values().stream().sorted(OPERATION_ORDER).toList());
    }

    /**
     * Looks up a variable by ID.
     *
     * @param variableId variable ID
     * @return the variable, or empty if not found
     */
    public Optional<Snapshot.Variable> variable(String variableId) {
        return Optional.ofNullable(variables.get(variableId));
    }

    /**
     * Looks up an operation by ID.
     *
     * @param operationId operation ID
     * @return the operation, or empty if not found
     */
    public Optional<Snapshot.Operation> operation(String operationId) {
        return Optional.ofNullable(operations.get(operationId));
    }

    /**
     * Returns the underlying snapshot.
     */
    public Snapshot getSnapshot() {
        return snapshot;
    }

    private Snapshot.Variable getVariable(String variableId) {
        final var variable = variables.get(variableId);
        if (variable == null) {
            throw new IllegalArgumentException("Variable is not found: " + variableId);
        }
        return variable;
    }

    public List<Snapshot.Variable> findVariables(Predicate<Snapshot.Variable> predicate) {
        return variables().stream().filter(predicate).toList();
    }

    /**
     * @param predicate
     * @return null if not found
     * @throws IllegalStateException if more than one result is found
     */
    public Snapshot.Variable findSingleVariable(Predicate<Snapshot.Variable> predicate) {
        final var results = findVariables(predicate);
        if (results.isEmpty()) {
            return null;
        }
        if (results.size() > 1) {
            throw new IllegalStateException("Expected a single result, but found " + results.size());
        }
        return results.get(0);
    }

    public Snapshot.Variable findSingleVariable(String name) {
        return findSingleVariable(it -> it.track().getDescriptor().getName().equals(name));
    }

    public List<Snapshot.Variable> findVariables(String name) {
        return findVariables(it -> it.track().getDescriptor().getName().equals(name));
    }

    /**
     * Returns all variables ordered by numeric ID.
     */
    public List<Snapshot.Variable> variables() {
        return snapshot.variables();
    }

    /**
     * Returns all operations ordered by numeric ID; producers always precede their consumers.
     */
    public List<Snapshot.Operation> operations() {
        return snapshot.operations();
    }
}
