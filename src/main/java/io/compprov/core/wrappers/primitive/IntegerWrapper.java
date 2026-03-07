package io.compprov.core.wrappers.primitive;

import io.compprov.core.ComputationContext;
import io.compprov.core.variable.VariableTrack;
import io.compprov.core.variable.VariableWrapper;
import io.compprov.core.variable.WrappedVariable;

public class IntegerWrapper implements VariableWrapper<Integer> {
    @Override
    public WrappedVariable wrap(ComputationContext context, VariableTrack variableTrack, Integer value) {
        return new WrappedInteger(context, variableTrack, value);
    }
}
