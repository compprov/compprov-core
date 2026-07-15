package io.compprov.examples.nav.wrapped;

import io.compprov.core.ComputationContext;
import io.compprov.core.meta.Descriptor;
import io.compprov.core.operation.WrappedArgument;
import io.compprov.core.variable.AbstractWrappedVariable;
import io.compprov.core.variable.VariableTrack;
import io.compprov.examples.nav.model.Amount;
import io.compprov.examples.nav.model.Rate;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;

import static io.compprov.core.meta.Meta.formula;

public class WrappedAmount extends AbstractWrappedVariable<Amount> {
    private static final Descriptor OP_ADD = Descriptor.descriptor("add", formula("a+b"));
    private static final Descriptor OP_ADD_BULK = Descriptor.descriptor("addBulk", formula("a+b0+...+bn"));
    private static final Descriptor OP_CONVERT = Descriptor.descriptor("convert", formula("convert(a,r)"));

    private static final Map<Descriptor, Function<List<Object>, Object>> functionsMap;

    static {
        Map<Descriptor, Function<List<Object>, Object>> functions = new HashMap<>();

        functions.put(OP_ADD, (arguments) -> {
            Amount a = (Amount) arguments.get(0);
            Amount b = (Amount) arguments.get(1);
            return a.add(b);
        });

        functions.put(OP_CONVERT, (arguments) -> {
            Amount a = (Amount) arguments.get(0);
            Rate r = (Rate) arguments.get(1);
            return a.convert(r);
        });

        functions.put(OP_ADD_BULK, (arguments) -> {
            Amount result = (Amount) arguments.get(0);
            for (int i = 1; i < arguments.size(); i++) {
                result = result.add((Amount) arguments.get(i));
            }
            return result;
        });

        functionsMap = Collections.unmodifiableMap(functions);
    }

    public WrappedAmount(ComputationContext context, VariableTrack variableTrack, Amount value) {
        super(context, variableTrack, value);
    }

    @Override
    public Function<List<Object>, Object> getFunction(Descriptor operationDescriptor) {
        return functionsMap.get(operationDescriptor);
    }

    public WrappedAmount add(WrappedAmount val, Descriptor resultDescriptor) {
        Objects.requireNonNull(val, "val");
        return (WrappedAmount) execute(
                OP_ADD,
                "a", this,
                "b", val,
                resultDescriptor);
    }

    public WrappedAmount convert(WrappedRate rate, Descriptor resultDescriptor) {
        Objects.requireNonNull(rate, "rate");
        return (WrappedAmount) execute(
                OP_CONVERT,
                "a", this,
                "r", rate,
                resultDescriptor);
    }

    public WrappedAmount addBulk(List<WrappedAmount> values, Descriptor resultDescriptor) {
        Objects.requireNonNull(values, "val");
        final var arguments = new ArrayList<WrappedArgument>(values.size() + 1);
        arguments.add(new WrappedArgument("a", this));
        for (int i = 0; i < values.size(); i++) {
            arguments.add(new WrappedArgument("b" + i, values.get(i)));
        }
        return (WrappedAmount) execute(
                OP_ADD_BULK,
                arguments,
                resultDescriptor);
    }
}
