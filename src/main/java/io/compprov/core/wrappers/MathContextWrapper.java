package io.compprov.core.wrappers;

import io.compprov.core.ComputationContext;
import io.compprov.core.variable.VariableTrack;
import io.compprov.core.variable.VariableWrapper;
import io.compprov.core.variable.WrappedVariable;

import java.math.MathContext;

public class MathContextWrapper implements VariableWrapper<MathContext> {
    @Override
    public WrappedVariable wrap(ComputationContext context, VariableTrack variableTrack, MathContext value) {
        return new WrappedMathContext(context, variableTrack, value);
    }
}
