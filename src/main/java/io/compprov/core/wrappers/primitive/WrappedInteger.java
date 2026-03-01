package io.compprov.core.wrappers.primitive;

import io.compprov.core.Context;
import io.compprov.core.variable.AbstractWrappedVariable;
import io.compprov.core.variable.VariableTrack;

public class WrappedInteger extends AbstractWrappedVariable<Integer> {

    public WrappedInteger(Context context, VariableTrack variableTrack, Integer value) {
        super(context, variableTrack, value);
    }
}
