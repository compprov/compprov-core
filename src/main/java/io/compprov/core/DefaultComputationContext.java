package io.compprov.core;

import io.compprov.core.meta.Descriptor;
import io.compprov.core.wrappers.WrappedBigDecimal;
import io.compprov.core.wrappers.WrappedBigInteger;
import io.compprov.core.wrappers.WrappedMathContext;
import io.compprov.core.wrappers.primitive.WrappedInteger;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.math.MathContext;

/**
 * <p>Thread-safe class, except for the {@link #snapshot()} operation.</p>
 * <p>Feel free to override and expand this class as you need.</p>
 */
public class DefaultComputationContext extends ComputationContext {
    public DefaultComputationContext(ComputationEnvironment environment, DataContext dataContext) {
        super(environment, dataContext);
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

    /**
     * Wraps an {@link Integer} input value and registers it in the context.
     *
     * @param value      the value to wrap
     * @param descriptor provenance descriptor
     * @return the wrapped variable
     */
    public WrappedInteger wrapInteger(Integer value, Descriptor descriptor) {
        return (WrappedInteger) super.wrap(value, descriptor);
    }

    /**
     * Wraps a {@link MathContext} input value and registers it in the context.
     *
     * @param value      the value to wrap
     * @param descriptor provenance descriptor
     * @return the wrapped variable
     */
    public WrappedMathContext wrapMathContext(MathContext value, Descriptor descriptor) {
        return (WrappedMathContext) super.wrap(value, descriptor);
    }
}
