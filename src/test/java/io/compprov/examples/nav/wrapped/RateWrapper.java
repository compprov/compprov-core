package io.compprov.examples.nav.wrapped;

import io.compprov.core.ComputationContext;
import io.compprov.core.variable.VariableTrack;
import io.compprov.core.variable.VariableWrapper;
import io.compprov.core.variable.WrappedVariable;
import io.compprov.examples.nav.model.Rate;

public class RateWrapper implements VariableWrapper<Rate> {
    @Override
    public WrappedVariable wrap(ComputationContext context, VariableTrack variableTrack, Rate value) {
        return new WrappedRate(context, variableTrack, value);
    }
}