package io.compprov.examples.nav.wrapped;

import io.compprov.core.ComputationContext;
import io.compprov.core.meta.Descriptor;
import io.compprov.core.variable.AbstractWrappedVariable;
import io.compprov.core.variable.VariableTrack;
import io.compprov.examples.nav.model.Rate;

import java.util.List;
import java.util.function.Function;

public class WrappedRate extends AbstractWrappedVariable<Rate> {
    public WrappedRate(ComputationContext context, VariableTrack variableTrack, Rate value) {
        super(context, variableTrack, value);
    }

    @Override
    public Function<List<Object>, Object> getFunction(Descriptor operationDescriptor) {
        throw new RuntimeException("No functions available");
    }
}

