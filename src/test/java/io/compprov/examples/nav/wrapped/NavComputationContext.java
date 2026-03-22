package io.compprov.examples.nav.wrapped;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.module.SimpleModule;
import io.compprov.core.*;
import io.compprov.core.meta.Descriptor;
import io.compprov.core.meta.Meta;
import io.compprov.core.operation.OperationTrack;
import io.compprov.core.serde.*;
import io.compprov.core.variable.VariableTrack;
import io.compprov.examples.nav.model.Amount;
import io.compprov.examples.nav.model.Rate;

import java.math.MathContext;
import java.time.Clock;
import java.time.ZoneId;
import java.time.ZonedDateTime;

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
