package io.compprov.core.wrappers;

import io.compprov.core.Context;
import io.compprov.core.Descriptor;
import io.compprov.core.variable.AbstractWrappedVariable;
import io.compprov.core.variable.VariableTrack;
import io.compprov.core.wrappers.primitive.WrappedInteger;

import java.math.BigDecimal;
import java.util.List;
import java.util.Objects;

import static io.compprov.core.meta.Meta.NO_META;
import static io.compprov.core.meta.MetaFormula.formula;

/**
 * Provenance-tracked {@link BigDecimal}. Every operation records itself into the owning context.
 */
public final class WrappedBigDecimal extends AbstractWrappedVariable<BigDecimal> {

    private static final Descriptor OP_ADD                   = Descriptor.descriptor("add",                   formula("(a+b)mc"),              NO_META);
    private static final Descriptor OP_SUBTRACT              = Descriptor.descriptor("subtract",              formula("(a-b)mc"),              NO_META);
    private static final Descriptor OP_MULTIPLY              = Descriptor.descriptor("multiply",              formula("(a*b)mc"),              NO_META);
    private static final Descriptor OP_DIVIDE                = Descriptor.descriptor("divide",                formula("(a/b)mc"),              NO_META);
    private static final Descriptor OP_DIVIDE_TO_INTEGRAL    = Descriptor.descriptor("divideToIntegralValue", formula("floor(a/b)mc"),        NO_META);
    private static final Descriptor OP_REMAINDER             = Descriptor.descriptor("remainder",             formula("(a%b)mc"),              NO_META);
    private static final Descriptor OP_POW                   = Descriptor.descriptor("pow",                   formula("(a^n)mc"),              NO_META);
    private static final Descriptor OP_ABS                   = Descriptor.descriptor("abs",                   formula("|a|mc"),                NO_META);
    private static final Descriptor OP_NEGATE                = Descriptor.descriptor("negate",                formula("(-a)mc"),               NO_META);
    private static final Descriptor OP_PLUS                  = Descriptor.descriptor("plus",                  formula("(+a)mc"),               NO_META);
    private static final Descriptor OP_SQRT                  = Descriptor.descriptor("sqrt",                  formula("sqrt(a)mc"),            NO_META);
    private static final Descriptor OP_ROUND                 = Descriptor.descriptor("round",                 formula("round(a)mc"),           NO_META);
    private static final Descriptor OP_SET_SCALE             = Descriptor.descriptor("setScale",              formula("setScale(a)mc"),        NO_META);
    private static final Descriptor OP_SCALE_BY_POWER_OF_TEN = Descriptor.descriptor("scaleByPowerOfTen",    formula("a*10^n"),               NO_META);
    private static final Descriptor OP_STRIP_TRAILING_ZEROS  = Descriptor.descriptor("stripTrailingZeros",   formula("stripTrailingZeros(a)"), NO_META);
    private static final Descriptor OP_ULP                   = Descriptor.descriptor("ulp",                   formula("ulp(a)"),               NO_META);
    private static final Descriptor OP_MOVE_POINT_LEFT       = Descriptor.descriptor("movePointLeft",         formula("a*10^(-n)"),            NO_META);
    private static final Descriptor OP_MOVE_POINT_RIGHT      = Descriptor.descriptor("movePointRight",        formula("a*10^n"),               NO_META);
    private static final Descriptor OP_MAX                   = Descriptor.descriptor("max",                   formula("max(a,b)"),             NO_META);
    private static final Descriptor OP_MIN                   = Descriptor.descriptor("min",                   formula("min(a,b)"),             NO_META);

    public WrappedBigDecimal(Context context, VariableTrack variableTrack, BigDecimal value) {
        super(context, variableTrack, value);
    }

    public WrappedBigDecimal add(WrappedBigDecimal augend, WrappedMathContext mc, Descriptor resultDescriptor) {
        Objects.requireNonNull(augend, "augend");
        Objects.requireNonNull(mc, "mc");
        return (WrappedBigDecimal) execute(
                () -> getValue().add(augend.getValue(), mc.getValue()), List.of(this, augend, mc), OP_ADD, resultDescriptor);
    }

    public WrappedBigDecimal subtract(WrappedBigDecimal subtrahend, WrappedMathContext mc, Descriptor resultDescriptor) {
        Objects.requireNonNull(subtrahend, "subtrahend");
        Objects.requireNonNull(mc, "mc");
        return (WrappedBigDecimal) execute(
                () -> getValue().subtract(subtrahend.getValue(), mc.getValue()),
                List.of(this, subtrahend, mc), OP_SUBTRACT, resultDescriptor);
    }

    public WrappedBigDecimal multiply(WrappedBigDecimal multiplicand, WrappedMathContext mc, Descriptor resultDescriptor) {
        Objects.requireNonNull(multiplicand, "multiplicand");
        Objects.requireNonNull(mc, "mc");
        return (WrappedBigDecimal) execute(
                () -> getValue().multiply(multiplicand.getValue(), mc.getValue()),
                List.of(this, multiplicand, mc), OP_MULTIPLY, resultDescriptor);
    }

    public WrappedBigDecimal divide(WrappedBigDecimal divisor, WrappedMathContext mc, Descriptor resultDescriptor) {
        Objects.requireNonNull(divisor, "divisor");
        Objects.requireNonNull(mc, "mc");
        return (WrappedBigDecimal) execute(
                () -> getValue().divide(divisor.getValue(), mc.getValue()),
                List.of(this, divisor, mc), OP_DIVIDE, resultDescriptor);
    }

    public WrappedBigDecimal divideToIntegralValue(WrappedBigDecimal divisor, WrappedMathContext mc,
                                                   Descriptor resultDescriptor) {
        Objects.requireNonNull(divisor, "divisor");
        Objects.requireNonNull(mc, "mc");
        return (WrappedBigDecimal) execute(
                () -> getValue().divideToIntegralValue(divisor.getValue(), mc.getValue()),
                List.of(this, divisor, mc), OP_DIVIDE_TO_INTEGRAL, resultDescriptor);
    }

    public WrappedBigDecimal remainder(WrappedBigDecimal divisor, WrappedMathContext mc, Descriptor resultDescriptor) {
        Objects.requireNonNull(divisor, "divisor");
        Objects.requireNonNull(mc, "mc");
        return (WrappedBigDecimal) execute(
                () -> getValue().remainder(divisor.getValue(), mc.getValue()),
                List.of(this, divisor, mc), OP_REMAINDER, resultDescriptor);
    }

    public WrappedBigDecimal max(WrappedBigDecimal val, Descriptor resultDescriptor) {
        Objects.requireNonNull(val, "val");
        return (WrappedBigDecimal) execute(
                () -> getValue().max(val.getValue()), List.of(this, val), OP_MAX, resultDescriptor);
    }

    public WrappedBigDecimal min(WrappedBigDecimal val, Descriptor resultDescriptor) {
        Objects.requireNonNull(val, "val");
        return (WrappedBigDecimal) execute(
                () -> getValue().min(val.getValue()), List.of(this, val), OP_MIN, resultDescriptor);
    }

    public WrappedBigDecimal pow(WrappedInteger n, WrappedMathContext mc, Descriptor resultDescriptor) {
        Objects.requireNonNull(n, "n");
        Objects.requireNonNull(mc, "mc");
        return (WrappedBigDecimal) execute(
                () -> getValue().pow(n.getValue(), mc.getValue()), List.of(this, n, mc), OP_POW, resultDescriptor);
    }

    public WrappedBigDecimal abs(WrappedMathContext mc, Descriptor resultDescriptor) {
        Objects.requireNonNull(mc, "mc");
        return (WrappedBigDecimal) execute(
                () -> getValue().abs(mc.getValue()), List.of(this, mc), OP_ABS, resultDescriptor);
    }

    public WrappedBigDecimal negate(WrappedMathContext mc, Descriptor resultDescriptor) {
        Objects.requireNonNull(mc, "mc");
        return (WrappedBigDecimal) execute(
                () -> getValue().negate(mc.getValue()), List.of(this, mc), OP_NEGATE, resultDescriptor);
    }

    public WrappedBigDecimal plus(WrappedMathContext mc, Descriptor resultDescriptor) {
        Objects.requireNonNull(mc, "mc");
        return (WrappedBigDecimal) execute(
                () -> getValue().plus(mc.getValue()), List.of(this, mc), OP_PLUS, resultDescriptor);
    }

    public WrappedBigDecimal sqrt(WrappedMathContext mc, Descriptor resultDescriptor) {
        Objects.requireNonNull(mc, "mc");
        return (WrappedBigDecimal) execute(
                () -> getValue().sqrt(mc.getValue()), List.of(this, mc), OP_SQRT, resultDescriptor);
    }

    public WrappedBigDecimal round(WrappedMathContext mc, Descriptor resultDescriptor) {
        Objects.requireNonNull(mc, "mc");
        return (WrappedBigDecimal) execute(
                () -> getValue().round(mc.getValue()), List.of(this, mc), OP_ROUND, resultDescriptor);
    }

     public WrappedBigDecimal setScale(WrappedMathContext mc, Descriptor resultDescriptor) {
        Objects.requireNonNull(mc, "mc");
        return (WrappedBigDecimal) execute(
                () -> getValue().setScale(mc.getValue().getPrecision(), mc.getValue().getRoundingMode()),
                List.of(this, mc), OP_SET_SCALE, resultDescriptor);
    }

    public WrappedBigDecimal scaleByPowerOfTen(WrappedInteger n, Descriptor resultDescriptor) {
        Objects.requireNonNull(n, "n");
        return (WrappedBigDecimal) execute(
                () -> getValue().scaleByPowerOfTen(n.getValue()), List.of(this, n), OP_SCALE_BY_POWER_OF_TEN, resultDescriptor);
    }

    public WrappedBigDecimal stripTrailingZeros(Descriptor resultDescriptor) {
        return (WrappedBigDecimal) execute(
                () -> getValue().stripTrailingZeros(), List.of(this), OP_STRIP_TRAILING_ZEROS, resultDescriptor);
    }

    public WrappedBigDecimal ulp(Descriptor resultDescriptor) {
        return (WrappedBigDecimal) execute(
                () -> getValue().ulp(), List.of(this), OP_ULP, resultDescriptor);
    }

    public WrappedBigDecimal movePointLeft(WrappedInteger n, Descriptor resultDescriptor) {
        Objects.requireNonNull(n, "n");
        return (WrappedBigDecimal) execute(
                () -> getValue().movePointLeft(n.getValue()), List.of(this, n), OP_MOVE_POINT_LEFT, resultDescriptor);
    }

    public WrappedBigDecimal movePointRight(WrappedInteger n, Descriptor resultDescriptor) {
        Objects.requireNonNull(n, "n");
        return (WrappedBigDecimal) execute(
                () -> getValue().movePointRight(n.getValue()), List.of(this, n), OP_MOVE_POINT_RIGHT, resultDescriptor);
    }
}
