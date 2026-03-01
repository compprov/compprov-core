package io.compprov.core.wrappers.primitive;

import io.compprov.core.Context;
import io.compprov.core.variable.AbstractWrappedVariable;
import io.compprov.core.variable.VariableTrack;

public class WrappedLong extends AbstractWrappedVariable<Long> {

    public WrappedLong(Context context, VariableTrack variableTrack, Long value) {
        super(context, variableTrack, value);
    }
}
