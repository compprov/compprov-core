package io.compprov.core.wrappers;

import io.compprov.core.Context;
import io.compprov.core.Descriptor;
import io.compprov.core.variable.AbstractWrappedVariable;
import io.compprov.core.variable.VariableTrack;
import io.compprov.core.wrappers.primitive.WrappedInteger;

import java.math.BigInteger;
import java.util.List;
import java.util.Objects;

import static io.compprov.core.meta.Meta.NO_META;
import static io.compprov.core.meta.MetaFormula.formula;

/**
 * Provenance-tracked {@link BigInteger}. Every operation records itself into the owning context.
 */
public final class WrappedBigInteger extends AbstractWrappedVariable<BigInteger> {

    private static final Descriptor OP_ADD         = Descriptor.descriptor("add",         formula("a+b"),           NO_META);
    private static final Descriptor OP_SUBTRACT    = Descriptor.descriptor("subtract",    formula("a-b"),           NO_META);
    private static final Descriptor OP_MULTIPLY    = Descriptor.descriptor("multiply",    formula("a*b"),           NO_META);
    private static final Descriptor OP_DIVIDE      = Descriptor.descriptor("divide",      formula("a/b"),           NO_META);
    private static final Descriptor OP_REMAINDER   = Descriptor.descriptor("remainder",   formula("a%b"),           NO_META);
    private static final Descriptor OP_GCD         = Descriptor.descriptor("gcd",         formula("gcd(a,b)"),      NO_META);
    private static final Descriptor OP_MOD         = Descriptor.descriptor("mod",         formula("a mod m"),       NO_META);
    private static final Descriptor OP_MOD_POW     = Descriptor.descriptor("modPow",      formula("a^e mod m"),     NO_META);
    private static final Descriptor OP_MOD_INVERSE = Descriptor.descriptor("modInverse",  formula("a^(-1) mod m"),  NO_META);
    private static final Descriptor OP_POW         = Descriptor.descriptor("pow",         formula("a^n"),           NO_META);
    private static final Descriptor OP_ABS         = Descriptor.descriptor("abs",         formula("|a|"),           NO_META);
    private static final Descriptor OP_NEGATE      = Descriptor.descriptor("negate",      formula("-a"),            NO_META);
    private static final Descriptor OP_NOT         = Descriptor.descriptor("not",         formula("~a"),            NO_META);
    private static final Descriptor OP_AND         = Descriptor.descriptor("and",         formula("a&b"),           NO_META);
    private static final Descriptor OP_OR          = Descriptor.descriptor("or",          formula("a|b"),           NO_META);
    private static final Descriptor OP_XOR         = Descriptor.descriptor("xor",         formula("a^b"),           NO_META);
    private static final Descriptor OP_AND_NOT     = Descriptor.descriptor("andNot",      formula("a&~b"),          NO_META);
    private static final Descriptor OP_SHIFT_LEFT  = Descriptor.descriptor("shiftLeft",   formula("a<<n"),          NO_META);
    private static final Descriptor OP_SHIFT_RIGHT = Descriptor.descriptor("shiftRight",  formula("a>>n"),          NO_META);
    private static final Descriptor OP_SET_BIT     = Descriptor.descriptor("setBit",      formula("setBit(a,n)"),   NO_META);
    private static final Descriptor OP_CLEAR_BIT   = Descriptor.descriptor("clearBit",    formula("clearBit(a,n)"), NO_META);
    private static final Descriptor OP_FLIP_BIT    = Descriptor.descriptor("flipBit",     formula("flipBit(a,n)"),  NO_META);
    private static final Descriptor OP_SQRT        = Descriptor.descriptor("sqrt",        formula("sqrt(a)"),       NO_META);
    private static final Descriptor OP_MAX         = Descriptor.descriptor("max",         formula("max(a,b)"),      NO_META);
    private static final Descriptor OP_MIN         = Descriptor.descriptor("min",         formula("min(a,b)"),      NO_META);

    public WrappedBigInteger(Context context, VariableTrack variableTrack, BigInteger value) {
        super(context, variableTrack, value);
    }

    public WrappedBigInteger add(WrappedBigInteger val, Descriptor resultDescriptor) {
        Objects.requireNonNull(val, "val");
        return (WrappedBigInteger) execute(
                () -> getValue().add(val.getValue()), List.of(this, val), OP_ADD, resultDescriptor);
    }

    public WrappedBigInteger subtract(WrappedBigInteger val, Descriptor resultDescriptor) {
        Objects.requireNonNull(val, "val");
        return (WrappedBigInteger) execute(
                () -> getValue().subtract(val.getValue()), List.of(this, val), OP_SUBTRACT, resultDescriptor);
    }

    public WrappedBigInteger multiply(WrappedBigInteger val, Descriptor resultDescriptor) {
        Objects.requireNonNull(val, "val");
        return (WrappedBigInteger) execute(
                () -> getValue().multiply(val.getValue()), List.of(this, val), OP_MULTIPLY, resultDescriptor);
    }

    public WrappedBigInteger divide(WrappedBigInteger val, Descriptor resultDescriptor) {
        Objects.requireNonNull(val, "val");
        return (WrappedBigInteger) execute(
                () -> getValue().divide(val.getValue()), List.of(this, val), OP_DIVIDE, resultDescriptor);
    }

    public WrappedBigInteger remainder(WrappedBigInteger val, Descriptor resultDescriptor) {
        Objects.requireNonNull(val, "val");
        return (WrappedBigInteger) execute(
                () -> getValue().remainder(val.getValue()), List.of(this, val), OP_REMAINDER, resultDescriptor);
    }

    public WrappedBigInteger gcd(WrappedBigInteger val, Descriptor resultDescriptor) {
        Objects.requireNonNull(val, "val");
        return (WrappedBigInteger) execute(
                () -> getValue().gcd(val.getValue()), List.of(this, val), OP_GCD, resultDescriptor);
    }

    public WrappedBigInteger mod(WrappedBigInteger m, Descriptor resultDescriptor) {
        Objects.requireNonNull(m, "m");
        return (WrappedBigInteger) execute(
                () -> getValue().mod(m.getValue()), List.of(this, m), OP_MOD, resultDescriptor);
    }

    public WrappedBigInteger modPow(WrappedBigInteger exponent, WrappedBigInteger m, Descriptor resultDescriptor) {
        Objects.requireNonNull(exponent, "exponent");
        Objects.requireNonNull(m, "m");
        return (WrappedBigInteger) execute(
                () -> getValue().modPow(exponent.getValue(), m.getValue()),
                List.of(this, exponent, m), OP_MOD_POW, resultDescriptor);
    }

    public WrappedBigInteger modInverse(WrappedBigInteger m, Descriptor resultDescriptor) {
        Objects.requireNonNull(m, "m");
        return (WrappedBigInteger) execute(
                () -> getValue().modInverse(m.getValue()), List.of(this, m), OP_MOD_INVERSE, resultDescriptor);
    }

    public WrappedBigInteger pow(WrappedInteger exponent, Descriptor resultDescriptor) {
        Objects.requireNonNull(exponent, "exponent");
        return (WrappedBigInteger) execute(
                () -> getValue().pow(exponent.getValue()), List.of(this, exponent), OP_POW, resultDescriptor);
    }

    public WrappedBigInteger abs(Descriptor resultDescriptor) {
        return (WrappedBigInteger) execute(
                () -> getValue().abs(), List.of(this), OP_ABS, resultDescriptor);
    }

    public WrappedBigInteger negate(Descriptor resultDescriptor) {
        return (WrappedBigInteger) execute(
                () -> getValue().negate(), List.of(this), OP_NEGATE, resultDescriptor);
    }

    public WrappedBigInteger not(Descriptor resultDescriptor) {
        return (WrappedBigInteger) execute(
                () -> getValue().not(), List.of(this), OP_NOT, resultDescriptor);
    }

    public WrappedBigInteger and(WrappedBigInteger val, Descriptor resultDescriptor) {
        Objects.requireNonNull(val, "val");
        return (WrappedBigInteger) execute(
                () -> getValue().and(val.getValue()), List.of(this, val), OP_AND, resultDescriptor);
    }

    public WrappedBigInteger or(WrappedBigInteger val, Descriptor resultDescriptor) {
        Objects.requireNonNull(val, "val");
        return (WrappedBigInteger) execute(
                () -> getValue().or(val.getValue()), List.of(this, val), OP_OR, resultDescriptor);
    }

    public WrappedBigInteger xor(WrappedBigInteger val, Descriptor resultDescriptor) {
        Objects.requireNonNull(val, "val");
        return (WrappedBigInteger) execute(
                () -> getValue().xor(val.getValue()), List.of(this, val), OP_XOR, resultDescriptor);
    }

    public WrappedBigInteger andNot(WrappedBigInteger val, Descriptor resultDescriptor) {
        Objects.requireNonNull(val, "val");
        return (WrappedBigInteger) execute(
                () -> getValue().andNot(val.getValue()), List.of(this, val), OP_AND_NOT, resultDescriptor);
    }

    public WrappedBigInteger shiftLeft(WrappedInteger n, Descriptor resultDescriptor) {
        Objects.requireNonNull(n, "n");
        return (WrappedBigInteger) execute(
                () -> getValue().shiftLeft(n.getValue()), List.of(this, n), OP_SHIFT_LEFT, resultDescriptor);
    }

    public WrappedBigInteger shiftRight(WrappedInteger n, Descriptor resultDescriptor) {
        Objects.requireNonNull(n, "n");
        return (WrappedBigInteger) execute(
                () -> getValue().shiftRight(n.getValue()), List.of(this, n), OP_SHIFT_RIGHT, resultDescriptor);
    }

    public WrappedBigInteger setBit(WrappedInteger n, Descriptor resultDescriptor) {
        Objects.requireNonNull(n, "n");
        return (WrappedBigInteger) execute(
                () -> getValue().setBit(n.getValue()), List.of(this, n), OP_SET_BIT, resultDescriptor);
    }

    public WrappedBigInteger clearBit(WrappedInteger n, Descriptor resultDescriptor) {
        Objects.requireNonNull(n, "n");
        return (WrappedBigInteger) execute(
                () -> getValue().clearBit(n.getValue()), List.of(this, n), OP_CLEAR_BIT, resultDescriptor);
    }

    public WrappedBigInteger flipBit(WrappedInteger n, Descriptor resultDescriptor) {
        Objects.requireNonNull(n, "n");
        return (WrappedBigInteger) execute(
                () -> getValue().flipBit(n.getValue()), List.of(this, n), OP_FLIP_BIT, resultDescriptor);
    }

    public WrappedBigInteger sqrt(Descriptor resultDescriptor) {
        return (WrappedBigInteger) execute(
                () -> getValue().sqrt(), List.of(this), OP_SQRT, resultDescriptor);
    }

    public WrappedBigInteger max(WrappedBigInteger val, Descriptor resultDescriptor) {
        Objects.requireNonNull(val, "val");
        return (WrappedBigInteger) execute(
                () -> getValue().max(val.getValue()), List.of(this, val), OP_MAX, resultDescriptor);
    }

    public WrappedBigInteger min(WrappedBigInteger val, Descriptor resultDescriptor) {
        Objects.requireNonNull(val, "val");
        return (WrappedBigInteger) execute(
                () -> getValue().min(val.getValue()), List.of(this, val), OP_MIN, resultDescriptor);
    }
}
