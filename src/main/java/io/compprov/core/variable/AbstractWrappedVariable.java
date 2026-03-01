package io.compprov.core.variable;

import io.compprov.core.Context;
import io.compprov.core.Descriptor;
import io.compprov.core.meta.Meta;

import java.util.List;
import java.util.function.Supplier;
import java.util.stream.Collectors;

public abstract class AbstractWrappedVariable<T> implements WrappedVariable<T> {

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

    public String getWrappedClass() {
        return value.getClass().getSimpleName();
    }

    protected WrappedVariable execute(Supplier<?> computation,
                                      List<WrappedVariable> input,
                                      Descriptor opDescriptor,
                                      Descriptor resultDescriptor) {
        Descriptor effectiveResult = resultDescriptor != null ? resultDescriptor : new Descriptor("%s(%s)"
                .formatted(opDescriptor.getName(), input
                        .stream()
                        .map(v -> v.getVariableTrack().getId().toString())
                        .collect(Collectors.joining(", "))),
                Meta.NO_META,
                Meta.NO_META);

        return getContext().executeOperation(computation, input, opDescriptor, effectiveResult);
    }
}
