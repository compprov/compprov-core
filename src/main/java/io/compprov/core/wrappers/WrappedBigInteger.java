package io.compprov.core.wrappers;

import io.compprov.core.ComputationContext;
import io.compprov.core.meta.Descriptor;
import io.compprov.core.variable.AbstractWrappedVariable;
import io.compprov.core.variable.VariableTrack;
import io.compprov.core.wrappers.primitive.WrappedInteger;

import java.math.BigInteger;
import java.util.*;
import java.util.function.Function;

import static io.compprov.core.meta.Meta.formula;

/**
 * Provenance-tracked {@link BigInteger}. Every operation records itself into the owning context.
 */
public final class WrappedBigInteger extends AbstractWrappedVariable<BigInteger> {

    private static final Descriptor OP_ADD = Descriptor.descriptor("add", formula("a+b"));
    private static final Descriptor OP_SUBTRACT = Descriptor.descriptor("subtract", formula("a-b"));
    private static final Descriptor OP_MULTIPLY = Descriptor.descriptor("multiply", formula("a*b"));
    private static final Descriptor OP_DIVIDE = Descriptor.descriptor("divide", formula("a/b"));
    private static final Descriptor OP_REMAINDER = Descriptor.descriptor("remainder", formula("a%b"));
    private static final Descriptor OP_GCD = Descriptor.descriptor("gcd", formula("gcd(a,b)"));
    private static final Descriptor OP_MOD = Descriptor.descriptor("mod", formula("a mod m"));
    private static final Descriptor OP_MOD_POW = Descriptor.descriptor("modPow", formula("a^e mod m"));
    private static final Descriptor OP_MOD_INVERSE = Descriptor.descriptor("modInverse", formula("a^(-1) mod m"));
    private static final Descriptor OP_POW = Descriptor.descriptor("pow", formula("a^n"));
    private static final Descriptor OP_ABS = Descriptor.descriptor("abs", formula("|a|"));
    private static final Descriptor OP_NEGATE = Descriptor.descriptor("negate", formula("-a"));
    private static final Descriptor OP_NOT = Descriptor.descriptor("not", formula("~a"));
    private static final Descriptor OP_AND = Descriptor.descriptor("and", formula("a&b"));
    private static final Descriptor OP_OR = Descriptor.descriptor("or", formula("a|b"));
    private static final Descriptor OP_XOR = Descriptor.descriptor("xor", formula("a^b"));
    private static final Descriptor OP_AND_NOT = Descriptor.descriptor("andNot", formula("a&~b"));
    private static final Descriptor OP_SHIFT_LEFT = Descriptor.descriptor("shiftLeft", formula("a<<n"));
    private static final Descriptor OP_SHIFT_RIGHT = Descriptor.descriptor("shiftRight", formula("a>>n"));
    private static final Descriptor OP_SET_BIT = Descriptor.descriptor("setBit", formula("setBit(a,n)"));
    private static final Descriptor OP_CLEAR_BIT = Descriptor.descriptor("clearBit", formula("clearBit(a,n)"));
    private static final Descriptor OP_FLIP_BIT = Descriptor.descriptor("flipBit", formula("flipBit(a,n)"));
    private static final Descriptor OP_SQRT = Descriptor.descriptor("sqrt", formula("sqrt(a)"));
    private static final Descriptor OP_MAX = Descriptor.descriptor("max", formula("max(a,b)"));
    private static final Descriptor OP_MIN = Descriptor.descriptor("min", formula("min(a,b)"));

    private static final Map<Descriptor, Function<List<Object>, Object>> functionsMap;

    static {
        Map<Descriptor, Function<List<Object>, Object>> functions = new HashMap<>();

        functions.put(OP_ADD, (arguments) -> {
            BigInteger a = (BigInteger) arguments.get(0);
            BigInteger b = (BigInteger) arguments.get(1);
            return a.add(b);
        });

        functions.put(OP_SUBTRACT, (arguments) -> {
            BigInteger a = (BigInteger) arguments.get(0);
            BigInteger b = (BigInteger) arguments.get(1);
            return a.subtract(b);
        });

        functions.put(OP_MULTIPLY, (arguments) -> {
            BigInteger a = (BigInteger) arguments.get(0);
            BigInteger b = (BigInteger) arguments.get(1);
            return a.multiply(b);
        });

        functions.put(OP_DIVIDE, (arguments) -> {
            BigInteger a = (BigInteger) arguments.get(0);
            BigInteger b = (BigInteger) arguments.get(1);
            return a.divide(b);
        });

        functions.put(OP_REMAINDER, (arguments) -> {
            BigInteger a = (BigInteger) arguments.get(0);
            BigInteger b = (BigInteger) arguments.get(1);
            return a.remainder(b);
        });

        functions.put(OP_GCD, (arguments) -> {
            BigInteger a = (BigInteger) arguments.get(0);
            BigInteger b = (BigInteger) arguments.get(1);
            return a.gcd(b);
        });

        functions.put(OP_MOD, (arguments) -> {
            BigInteger a = (BigInteger) arguments.get(0);
            BigInteger m = (BigInteger) arguments.get(1);
            return a.mod(m);
        });

        functions.put(OP_MOD_POW, (arguments) -> {
            BigInteger a = (BigInteger) arguments.get(0);
            BigInteger e = (BigInteger) arguments.get(1);
            BigInteger m = (BigInteger) arguments.get(2);
            return a.modPow(e, m);
        });

        functions.put(OP_MOD_INVERSE, (arguments) -> {
            BigInteger a = (BigInteger) arguments.get(0);
            BigInteger m = (BigInteger) arguments.get(1);
            return a.modInverse(m);
        });

        functions.put(OP_POW, (arguments) -> {
            BigInteger a = (BigInteger) arguments.get(0);
            Integer n = (Integer) arguments.get(1);
            return a.pow(n);
        });

        functions.put(OP_ABS, (arguments) -> {
            BigInteger a = (BigInteger) arguments.get(0);
            return a.abs();
        });

        functions.put(OP_NEGATE, (arguments) -> {
            BigInteger a = (BigInteger) arguments.get(0);
            return a.negate();
        });

        functions.put(OP_NOT, (arguments) -> {
            BigInteger a = (BigInteger) arguments.get(0);
            return a.not();
        });

        functions.put(OP_AND, (arguments) -> {
            BigInteger a = (BigInteger) arguments.get(0);
            BigInteger b = (BigInteger) arguments.get(1);
            return a.and(b);
        });

        functions.put(OP_OR, (arguments) -> {
            BigInteger a = (BigInteger) arguments.get(0);
            BigInteger b = (BigInteger) arguments.get(1);
            return a.or(b);
        });

        functions.put(OP_XOR, (arguments) -> {
            BigInteger a = (BigInteger) arguments.get(0);
            BigInteger b = (BigInteger) arguments.get(1);
            return a.xor(b);
        });

        functions.put(OP_AND_NOT, (arguments) -> {
            BigInteger a = (BigInteger) arguments.get(0);
            BigInteger b = (BigInteger) arguments.get(1);
            return a.andNot(b);
        });

        functions.put(OP_SHIFT_LEFT, (arguments) -> {
            BigInteger a = (BigInteger) arguments.get(0);
            Integer n = (Integer) arguments.get(1);
            return a.shiftLeft(n);
        });

        functions.put(OP_SHIFT_RIGHT, (arguments) -> {
            BigInteger a = (BigInteger) arguments.get(0);
            Integer n = (Integer) arguments.get(1);
            return a.shiftRight(n);
        });

        functions.put(OP_SET_BIT, (arguments) -> {
            BigInteger a = (BigInteger) arguments.get(0);
            Integer n = (Integer) arguments.get(1);
            return a.setBit(n);
        });

        functions.put(OP_CLEAR_BIT, (arguments) -> {
            BigInteger a = (BigInteger) arguments.get(0);
            Integer n = (Integer) arguments.get(1);
            return a.clearBit(n);
        });

        functions.put(OP_FLIP_BIT, (arguments) -> {
            BigInteger a = (BigInteger) arguments.get(0);
            Integer n = (Integer) arguments.get(1);
            return a.flipBit(n);
        });

        functions.put(OP_SQRT, (arguments) -> {
            BigInteger a = (BigInteger) arguments.get(0);
            return a.sqrt();
        });

        functions.put(OP_MAX, (arguments) -> {
            BigInteger a = (BigInteger) arguments.get(0);
            BigInteger b = (BigInteger) arguments.get(1);
            return a.max(b);
        });

        functions.put(OP_MIN, (arguments) -> {
            BigInteger a = (BigInteger) arguments.get(0);
            BigInteger b = (BigInteger) arguments.get(1);
            return a.min(b);
        });

        functionsMap = Collections.unmodifiableMap(functions);
    }

    public WrappedBigInteger(ComputationContext context, VariableTrack variableTrack, BigInteger value) {
        super(context, variableTrack, value);
    }

    @Override
    public Function<List<Object>, Object> getFunction(Descriptor operationDescriptor) {
        return functionsMap.get(operationDescriptor);
    }

    public WrappedBigInteger add(WrappedBigInteger val, Descriptor resultDescriptor) {
        Objects.requireNonNull(val, "val");
        return (WrappedBigInteger) execute(
                OP_ADD,
                "a", this,
                "b", val,
                resultDescriptor);
    }

    public WrappedBigInteger subtract(WrappedBigInteger val, Descriptor resultDescriptor) {
        Objects.requireNonNull(val, "val");
        return (WrappedBigInteger) execute(
                OP_SUBTRACT,
                "a", this,
                "b", val,
                resultDescriptor);
    }

    public WrappedBigInteger multiply(WrappedBigInteger val, Descriptor resultDescriptor) {
        Objects.requireNonNull(val, "val");
        return (WrappedBigInteger) execute(
                OP_MULTIPLY,
                "a", this,
                "b", val,
                resultDescriptor);
    }

    public WrappedBigInteger divide(WrappedBigInteger val, Descriptor resultDescriptor) {
        Objects.requireNonNull(val, "val");
        return (WrappedBigInteger) execute(
                OP_DIVIDE,
                "a", this,
                "b", val,
                resultDescriptor);
    }

    public WrappedBigInteger remainder(WrappedBigInteger val, Descriptor resultDescriptor) {
        Objects.requireNonNull(val, "val");
        return (WrappedBigInteger) execute(
                OP_REMAINDER,
                "a", this,
                "b", val,
                resultDescriptor);
    }

    public WrappedBigInteger gcd(WrappedBigInteger val, Descriptor resultDescriptor) {
        Objects.requireNonNull(val, "val");
        return (WrappedBigInteger) execute(
                OP_GCD,
                "a", this,
                "b", val,
                resultDescriptor);
    }

    public WrappedBigInteger mod(WrappedBigInteger m, Descriptor resultDescriptor) {
        Objects.requireNonNull(m, "m");
        return (WrappedBigInteger) execute(
                OP_MOD,
                "a", this,
                "m", m,
                resultDescriptor);
    }

    public WrappedBigInteger modPow(WrappedBigInteger exponent, WrappedBigInteger m, Descriptor resultDescriptor) {
        Objects.requireNonNull(exponent, "exponent");
        Objects.requireNonNull(m, "m");
        return (WrappedBigInteger) execute(
                OP_MOD_POW,
                "a", this,
                "e", exponent,
                "m", m,
                resultDescriptor);
    }

    public WrappedBigInteger modInverse(WrappedBigInteger m, Descriptor resultDescriptor) {
        Objects.requireNonNull(m, "m");
        return (WrappedBigInteger) execute(
                OP_MOD_INVERSE,
                "a", this,
                "m", m,
                resultDescriptor);
    }

    public WrappedBigInteger pow(WrappedInteger exponent, Descriptor resultDescriptor) {
        Objects.requireNonNull(exponent, "exponent");
        return (WrappedBigInteger) execute(
                OP_POW,
                "a", this,
                "n", exponent,
                resultDescriptor);
    }

    public WrappedBigInteger abs(Descriptor resultDescriptor) {
        return (WrappedBigInteger) execute(
                OP_ABS,
                "a", this,
                resultDescriptor);
    }

    public WrappedBigInteger negate(Descriptor resultDescriptor) {
        return (WrappedBigInteger) execute(
                OP_NEGATE,
                "a", this,
                resultDescriptor);
    }

    public WrappedBigInteger not(Descriptor resultDescriptor) {
        return (WrappedBigInteger) execute(
                OP_NOT,
                "a", this,
                resultDescriptor);
    }

    public WrappedBigInteger and(WrappedBigInteger val, Descriptor resultDescriptor) {
        Objects.requireNonNull(val, "val");
        return (WrappedBigInteger) execute(
                OP_AND,
                "a", this,
                "b", val,
                resultDescriptor);
    }

    public WrappedBigInteger or(WrappedBigInteger val, Descriptor resultDescriptor) {
        Objects.requireNonNull(val, "val");
        return (WrappedBigInteger) execute(
                OP_OR,
                "a", this,
                "b", val,
                resultDescriptor);
    }

    public WrappedBigInteger xor(WrappedBigInteger val, Descriptor resultDescriptor) {
        Objects.requireNonNull(val, "val");
        return (WrappedBigInteger) execute(
                OP_XOR,
                "a", this,
                "b", val,
                resultDescriptor);
    }

    public WrappedBigInteger andNot(WrappedBigInteger val, Descriptor resultDescriptor) {
        Objects.requireNonNull(val, "val");
        return (WrappedBigInteger) execute(
                OP_AND_NOT,
                "a", this,
                "b", val,
                resultDescriptor);
    }

    public WrappedBigInteger shiftLeft(WrappedInteger n, Descriptor resultDescriptor) {
        Objects.requireNonNull(n, "n");
        return (WrappedBigInteger) execute(
                OP_SHIFT_LEFT,
                "a", this,
                "n", n,
                resultDescriptor);
    }

    public WrappedBigInteger shiftRight(WrappedInteger n, Descriptor resultDescriptor) {
        Objects.requireNonNull(n, "n");
        return (WrappedBigInteger) execute(
                OP_SHIFT_RIGHT,
                "a", this,
                "n", n,
                resultDescriptor);
    }

    public WrappedBigInteger setBit(WrappedInteger n, Descriptor resultDescriptor) {
        Objects.requireNonNull(n, "n");
        return (WrappedBigInteger) execute(
                OP_SET_BIT,
                "a", this,
                "n", n,
                resultDescriptor);
    }

    public WrappedBigInteger clearBit(WrappedInteger n, Descriptor resultDescriptor) {
        Objects.requireNonNull(n, "n");
        return (WrappedBigInteger) execute(
                OP_CLEAR_BIT,
                "a", this,
                "n", n,
                resultDescriptor);
    }

    public WrappedBigInteger flipBit(WrappedInteger n, Descriptor resultDescriptor) {
        Objects.requireNonNull(n, "n");
        return (WrappedBigInteger) execute(
                OP_FLIP_BIT,
                "a", this,
                "n", n,
                resultDescriptor);
    }

    public WrappedBigInteger sqrt(Descriptor resultDescriptor) {
        return (WrappedBigInteger) execute(
                OP_SQRT,
                "a", this,
                resultDescriptor);
    }

    public WrappedBigInteger max(WrappedBigInteger val, Descriptor resultDescriptor) {
        Objects.requireNonNull(val, "val");
        return (WrappedBigInteger) execute(
                OP_MAX,
                "a", this,
                "b", val,
                resultDescriptor);
    }

    public WrappedBigInteger min(WrappedBigInteger val, Descriptor resultDescriptor) {
        Objects.requireNonNull(val, "val");
        return (WrappedBigInteger) execute(
                OP_MIN,
                "a", this,
                "b", val,
                resultDescriptor);
    }
}
