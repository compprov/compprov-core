package io.compprov.core.wrappers;

import io.compprov.core.ComputationContext;
import io.compprov.core.meta.Descriptor;
import io.compprov.core.variable.AbstractWrappedVariable;
import io.compprov.core.variable.VariableTrack;
import io.compprov.core.wrappers.primitive.WrappedInteger;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;

import static io.compprov.core.meta.Meta.formula;

public final class WrappedBigDecimals extends AbstractWrappedVariable<BigDecimal[]> {

    private static final Descriptor OP_EXTRACT = Descriptor.descriptor("extract", formula("a[i]"));
    private static final Map<Descriptor, Function<List<Object>, Object>> functionsMap;

    static {
        Map<Descriptor, Function<List<Object>, Object>> functions = new HashMap<>();
        functions.put(OP_EXTRACT, (arguments) -> {
            BigDecimal[] a = (BigDecimal[]) arguments.get(0);
            Integer i = (Integer) arguments.get(1);
            return a[i];
        });
        functionsMap = Collections.unmodifiableMap(functions);
    }

    public WrappedBigDecimals(ComputationContext context, VariableTrack variableTrack, BigDecimal[] value) {
        super(context, variableTrack, value);
    }

    @Override
    public Function<List<Object>, Object> getFunction(Descriptor operationDescriptor) {
        return functionsMap.get(operationDescriptor);
    }

    public WrappedBigDecimal extract(WrappedInteger i, Descriptor resultDescriptor) {
        Objects.requireNonNull(i, "augend");
        return (WrappedBigDecimal) execute(
                OP_EXTRACT,
                "a", this,
                "i", i,
                resultDescriptor);
    }
}
