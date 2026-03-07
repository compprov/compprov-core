package io.compprov.core.wrappers.primitive;

import io.compprov.core.ComputationContext;
import io.compprov.core.variable.VariableTrack;
import io.compprov.core.variable.VariableWrapper;
import io.compprov.core.variable.WrappedVariable;

public class LongWrapper implements VariableWrapper<Long> {
    @Override
    public WrappedVariable wrap(ComputationContext context, VariableTrack variableTrack, Long value) {
        return new WrappedLong(context, variableTrack, value);
    }
}
