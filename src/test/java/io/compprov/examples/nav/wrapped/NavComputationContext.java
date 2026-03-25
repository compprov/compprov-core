package io.compprov.examples.nav.wrapped;

import io.compprov.core.ComputationEnvironment;
import io.compprov.core.DataContext;
import io.compprov.core.DefaultComputationContext;
import io.compprov.core.DefaultComputationEnvironment;
import io.compprov.core.meta.Descriptor;
import io.compprov.examples.nav.model.Amount;
import io.compprov.examples.nav.model.Rate;

public class NavComputationContext extends DefaultComputationContext {

    public static final ComputationEnvironment environment;

    static {

        environment = new DefaultComputationEnvironment();

        //register wrappers
        environment.registerWrapper(Amount.class, new AmountWrapper(), new AmountDeserializer());
        environment.registerWrapper(Rate.class, new RateWrapper());
    }

    public NavComputationContext(Descriptor descriptor) {
        super(environment, new DataContext(descriptor));
    }

    public WrappedAmount wrap(Amount amount, Descriptor descriptor) {
        return (WrappedAmount) super.wrap(amount, descriptor);
    }

    public WrappedRate wrap(Rate rate, Descriptor descriptor) {
        return (WrappedRate) super.wrap(rate, descriptor);
    }
}
