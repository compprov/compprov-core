package io.compprov.core.wrappers;

import io.compprov.core.ComputationContext;
import io.compprov.core.meta.Descriptor;
import io.compprov.core.variable.AbstractWrappedVariable;
import io.compprov.core.variable.VariableTrack;
import io.compprov.core.variable.WrappedVariable;
import io.compprov.core.wrappers.primitive.WrappedInteger;

import java.math.BigDecimal;
import java.math.MathContext;
import java.util.*;
import java.util.function.Function;

import static io.compprov.core.meta.Meta.formula;

/**
 * Provenance-tracked {@link BigDecimal}. Every operation records itself into the owning context.
 */
public final class WrappedBigDecimal extends AbstractWrappedVariable<BigDecimal> {

    private static final Descriptor OP_ADD = Descriptor.descriptor("add", formula("(a+b)mc"));
    private static final Descriptor OP_SUBTRACT = Descriptor.descriptor("subtract", formula("(a-b)mc"));
    private static final Descriptor OP_MULTIPLY = Descriptor.descriptor("multiply", formula("(a*b)mc"));
    private static final Descriptor OP_DIVIDE = Descriptor.descriptor("divide", formula("(a/b)mc"));
    private static final Descriptor OP_DIVIDE_TO_INTEGRAL = Descriptor.descriptor("divideToIntegralValue", formula("floor(a/b)mc"));
    private static final Descriptor OP_REMAINDER = Descriptor.descriptor("remainder", formula("(a%b)mc"));
    private static final Descriptor OP_POW = Descriptor.descriptor("pow", formula("(a^n)mc"));
    private static final Descriptor OP_ABS = Descriptor.descriptor("abs", formula("|a|mc"));
    private static final Descriptor OP_NEGATE = Descriptor.descriptor("negate", formula("(-a)mc"));
    private static final Descriptor OP_PLUS = Descriptor.descriptor("plus", formula("(+a)mc"));
    private static final Descriptor OP_SQRT = Descriptor.descriptor("sqrt", formula("sqrt(a)mc"));
    private static final Descriptor OP_ROUND = Descriptor.descriptor("round", formula("round(a)mc"));
    private static final Descriptor OP_SET_SCALE = Descriptor.descriptor("setScale", formula("setScale(a)mc"));
    private static final Descriptor OP_SCALE_BY_POWER_OF_TEN = Descriptor.descriptor("scaleByPowerOfTen", formula("a*10^n"));
    private static final Descriptor OP_STRIP_TRAILING_ZEROS = Descriptor.descriptor("stripTrailingZeros", formula("stripTrailingZeros(a)"));
    private static final Descriptor OP_ULP = Descriptor.descriptor("ulp", formula("ulp(a)"));
    private static final Descriptor OP_MOVE_POINT_LEFT = Descriptor.descriptor("movePointLeft", formula("a*10^(-n)"));
    private static final Descriptor OP_MOVE_POINT_RIGHT = Descriptor.descriptor("movePointRight", formula("a*10^n"));
    private static final Descriptor OP_MAX = Descriptor.descriptor("max", formula("max(a,b)"));
    private static final Descriptor OP_MIN = Descriptor.descriptor("min", formula("min(a,b)"));
    private static final Descriptor OP_ADD_BULK = Descriptor.descriptor("addBulk", formula("(a+b0+...+bn)mc"));
    private static final Descriptor OP_SUBTRACT_BULK = Descriptor.descriptor("addBulk", formula("(a-b0-...-bn)mc"));
    private static final Descriptor OP_MULTIPLY_BULK = Descriptor.descriptor("multiplyBulk", formula("(a*b0*...*bn)mc"));
    private static final Descriptor OP_MAX_BULK = Descriptor.descriptor("maxBulk", formula("max(a,b0,...,bn)"));
    private static final Descriptor OP_MIN_BULK = Descriptor.descriptor("minBulk", formula("min(a,b0,...,bn)"));

    private static final Map<Descriptor, Function<List<Object>, Object>> functionsMap;

    static {
        Map<Descriptor, Function<List<Object>, Object>> functions = new HashMap<>();
        functions.put(OP_ADD, (arguments) -> {
            BigDecimal a = (BigDecimal) arguments.get(0);
            BigDecimal b = (BigDecimal) arguments.get(1);
            MathContext mc = (MathContext) arguments.get(2);
            return a.add(b, mc);
        });

        functions.put(OP_SUBTRACT, (arguments) -> {
            BigDecimal a = (BigDecimal) arguments.get(0);
            BigDecimal b = (BigDecimal) arguments.get(1);
            MathContext mc = (MathContext) arguments.get(2);
            return a.subtract(b, mc);
        });

        functions.put(OP_MULTIPLY, (arguments) -> {
            BigDecimal a = (BigDecimal) arguments.get(0);
            BigDecimal b = (BigDecimal) arguments.get(1);
            MathContext mc = (MathContext) arguments.get(2);
            return a.multiply(b, mc);
        });

        functions.put(OP_DIVIDE, (arguments) -> {
            BigDecimal a = (BigDecimal) arguments.get(0);
            BigDecimal b = (BigDecimal) arguments.get(1);
            MathContext mc = (MathContext) arguments.get(2);
            return a.divide(b, mc);
        });

        functions.put(OP_DIVIDE_TO_INTEGRAL, (arguments) -> {
            BigDecimal a = (BigDecimal) arguments.get(0);
            BigDecimal b = (BigDecimal) arguments.get(1);
            MathContext mc = (MathContext) arguments.get(2);
            return a.divideToIntegralValue(b, mc);
        });

        functions.put(OP_REMAINDER, (arguments) -> {
            BigDecimal a = (BigDecimal) arguments.get(0);
            BigDecimal b = (BigDecimal) arguments.get(1);
            MathContext mc = (MathContext) arguments.get(2);
            return a.remainder(b, mc);
        });

        functions.put(OP_POW, (arguments) -> {
            BigDecimal a = (BigDecimal) arguments.get(0);
            Integer n = (Integer) arguments.get(1);
            MathContext mc = (MathContext) arguments.get(2);
            return a.pow(n, mc);
        });

        functions.put(OP_ABS, (arguments) -> {
            BigDecimal a = (BigDecimal) arguments.get(0);
            MathContext mc = (MathContext) arguments.get(1);
            return a.abs(mc);
        });

        functions.put(OP_NEGATE, (arguments) -> {
            BigDecimal a = (BigDecimal) arguments.get(0);
            MathContext mc = (MathContext) arguments.get(1);
            return a.negate(mc);
        });

        functions.put(OP_PLUS, (arguments) -> {
            BigDecimal a = (BigDecimal) arguments.get(0);
            MathContext mc = (MathContext) arguments.get(1);
            return a.plus(mc);
        });

        functions.put(OP_SQRT, (arguments) -> {
            BigDecimal a = (BigDecimal) arguments.get(0);
            MathContext mc = (MathContext) arguments.get(1);
            return a.sqrt(mc);
        });

        functions.put(OP_ROUND, (arguments) -> {
            BigDecimal a = (BigDecimal) arguments.get(0);
            MathContext mc = (MathContext) arguments.get(1);
            return a.round(mc);
        });

        functions.put(OP_SET_SCALE, (arguments) -> {
            BigDecimal a = (BigDecimal) arguments.get(0);
            MathContext mc = (MathContext) arguments.get(1);
            return a.setScale(mc.getPrecision(), mc.getRoundingMode());
        });

        functions.put(OP_SCALE_BY_POWER_OF_TEN, (arguments) -> {
            BigDecimal a = (BigDecimal) arguments.get(0);
            Integer n = (Integer) arguments.get(1);
            return a.scaleByPowerOfTen(n);
        });

        functions.put(OP_STRIP_TRAILING_ZEROS, (arguments) -> {
            BigDecimal a = (BigDecimal) arguments.get(0);
            return a.stripTrailingZeros();
        });

        functions.put(OP_ULP, (arguments) -> {
            BigDecimal a = (BigDecimal) arguments.get(0);
            return a.ulp();
        });

        functions.put(OP_MOVE_POINT_LEFT, (arguments) -> {
            BigDecimal a = (BigDecimal) arguments.get(0);
            Integer n = (Integer) arguments.get(1);
            return a.movePointLeft(n);
        });

        functions.put(OP_MOVE_POINT_RIGHT, (arguments) -> {
            BigDecimal a = (BigDecimal) arguments.get(0);
            Integer n = (Integer) arguments.get(1);
            return a.movePointRight(n);
        });

        functions.put(OP_MAX, (arguments) -> {
            BigDecimal a = (BigDecimal) arguments.get(0);
            BigDecimal b = (BigDecimal) arguments.get(1);
            return a.max(b);
        });

        functions.put(OP_MIN, (arguments) -> {
            BigDecimal a = (BigDecimal) arguments.get(0);
            BigDecimal b = (BigDecimal) arguments.get(1);
            return a.min(b);
        });

        functions.put(OP_ADD_BULK, (arguments) -> {
            BigDecimal result = (BigDecimal) arguments.get(0);
            MathContext mc = (MathContext) arguments.get(arguments.size() - 1);
            for (int i = 1; i < arguments.size() - 1; i++) {
                result = result.add((BigDecimal) arguments.get(i), mc);
            }
            return result;
        });

        functions.put(OP_SUBTRACT_BULK, (arguments) -> {
            BigDecimal result = (BigDecimal) arguments.get(0);
            MathContext mc = (MathContext) arguments.get(arguments.size() - 1);
            for (int i = 1; i < arguments.size() - 1; i++) {
                result = result.subtract((BigDecimal) arguments.get(i), mc);
            }
            return result;
        });

        functions.put(OP_MULTIPLY_BULK, (arguments) -> {
            BigDecimal result = (BigDecimal) arguments.get(0);
            MathContext mc = (MathContext) arguments.get(arguments.size() - 1);
            for (int i = 1; i < arguments.size() - 1; i++) {
                result = result.multiply((BigDecimal) arguments.get(i), mc);
            }
            return result;
        });

        functions.put(OP_MAX_BULK, (arguments) -> {
            BigDecimal result = (BigDecimal) arguments.get(0);
            for (int i = 1; i < arguments.size(); i++) {
                result = result.max((BigDecimal) arguments.get(i));
            }
            return result;
        });

        functions.put(OP_MIN_BULK, (arguments) -> {
            BigDecimal result = (BigDecimal) arguments.get(0);
            for (int i = 1; i < arguments.size(); i++) {
                result = result.min((BigDecimal) arguments.get(i));
            }
            return result;
        });

        functionsMap = Collections.unmodifiableMap(functions);
    }

    public WrappedBigDecimal(ComputationContext context, VariableTrack variableTrack, BigDecimal value) {
        super(context, variableTrack, value);
    }

    @Override
    public Function<List<Object>, Object> getFunction(Descriptor operationDescriptor) {
        return functionsMap.get(operationDescriptor);
    }

    public WrappedBigDecimal add(WrappedBigDecimal augend, WrappedMathContext mc, Descriptor resultDescriptor) {
        Objects.requireNonNull(augend, "augend");
        Objects.requireNonNull(mc, "mc");
        return (WrappedBigDecimal) execute(
                OP_ADD,
                "a", this,
                "b", augend,
                "mc", mc,
                resultDescriptor);
    }

    public WrappedBigDecimal subtract(WrappedBigDecimal subtrahend, WrappedMathContext mc, Descriptor resultDescriptor) {
        Objects.requireNonNull(subtrahend, "subtrahend");
        Objects.requireNonNull(mc, "mc");
        return (WrappedBigDecimal) execute(
                OP_SUBTRACT,
                "a", this,
                "b", subtrahend,
                "mc", mc,
                resultDescriptor);
    }

    public WrappedBigDecimal multiply(WrappedBigDecimal multiplicand, WrappedMathContext mc, Descriptor resultDescriptor) {
        Objects.requireNonNull(multiplicand, "multiplicand");
        Objects.requireNonNull(mc, "mc");
        return (WrappedBigDecimal) execute(
                OP_MULTIPLY,
                "a", this,
                "b", multiplicand,
                "mc", mc,
                resultDescriptor);
    }

    public WrappedBigDecimal divide(WrappedBigDecimal divisor, WrappedMathContext mc, Descriptor resultDescriptor) {
        Objects.requireNonNull(divisor, "divisor");
        Objects.requireNonNull(mc, "mc");
        return (WrappedBigDecimal) execute(
                OP_DIVIDE,
                "a", this,
                "b", divisor,
                "mc", mc,
                resultDescriptor);
    }

    public WrappedBigDecimal divideToIntegralValue(WrappedBigDecimal divisor, WrappedMathContext mc,
                                                   Descriptor resultDescriptor) {
        Objects.requireNonNull(divisor, "divisor");
        Objects.requireNonNull(mc, "mc");
        return (WrappedBigDecimal) execute(
                OP_DIVIDE_TO_INTEGRAL,
                "a", this,
                "b", divisor,
                "mc", mc,
                resultDescriptor);
    }

    public WrappedBigDecimal remainder(WrappedBigDecimal divisor, WrappedMathContext mc, Descriptor resultDescriptor) {
        Objects.requireNonNull(divisor, "divisor");
        Objects.requireNonNull(mc, "mc");
        return (WrappedBigDecimal) execute(
                OP_REMAINDER,
                "a", this,
                "b", divisor,
                "mc", mc,
                resultDescriptor);
    }

    public WrappedBigDecimal max(WrappedBigDecimal val, Descriptor resultDescriptor) {
        Objects.requireNonNull(val, "val");
        return (WrappedBigDecimal) execute(
                OP_MAX,
                "a", this,
                "b", val,
                resultDescriptor);
    }

    public WrappedBigDecimal min(WrappedBigDecimal val, Descriptor resultDescriptor) {
        Objects.requireNonNull(val, "val");
        return (WrappedBigDecimal) execute(
                OP_MIN,
                "a", this,
                "b", val,
                resultDescriptor);
    }

    public WrappedBigDecimal pow(WrappedInteger n, WrappedMathContext mc, Descriptor resultDescriptor) {
        Objects.requireNonNull(n, "n");
        Objects.requireNonNull(mc, "mc");
        return (WrappedBigDecimal) execute(
                OP_POW,
                "a", this,
                "n", n,
                "mc", mc,
                resultDescriptor);
    }

    public WrappedBigDecimal abs(WrappedMathContext mc, Descriptor resultDescriptor) {
        Objects.requireNonNull(mc, "mc");
        return (WrappedBigDecimal) execute(
                OP_ABS,
                "a", this,
                "mc", mc,
                resultDescriptor);
    }

    public WrappedBigDecimal negate(WrappedMathContext mc, Descriptor resultDescriptor) {
        Objects.requireNonNull(mc, "mc");
        return (WrappedBigDecimal) execute(
                OP_NEGATE,
                "a", this,
                "mc", mc,
                resultDescriptor);
    }

    public WrappedBigDecimal plus(WrappedMathContext mc, Descriptor resultDescriptor) {
        Objects.requireNonNull(mc, "mc");
        return (WrappedBigDecimal) execute(
                OP_PLUS,
                "a", this,
                "mc", mc,
                resultDescriptor);
    }

    public WrappedBigDecimal sqrt(WrappedMathContext mc, Descriptor resultDescriptor) {
        Objects.requireNonNull(mc, "mc");
        return (WrappedBigDecimal) execute(
                OP_SQRT,
                "a", this,
                "mc", mc,
                resultDescriptor);
    }

    public WrappedBigDecimal round(WrappedMathContext mc, Descriptor resultDescriptor) {
        Objects.requireNonNull(mc, "mc");
        return (WrappedBigDecimal) execute(
                OP_ROUND,
                "a", this,
                "mc", mc,
                resultDescriptor);
    }

    public WrappedBigDecimal setScale(WrappedMathContext mc, Descriptor resultDescriptor) {
        Objects.requireNonNull(mc, "mc");
        return (WrappedBigDecimal) execute(
                OP_SET_SCALE,
                "a", this,
                "mc", mc,
                resultDescriptor);
    }

    public WrappedBigDecimal scaleByPowerOfTen(WrappedInteger n, Descriptor resultDescriptor) {
        Objects.requireNonNull(n, "n");
        return (WrappedBigDecimal) execute(
                OP_SCALE_BY_POWER_OF_TEN,
                "a", this,
                "n", n,
                resultDescriptor);
    }

    public WrappedBigDecimal stripTrailingZeros(Descriptor resultDescriptor) {
        return (WrappedBigDecimal) execute(
                OP_STRIP_TRAILING_ZEROS,
                "a", this,
                resultDescriptor);
    }

    public WrappedBigDecimal ulp(Descriptor resultDescriptor) {
        return (WrappedBigDecimal) execute(
                OP_ULP,
                "a", this,
                resultDescriptor);
    }

    public WrappedBigDecimal movePointLeft(WrappedInteger n, Descriptor resultDescriptor) {
        Objects.requireNonNull(n, "n");
        return (WrappedBigDecimal) execute(
                OP_MOVE_POINT_LEFT,
                "a", this,
                "n", n,
                resultDescriptor);
    }

    public WrappedBigDecimal movePointRight(WrappedInteger n, Descriptor resultDescriptor) {
        Objects.requireNonNull(n, "n");
        return (WrappedBigDecimal) execute(
                OP_MOVE_POINT_RIGHT,
                "a", this,
                "n", n,
                resultDescriptor);
    }

    public WrappedBigDecimal addBulk(List<WrappedBigDecimal> values, WrappedMathContext mc, Descriptor resultDescriptor) {
        Objects.requireNonNull(values, "val");
        LinkedHashMap<String, WrappedVariable> arguments = new LinkedHashMap<>();
        arguments.put("a", this);
        for (int i = 0; i < values.size(); i++) {
            arguments.put("b" + i, values.get(i));
        }
        arguments.put("mc", mc);
        return (WrappedBigDecimal) execute(
                OP_ADD_BULK,
                arguments,
                resultDescriptor);
    }

    public WrappedBigDecimal multiplyBulk(List<WrappedBigDecimal> values, WrappedMathContext mc, Descriptor resultDescriptor) {
        Objects.requireNonNull(values, "val");
        LinkedHashMap<String, WrappedVariable> arguments = new LinkedHashMap<>();
        arguments.put("a", this);
        for (int i = 0; i < values.size(); i++) {
            arguments.put("b" + i, values.get(i));
        }
        arguments.put("mc", mc);
        return (WrappedBigDecimal) execute(
                OP_MULTIPLY_BULK,
                arguments,
                resultDescriptor);
    }

    public WrappedBigDecimal subtractBulk(List<WrappedBigDecimal> values, WrappedMathContext mc, Descriptor resultDescriptor) {
        Objects.requireNonNull(values, "val");
        LinkedHashMap<String, WrappedVariable> arguments = new LinkedHashMap<>();
        arguments.put("a", this);
        for (int i = 0; i < values.size(); i++) {
            arguments.put("b" + i, values.get(i));
        }
        arguments.put("mc", mc);
        return (WrappedBigDecimal) execute(
                OP_SUBTRACT_BULK,
                arguments,
                resultDescriptor);
    }

    public WrappedBigDecimal maxBulk(List<WrappedBigDecimal> values, Descriptor resultDescriptor) {
        Objects.requireNonNull(values, "values");
        LinkedHashMap<String, WrappedVariable> arguments = new LinkedHashMap<>();
        for (int i = 0; i < values.size(); i++) {
            arguments.put("b" + i, values.get(i));
        }
        return (WrappedBigDecimal) execute(OP_MAX_BULK, arguments, resultDescriptor);
    }

    public WrappedBigDecimal minBulk(List<WrappedBigDecimal> values, Descriptor resultDescriptor) {
        Objects.requireNonNull(values, "values");
        LinkedHashMap<String, WrappedVariable> arguments = new LinkedHashMap<>();
        for (int i = 0; i < values.size(); i++) {
            arguments.put("b" + i, values.get(i));
        }
        return (WrappedBigDecimal) execute(OP_MIN_BULK, arguments, resultDescriptor);
    }
}
