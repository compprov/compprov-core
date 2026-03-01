package io.compprov.core.wrappers;

import io.compprov.core.Context;
import io.compprov.core.variable.VariableTrack;
import io.compprov.core.variable.VariableWrapper;
import io.compprov.core.variable.WrappedVariable;

import java.math.BigInteger;

public final class BigIntegerWrapper implements VariableWrapper<BigInteger> {

    @Override
    public WrappedVariable wrap(Context context, VariableTrack variableTrack, BigInteger value) {
        return new WrappedBigInteger(context, variableTrack, value);
    }
}
