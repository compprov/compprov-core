package io.compprov.core.wrappers;

import io.compprov.core.Context;
import io.compprov.core.variable.AbstractWrappedVariable;
import io.compprov.core.variable.VariableTrack;

import java.math.MathContext;

public class WrappedMathContext extends AbstractWrappedVariable<MathContext> {

    public WrappedMathContext(Context context, VariableTrack variableTrack, MathContext value) {
        super(context, variableTrack, value);
    }
}
