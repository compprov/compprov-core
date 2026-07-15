package io.compprov.core.operation;

import io.compprov.core.variable.WrappedVariable;

public record WrappedArgument(String key, WrappedVariable variable) implements OperationArgument{
    @Override
    public String metaName() {
        return key;
    }

    @Override
    public String variableId() {
        return variable.getVariableTrack().getId();
    }
}

