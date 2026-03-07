package io.compprov.core.wrappers.primitive;

import io.compprov.core.ComputationContext;
import io.compprov.core.meta.Descriptor;
import io.compprov.core.variable.AbstractWrappedVariable;
import io.compprov.core.variable.VariableTrack;

import java.util.List;
import java.util.function.Function;

public class WrappedInteger extends AbstractWrappedVariable<Integer> {

    public WrappedInteger(ComputationContext context, VariableTrack variableTrack, Integer value) {
        super(context, variableTrack, value);
    }

    @Override
    public Function<List<Object>, Object> getFunction(Descriptor operationDescriptor) {
        throw new UnsupportedOperationException("Integer is a parameter type and does not support operations");
    }
}
