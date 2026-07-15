package io.compprov.core;

import io.compprov.core.operation.OperationArgument;
import io.compprov.core.operation.WrappedOperation;
import io.compprov.core.variable.WrappedVariable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * An immutable, reusable template of a previously-recorded computation, captured from a
 * {@link ComputationContext}. Replaying it via a {@link MutableState} lets a repeated
 * computation step be tracked once and executed many times without re-recording its internal
 * variables and operations into the driving CPG on every call — see {@code WrappedSubgraph}.
 *
 * <p>{@link #operations()} and {@link #variables()} are fixed at construction and read-only,
 * so a single {@code Subgraph} instance can safely back any number of independent
 * {@link MutableState} instances, including concurrently from multiple threads.</p>
 */
public class Subgraph {

    private final MutableState defaultState;
    private final List<WrappedVariable> variables;
    private final List<WrappedOperation> operations;
    private final List<String> argumentIds;
    private final String resultId;

    public Subgraph(ComputationContext subgraphCtx, List<String> argumentIds, String resultId) {

        this.variables = subgraphCtx.data.variables.values().stream().toList();
        this.operations = subgraphCtx.data.operations.values()
                .stream()
                .sorted(Comparator.comparing(it -> it.getOperationTrack().getNumericId()))
                .toList();
        this.defaultState = new MutableState(variables);

        for (var argumentId : argumentIds) {
            if (!defaultState.variables.containsKey(argumentId)) {
                throw new IllegalArgumentException("Argument with id: " + argumentId + " is not found in subgraph");
            }
        }
        this.argumentIds = Collections.unmodifiableList(argumentIds);

        if (!defaultState.variables.containsKey(resultId)) {
            throw new IllegalArgumentException("Result variable with id: " + resultId + " is not found in subgraph");
        }
        this.resultId = resultId;
    }


    public List<String> argumentIds() {
        return argumentIds;
    }

    public String resultId() {
        return resultId;
    }

    /**
     * The state shared by every call to {@code WrappedSubgraph.execute()}. Callers must
     * synchronize on it before mutating or computing, since it is reused across invocations.
     */
    public MutableState getDefaultState() {
        return defaultState;
    }

    /**
     * A fresh, private copy of the template's intermediate variables. Unlike
     * {@link #getDefaultState()}, a state returned here is not shared with any other caller,
     * so it can be computed concurrently with other states without external synchronization —
     * at the cost of copying every tracked variable on each call.
     */
    public MutableState createState() {
        return new MutableState(variables);
    }

    /**
     * Replays every recorded operation against {@code state} and returns the result variable's
     * value. Not safe to call concurrently with another {@code compute()} call sharing the same
     * {@code state} — see {@link #getDefaultState()} vs. {@link #createState()}.
     */
    public Object compute(MutableState state) {
        operations.forEach(op -> executeOperation(op, state));
        return state.variables.get(resultId).value;
    }

    public void executeOperation(WrappedOperation operation, MutableState state) {
        final var operationArguments = operation.getArguments();
        final var callerId = operationArguments.get(0).variableId();
        final var caller = state.variables.get(callerId);

        final var opDescriptor = operation.getOperationTrack().getDescriptor();
        final var functionToCall = caller.variable.getFunction(opDescriptor);
        if (functionToCall == null) {
            throw new IllegalStateException("No function registered for operation: " + opDescriptor.getName());
        }

        final var operationArgumentValues = quickExtract(operationArguments, state);
        final var result = functionToCall.apply(operationArgumentValues);
        Objects.requireNonNull(result, "computation must not return null");

        state.variables.get(operation.getResultId()).value = result;
    }

    public List<WrappedOperation> operations() {
        return operations;
    }

    public List<WrappedVariable> variables() {
        return variables
                .stream()
                .sorted(Comparator.comparing(it -> it.getVariableTrack().getNumericId()))
                .toList();
    }

    /**
     * A single execution's private working copy of a {@link Subgraph}'s tracked variables.
     * Not thread-safe on its own — see {@link Subgraph#getDefaultState()} and
     * {@link Subgraph#createState()} for the two ways callers obtain one.
     */
    public static class MutableState {
        private final Map<String, MutableVariable> variables;

        public MutableState(List<WrappedVariable> variables) {
            this.variables = new HashMap<>();
            variables.forEach(e -> this.variables.put(e.getVariableTrack().getId(), new MutableVariable(e)));
        }

        public void setArgument(String argumentId, Object value) {
            final var mv = variables.get(argumentId);
            if (mv == null) {
                throw new IllegalArgumentException("Argument with id: " + argumentId + " is not found in subgraph");
            }
            mv.value = value;
        }

        public Object getValue(String variableId) {
            final var result = variables.get(variableId);
            if (result == null) {
                throw new IllegalArgumentException("Variable not found: " + variableId);
            }
            return result.value;
        }
    }

    public static class MutableVariable {
        private final WrappedVariable variable;
        private volatile Object value;

        public MutableVariable(WrappedVariable variable) {
            this.variable = variable;
            this.value = variable.getValue();
        }
    }

    @Override
    public String toString() {
        return "Subgraph of " + resultId + argumentIds;
    }

    private List<Object> quickExtract(List<? extends OperationArgument> arguments, MutableState state) {
        final var result = new ArrayList<>(arguments.size());
        for (int i = 0; i < arguments.size(); i++) {
            result.add(state.variables.get(arguments.get(i).variableId()).value);
        }
        return result;
    }
}
