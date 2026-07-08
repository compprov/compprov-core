package io.compprov.core;

import io.compprov.core.operation.WrappedOperation;
import io.compprov.core.variable.WrappedVariable;

import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

public class Subgraph {

    private final Map<String, MutableVariable> variables;
    private final List<WrappedOperation> operations;
    private final List<String> argumentIds;
    private final String resultId;

    public Subgraph(ComputationContext subgraphCtx, List<String> argumentIds, String resultId) {

        variables = subgraphCtx.data.variables.values()
                .stream()
                .collect(Collectors.toMap(
                        v -> v.getVariableTrack().getId(),
                        MutableVariable::new));
        this.operations = subgraphCtx.data.operations.values()
                .stream()
                .sorted(Comparator.comparing(it -> it.getOperationTrack().getNumericId()))
                .toList();

        for (var argumentId : argumentIds) {
            if (!variables.containsKey(argumentId)) {
                throw new IllegalArgumentException("Argument with id: " + argumentId + " is not found in subgraph");
            }
        }
        this.argumentIds = Collections.unmodifiableList(argumentIds);

        if (!variables.containsKey(resultId)) {
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

    public void setArgument(String argumentId, Object value) {
        final var mv = variables.get(argumentId);
        if (mv == null) {
            throw new IllegalArgumentException("Argument with id: " + argumentId + " is not found in subgraph");
        }
        mv.value = value;
    }

    public Object compute() {
        operations.forEach(this::executeOperation);
        return variables.get(resultId).value;
    }

    public void executeOperation(WrappedOperation operation) {
        final var operationArgumentIds = operation.getArguments();
        final var callerId = operationArgumentIds.values().iterator().next();
        final var caller = variables.get(callerId);

        final var opDescriptor = operation.getOperationTrack().getDescriptor();
        final var functionToCall = caller.variable.getFunction(opDescriptor);
        if (functionToCall == null) {
            throw new IllegalStateException("No function registered for operation: " + opDescriptor.getName());
        }

        final var operationArgumentValues = operationArgumentIds.values()
                .stream()
                .map(argId -> variables.get(argId).value)
                .collect(Collectors.toList());
        final var result = functionToCall.apply(operationArgumentValues);
        Objects.requireNonNull(result, "computation must not return null");

        variables.get(operation.getResultId()).value = result;
    }

    public List<WrappedOperation> operations() {
        return operations;
    }

    public List<WrappedVariable> variables() {
        return variables.values()
                .stream()
                .map(it -> it.variable)
                .sorted(Comparator.comparing(it -> it.getVariableTrack().getNumericId()))
                .toList();
    }

    public static class MutableVariable {
        private final WrappedVariable variable;
        private volatile Object value;

        public MutableVariable(WrappedVariable variable) {
            this.variable = variable;
            this.value = variable.getValue();
        }
    }
}
