package io.compprov.core.wrappers;

import io.compprov.core.Context;
import io.compprov.core.variable.VariableTrack;
import io.compprov.core.variable.VariableWrapper;
import io.compprov.core.variable.WrappedVariable;

import java.math.BigDecimal;

public final class BigDecimalWrapper implements VariableWrapper<BigDecimal> {

    @Override
    public WrappedVariable wrap(Context context, VariableTrack variableTrack, BigDecimal value) {
        return new WrappedBigDecimal(context, variableTrack, value);
    }
}
