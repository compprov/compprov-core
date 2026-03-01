package io.compprov.core;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.module.SimpleModule;
import io.compprov.core.meta.Meta;
import io.compprov.core.operation.WrappedOperation;
import io.compprov.core.variable.AbstractWrappedVariable;
import io.compprov.core.wrappers.*;
import io.compprov.core.wrappers.primitive.IntegerWrapper;
import io.compprov.core.wrappers.primitive.LongWrapper;
import io.compprov.core.wrappers.primitive.WrappedInteger;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.math.MathContext;
import java.time.Clock;
import java.time.ZoneId;
import java.time.ZonedDateTime;

/**
 * Ready-to-use {@link Context} pre-configured with serializers for all built-in
 * types and wrappers for {@link BigDecimal} and {@link BigInteger}.
 *
 * <p>Use the no-arg constructor for production code. The three-arg constructor
 * is available for testing with deterministic clocks.</p>
 */
public class DefaultContext extends Context {

    /**
     * Creates a default context using the UTC system clock and a fresh
     * {@link ObjectMapper} with all required serializers registered.
     */
    public DefaultContext() {
        this(Clock.systemUTC(), ZoneId.of("UTC"), new ObjectMapper());

        SimpleModule module = new SimpleModule();
        module.addSerializer(AbstractWrappedVariable.class, new AbstractWrappedVariableSerializer());
        module.addSerializer(WrappedOperation.class, new WrappedOperationSerializer());
        module.addSerializer(ZonedDateTime.class, new ZonedDateTimeSerializer());
        module.addSerializer(Meta.NoMeta.class, new NoMetaSerializer());
        mapper.registerModule(module);
    }

    /**
     * Creates a context with the given clock, zone, and mapper.
     *
     * <p>The caller is responsible for configuring the {@link ObjectMapper} with the
     * necessary serializers before calling {@link #toJson()}.</p>
     *
     * @param clock  the clock used to timestamp variables and operations
     * @param zoneId the zone attached to every timestamp
     * @param mapper the Jackson mapper
     */
    public DefaultContext(Clock clock, ZoneId zoneId, ObjectMapper mapper) {
        super(clock, zoneId, mapper);

        registerWrapper(BigDecimal.class, new BigDecimalWrapper());
        registerWrapper(BigInteger.class, new BigIntegerWrapper());
        registerWrapper(Integer.class, new IntegerWrapper());
        registerWrapper(Long.class, new LongWrapper());
        registerWrapper(MathContext.class, new MathContextWrapper());
    }

    /**
     * Wraps a {@link BigDecimal} input value and registers it in the context.
     *
     * @param value      the value to wrap
     * @param descriptor provenance descriptor
     * @return the wrapped variable
     */
    public WrappedBigDecimal wrapBigDecimal(BigDecimal value, Descriptor descriptor) {
        return (WrappedBigDecimal) super.wrap(value, descriptor);
    }

    /**
     * Wraps a {@link BigInteger} input value and registers it in the context.
     *
     * @param value      the value to wrap
     * @param descriptor provenance descriptor
     * @return the wrapped variable
     */
    public WrappedBigInteger wrapBigInteger(BigInteger value, Descriptor descriptor) {
        return (WrappedBigInteger) super.wrap(value, descriptor);
    }

    public WrappedInteger wrapInteger(Integer value, Descriptor descriptor) {
        return (WrappedInteger) super.wrap(value, descriptor);
    }

    public WrappedMathContext wrapMathContext(MathContext value, Descriptor descriptor) {
        return (WrappedMathContext) super.wrap(value, descriptor);
    }
}
