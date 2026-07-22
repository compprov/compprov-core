package io.compprov.tools;

import io.compprov.core.Snapshot;
import io.compprov.core.meta.Descriptor;
import io.compprov.core.operation.WrappedArgumentId;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
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
 * Index maps are built once at construction time; all query methods run in O(result size).
 */
public class SnapshotNavigator {

    private final Snapshot snapshot;
    private final Set<String> leafIds;
    private final Set<String> rootIds;

    private final Map<String, Snapshot.Variable> variables;
    private final Map<String, Snapshot.Operation> operations;
    private final Map<String, Snapshot.Operation> producedBy;
    private final Map<String, List<Snapshot.Operation>> participatesIn;

    /**
     * Builds navigation indexes from the given snapshot.
     */
    public SnapshotNavigator(Snapshot snapshot) {
        this.snapshot = snapshot;
        variables = new HashMap<>();
        operations = new HashMap<>();
        producedBy = new HashMap<>();
        participatesIn = new HashMap<>();
        leafIds = new HashSet<>();
        rootIds = new HashSet<>();

        for (var operation : snapshot.operations()) {
            operations.put(operation.track().getId(), operation);
            producedBy.put(operation.resultId(), operation);
            operation.arguments().stream().map(WrappedArgumentId::variableId).map(Object::toString).forEach(inputId -> {
                participatesIn.computeIfAbsent(inputId, (id) -> new ArrayList<>()).add(operation);
            });
        }

        snapshot.variables().forEach(variable -> {
            String id = variable.track().getId();
            variables.put(id, variable);
            if (!producedBy.containsKey(variable.track().getId())) { //to avoid falsification through Kind
                rootIds.add(id);
            }

            if (!participatesIn.containsKey(id)) {
                leafIds.add(id);
            }
        });
    }

    /**
     * Returns variables that are not consumed by any operation (terminal outputs).
     */
    public List<Snapshot.Variable> leaves() {
        return leafIds.stream().map(this::getVariable).toList();
    }

    /**
     * Returns all input variables.
     */
    public List<Snapshot.Variable> roots() {
        return rootIds.stream().map(this::getVariable).toList();
    }

    /**
     * Returns input variables that do not participate in any operation.
     */
    public List<Snapshot.Variable> unused() {
        return rootIds
                .stream()
                .filter(id -> !participatesIn.containsKey(id))
                .map(this::getVariable)
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
                .map(Object::toString)
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
     * {@code stopVariables}. Stop variables themselves are excluded from the result.
     *
     * @param variableId    source variable ID
     * @param stopVariables variable IDs at which traversal halts
     */
    public List<Snapshot.Variable> producesDeep(String variableId, Set<String> stopVariables) {
        if (stopVariables.contains(variableId)) {
            return Collections.emptyList();
        }
        Map<String, Snapshot.Variable> result = new HashMap<>();
        final var producedVariables = produces(variableId);
        for (var producedVariable : producedVariables) {
            result.put(producedVariable.track().getId(), producedVariable);
            producesDeep(producedVariable.track().getId(), stopVariables)
                    .forEach(variable -> result.put(variable.track().getId(), variable));
        }
        return result.values().stream().toList();
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
        if (stopVariables.contains(variableId)) {
            return Collections.emptyList();
        }
        final var producedByOperation = producedBy(variableId);
        if (producedByOperation.isEmpty()) {
            return Collections.emptyList();
        }

        Map<String, Snapshot.Operation> result = new HashMap<>();
        producedByOperation.ifPresent(operation -> {
            result.put(operation.track().getId(), operation);
            operation.arguments()
                    .stream()
                    .map(WrappedArgumentId::variableId)
                    .map(Object::toString)
                    .map(varId -> producedByDeep(varId, stopVariables))
                    .flatMap(Collection::stream)
                    .forEach(op -> result.put(op.track().getId(), op));
        });
        return result.values().stream().toList();
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
     *
     * @param variableId    target variable ID
     * @param stopVariables variable IDs at which traversal halts
     */
    public List<Snapshot.Variable> dependsOnDeep(String variableId, Set<String> stopVariables) {
        if (stopVariables.contains(variableId)) {
            return Collections.emptyList();
        }
        final var producedByOperation = producedBy(variableId);
        if (producedByOperation.isEmpty()) {
            return Collections.emptyList();
        }

        Map<String, Snapshot.Variable> result = new HashMap<>();
        producedByOperation.ifPresent(operation -> {
            operation.arguments()
                    .stream()
                    .map(WrappedArgumentId::variableId)
                    .map(Object::toString)
                    .map(this::getVariable)
                    .forEach(variable -> {
                        result.put(variable.track().getId(), variable);
                        dependsOnDeep(variable.track().getId(), stopVariables)
                                .forEach(v -> result.put(v.track().getId(), v));
                    });
        });
        return result.values().stream().toList();
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
        final var ops = new ArrayList<>(producedByDeep(variableId, stopVariables));
        final var variables = new ArrayList<>(dependsOnDeep(variableId, stopVariables));
        variables.add(getVariable(variableId));

        ops.sort((o1, o2) -> Integer.compare(o1.track().getNumericId(), o2.track().getNumericId()));
        variables.sort((v1, v2) -> Integer.compare(v1.track().getNumericId(), v2.track().getNumericId()));
        return new Snapshot(Descriptor.descriptor("CPG for variable " + variableId), variables, ops);
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
        return variables.values().stream().filter(predicate).toList();
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

    public List<Snapshot.Variable> variables() {
        return snapshot.variables();
    }

    public List<Snapshot.Operation> operations() {
        return snapshot.operations();
    }
}
