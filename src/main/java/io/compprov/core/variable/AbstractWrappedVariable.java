package io.compprov.core.variable;

import io.compprov.core.Context;

public class AbstractWrappedVariable<T> implements WrappedVariable<T> {

    private final Context context;
    private final VariableTrack variableTrack;
    private final T value;

    public AbstractWrappedVariable(Context context, VariableTrack variableTrack, T value) {
        this.context = context;
        this.variableTrack = variableTrack;
        this.value = value;
    }

    @Override
    public VariableTrack getVariableTrack() {
        return variableTrack;
    }

    @Override
    public T getValue() {
        return value;
    }

    @Override
    public Context getContext() {
        return context;
    }
}
