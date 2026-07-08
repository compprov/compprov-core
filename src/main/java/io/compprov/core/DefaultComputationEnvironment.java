package io.compprov.core;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.module.SimpleModule;
import io.compprov.core.meta.Descriptor;
import io.compprov.core.meta.Meta;
import io.compprov.core.serde.DescriptorDeserializer;
import io.compprov.core.serde.MathContextDeserializer;
import io.compprov.core.serde.MetaSerializer;
import io.compprov.core.serde.OperationDeserializer;
import io.compprov.core.serde.SubgraphDeserializer;
import io.compprov.core.serde.SubgraphSerializer;
import io.compprov.core.serde.VariableDeserializer;
import io.compprov.core.serde.VariableTrackDeserializer;
import io.compprov.core.serde.ZonedDateTimeSerializer;
import io.compprov.core.variable.VariableTrack;
import io.compprov.core.wrappers.BigDecimalWrapper;
import io.compprov.core.wrappers.BigIntegerWrapper;
import io.compprov.core.wrappers.MathContextWrapper;
import io.compprov.core.wrappers.primitive.IntegerWrapper;
import io.compprov.core.wrappers.primitive.LongWrapper;
import io.compprov.core.wrappers.subgraph.SubgraphWrapper;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.math.MathContext;
import java.time.Clock;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Objects;

/**
 * Thread safe. Ready-to-use {@link ComputationEnvironment} pre-configured with serializers for all built-in
 * types and wrappers.
 */
public class DefaultComputationEnvironment extends ComputationEnvironment {

    public static DefaultComputationEnvironment create() {
        return create(true, false);
    }

    /**
     * Creates a new environment using the UTC system clock and a fresh
     * {@link ObjectMapper} with all required serializers registered.
     *
     * @param requireInputDescriptor  when {@code true}, null is not allowed for the descriptor in wrap methods
     * @param requireResultDescriptor when {@code true}, null is not allowed for the descriptor in executeOperation
     */
    public static DefaultComputationEnvironment create(boolean requireInputDescriptor, boolean requireResultDescriptor) {
        final var env = new DefaultComputationEnvironment(Clock.systemUTC(), ZoneId.of("UTC"), new ObjectMapper(),
                requireInputDescriptor, requireResultDescriptor);
        env.configureMapper();
        return env;
    }

    public static DefaultComputationEnvironment create(Clock clock, ZoneId zoneId, ObjectMapper mapper,
                                                       boolean requireInputDescriptor, boolean requireResultDescriptor) {
        return new DefaultComputationEnvironment(clock, zoneId, mapper, requireInputDescriptor,
                requireResultDescriptor);
    }

    /**
     * @param clock
     * @param zoneId
     * @param mapper                  - should be able to serialize and deserialize compprov types
     * @param requireInputDescriptor
     * @param requireResultDescriptor
     */
    protected DefaultComputationEnvironment(Clock clock, ZoneId zoneId, ObjectMapper mapper,
                                            boolean requireInputDescriptor, boolean requireResultDescriptor) {
        super(clock, zoneId, mapper, requireInputDescriptor, requireResultDescriptor);
        registerWrapper(BigDecimal.class, new BigDecimalWrapper());
        registerWrapper(BigInteger.class, new BigIntegerWrapper());
        registerWrapper(Integer.class, new IntegerWrapper());
        registerWrapper(Long.class, new LongWrapper());
        registerWrapper(MathContext.class, new MathContextWrapper());
        registerWrapper(Subgraph.class, new SubgraphWrapper());

    }

    private void configureMapper() {
        SimpleModule module = new SimpleModule();
        module.addSerializer(ZonedDateTime.class, new ZonedDateTimeSerializer());
        module.addSerializer(Meta.class, new MetaSerializer());
        module.addSerializer(Subgraph.class, new SubgraphSerializer());
        module.addDeserializer(MathContext.class, new MathContextDeserializer());
        module.addDeserializer(Descriptor.class, new DescriptorDeserializer());
        module.addDeserializer(VariableTrack.class, new VariableTrackDeserializer());
        module.addDeserializer(Snapshot.Variable.class, new VariableDeserializer(wrappers));
        module.addDeserializer(Snapshot.Operation.class, new OperationDeserializer());
        module.addDeserializer(Subgraph.class, new SubgraphDeserializer(this));
        mapper.configOverride(BigDecimal.class).setFormat(JsonFormat.Value.forShape(JsonFormat.Shape.STRING));
        mapper.configOverride(BigInteger.class).setFormat(JsonFormat.Value.forShape(JsonFormat.Shape.STRING));
        mapper.registerModule(module);
    }

    @Override
    public ComputationContext compute(Snapshot snapshot) {
        Objects.requireNonNull(snapshot, "snapshot");

        return new DefaultComputationContext(this, new DataContext(snapshot.descriptor()), snapshot);
    }
}
