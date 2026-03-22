package io.compprov.examples.nav.wrapped;

import io.compprov.core.ComputationContext;
import io.compprov.core.variable.VariableTrack;
import io.compprov.core.variable.VariableWrapper;
import io.compprov.core.variable.WrappedVariable;
import io.compprov.examples.nav.model.Amount;

public class AmountWrapper implements VariableWrapper<Amount> {
    @Override
    public WrappedVariable wrap(ComputationContext context, VariableTrack variableTrack, Amount value) {
        return new WrappedAmount(context, variableTrack, value);
    }
}
