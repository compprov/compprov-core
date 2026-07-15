package io.compprov.core.wrappers;

import io.compprov.core.ComputationContext;
import io.compprov.core.variable.VariableTrack;
import io.compprov.core.variable.VariableWrapper;
import io.compprov.core.variable.WrappedVariable;

import java.math.BigInteger;

public final class BigIntegersWrapper implements VariableWrapper<BigInteger[]> {

    @Override
    public WrappedVariable wrap(ComputationContext context, VariableTrack variableTrack, BigInteger[] value) {
        return new WrappedBigIntegers(context, variableTrack, value);
    }
}
