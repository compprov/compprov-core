package io.compprov.core.wrappers;

import io.compprov.core.ComputationContext;
import io.compprov.core.meta.Descriptor;
import io.compprov.core.variable.AbstractWrappedVariable;
import io.compprov.core.variable.VariableTrack;

import java.math.MathContext;
import java.util.List;
import java.util.function.Function;

public class WrappedMathContext extends AbstractWrappedVariable<MathContext> {

    public WrappedMathContext(ComputationContext context, VariableTrack variableTrack, MathContext value) {
        super(context, variableTrack, value);
    }

    @Override
    public Function<List<Object>, Object> getFunction(Descriptor operationDescriptor) {
        throw new UnsupportedOperationException("MathContext is a parameter type and does not support operations");
    }
}
