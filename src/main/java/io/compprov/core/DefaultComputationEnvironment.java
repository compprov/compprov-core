package io.compprov.core;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.module.SimpleModule;
import io.compprov.core.meta.Descriptor;
import io.compprov.core.meta.Meta;
import io.compprov.core.operation.OperationTrack;
import io.compprov.core.serde.*;
import io.compprov.core.variable.VariableTrack;
import io.compprov.core.wrappers.BigDecimalWrapper;
import io.compprov.core.wrappers.BigIntegerWrapper;
import io.compprov.core.wrappers.MathContextWrapper;
import io.compprov.core.wrappers.primitive.IntegerWrapper;
import io.compprov.core.wrappers.primitive.LongWrapper;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.math.MathContext;
import java.time.Clock;
import java.time.ZoneId;
import java.time.ZonedDateTime;

/**
 * Thread safe. Ready-to-use {@link ComputationEnvironment} pre-configured with serializers for all built-in
 * types and wrappers.
 */
public class DefaultComputationEnvironment extends ComputationEnvironment {

    public DefaultComputationEnvironment() {
        this(true, false);
    }

    /**
     * Creates a new environment using the UTC system clock and a fresh
     * {@link ObjectMapper} with all required serializers registered.
     *
     * @param requireInputDescriptor  when {@code true}, null is not allowed for the descriptor in wrap methods
     * @param requireResultDescriptor when {@code true}, null is not allowed for the descriptor in executeOperation
     */
    public DefaultComputationEnvironment(boolean requireInputDescriptor, boolean requireResultDescriptor) {
        this(Clock.systemUTC(), ZoneId.of("UTC"), new ObjectMapper(),
                requireInputDescriptor, requireResultDescriptor);

        SimpleModule module = new SimpleModule();
        module.addSerializer(ZonedDateTime.class, new ZonedDateTimeSerializer());
        module.addSerializer(Meta.class, new MetaSerializer());
        module.addDeserializer(MathContext.class, new MathContextDeserializer());
        module.addDeserializer(Descriptor.class, new DescriptorDeserializer());
        module.addDeserializer(VariableTrack.class, new VariableTrackDeserializer());
        module.addDeserializer(OperationTrack.class, new OperationTrackDeserializer());
        module.addDeserializer(Snapshot.Variable.class, new VariableDeserializer());
        mapper.registerModule(module);
    }

    public DefaultComputationEnvironment(Clock clock, ZoneId zoneId, ObjectMapper mapper,
                                         boolean requireInputDescriptor, boolean requireResultDescriptor) {
        super(clock, zoneId, mapper, requireInputDescriptor, requireResultDescriptor);

        registerWrapper(BigDecimal.class, new BigDecimalWrapper());
        registerWrapper(BigInteger.class, new BigIntegerWrapper());
        registerWrapper(Integer.class, new IntegerWrapper());
        registerWrapper(Long.class, new LongWrapper());
        registerWrapper(MathContext.class, new MathContextWrapper());
    }
}
