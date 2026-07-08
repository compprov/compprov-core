package io.compprov.core.wrappers.subgraph;

import io.compprov.core.ComputationContext;
import io.compprov.core.Subgraph;
import io.compprov.core.meta.Descriptor;
import io.compprov.core.variable.AbstractWrappedVariable;
import io.compprov.core.variable.VariableTrack;
import io.compprov.core.variable.WrappedVariable;

import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;

import static io.compprov.core.meta.Meta.formula;

public class WrappedSubgraph extends AbstractWrappedVariable<Subgraph> {

    private static final Descriptor OP_EXECUTE = Descriptor.descriptor("execute", formula("execute(subgraph,a0,...,an)"));

    private static final Map<Descriptor, Function<List<Object>, Object>> functionsMap;

    static {
        Map<Descriptor, Function<List<Object>, Object>> functions = new HashMap<>();
        functions.put(OP_EXECUTE, (arguments) -> {
            Subgraph subgraph = (Subgraph) arguments.get(0);
            if (subgraph.argumentIds().size() != arguments.size() - 1) {
                throw new IllegalArgumentException("Subgraph arguments size should fit specified arguments size");
            }
            synchronized (subgraph) {
                for (int i = 0; i < subgraph.argumentIds().size(); i++) {
                    String argumentId = subgraph.argumentIds().get(i);
                    subgraph.setArgument(argumentId, arguments.get(i + 1));
                }
                return subgraph.compute();
            }
        });

        functionsMap = Collections.unmodifiableMap(functions);
    }

    public WrappedSubgraph(ComputationContext context, VariableTrack variableTrack, Subgraph subgraph) {
        super(context, variableTrack, subgraph);
    }

    @Override
    public Function<List<Object>, Object> getFunction(Descriptor operationDescriptor) {
        return functionsMap.get(operationDescriptor);
    }

    public WrappedVariable execute(List<WrappedVariable> args, Descriptor resultDescriptor) {
        Objects.requireNonNull(args, "args");
        LinkedHashMap<String, WrappedVariable> arguments = new LinkedHashMap<>();
        arguments.put("subgraph", this);
        for (int i = 0; i < args.size(); i++) {
            arguments.put("a" + i, args.get(i));
        }
        return execute(
                OP_EXECUTE,
                arguments,
                resultDescriptor);
    }
}
