package io.compprov.core.wrappers.primitive;

import io.compprov.core.ComputationContext;
import io.compprov.core.meta.Descriptor;
import io.compprov.core.variable.AbstractWrappedVariable;
import io.compprov.core.variable.VariableTrack;

import java.util.List;
import java.util.function.Function;

public class WrappedLong extends AbstractWrappedVariable<Long> {

    public WrappedLong(ComputationContext context, VariableTrack variableTrack, Long value) {
        super(context, variableTrack, value);
    }

    @Override
    public Function<List<Object>, Object> getFunction(Descriptor operationDescriptor) {
        throw new UnsupportedOperationException("Long is a parameter type and does not support operations");
    }
}
