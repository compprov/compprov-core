package io.compprov.core.wrappers.subgraph;

import io.compprov.core.ComputationContext;
import io.compprov.core.Snapshot;
import io.compprov.core.Subgraph;
import io.compprov.core.meta.Descriptor;
import io.compprov.core.operation.WrappedOperation;
import io.compprov.core.variable.AbstractWrappedVariable;
import io.compprov.core.variable.VariableTrack;
import io.compprov.core.variable.WrappedVariable;

import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;

import static io.compprov.core.meta.Meta.formula;

/**
 * Provenance-tracked {@link Subgraph}. Each {@link #execute} / {@link #executeConcurrent} call
 * records exactly one {@code execute} operation in the owning context, regardless of how many
 * operations the underlying template contains.
 */
public class WrappedSubgraph extends AbstractWrappedVariable<Subgraph> {

    private static final Descriptor OP_EXECUTE = Descriptor.descriptor("execute", formula("execute(subgraph,a0,...,an)"));
    private static final Descriptor OP_EXECUTE_CONCURRENT = Descriptor.descriptor("execute_concurrent", formula("execute(subgraph,a0,...,an)"));

    private static final Map<Descriptor, Function<List<Object>, Object>> functionsMap;

    static {
        Map<Descriptor, Function<List<Object>, Object>> functions = new HashMap<>();
        functions.put(OP_EXECUTE, (arguments) -> {
            Subgraph subgraph = (Subgraph) arguments.get(0);
            if (subgraph.argumentIds().size() != arguments.size() - 1) {
                throw new IllegalArgumentException("Subgraph arguments size should fit specified arguments size");
            }
            final var state = subgraph.getDefaultState();
            synchronized (state) {
                for (int i = 0; i < subgraph.argumentIds().size(); i++) {
                    String argumentId = subgraph.argumentIds().get(i);
                    state.setArgument(argumentId, arguments.get(i + 1));
                }
                return subgraph.compute(state);
            }
        });

        functions.put(OP_EXECUTE_CONCURRENT, (arguments) -> {
            Subgraph subgraph = (Subgraph) arguments.get(0);
            if (subgraph.argumentIds().size() != arguments.size() - 1) {
                throw new IllegalArgumentException("Subgraph arguments size should fit specified arguments size");
            }
            final var state = subgraph.createState();
            for (int i = 0; i < subgraph.argumentIds().size(); i++) {
                String argumentId = subgraph.argumentIds().get(i);
                state.setArgument(argumentId, arguments.get(i + 1));
            }
            return subgraph.compute(state);
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

    /**
     * Works faster with lesser memory consumption, but uses blocking operations over the subgraph.
     * Best fit for single-threaded calculations.
     *
     * @param args             argument values, positionally matching {@code Subgraph.argumentIds()}
     * @param resultDescriptor descriptor for the result variable, or {@code null} to auto-generate one
     * @return the wrapped result variable
     */
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

    /**
     * Does not use blocking operations over the subgraph, but requires extra memory and has some overhead,
     * fits for concurrent multi-threaded calculations.
     *
     * @param args             argument values, positionally matching {@code Subgraph.argumentIds()}
     * @param resultDescriptor descriptor for the result variable, or {@code null} to auto-generate one
     * @return the wrapped result variable
     */
    public WrappedVariable executeConcurrent(List<WrappedVariable> args, Descriptor resultDescriptor) {
        Objects.requireNonNull(args, "args");
        LinkedHashMap<String, WrappedVariable> arguments = new LinkedHashMap<>();
        arguments.put("subgraph", this);
        for (int i = 0; i < args.size(); i++) {
            arguments.put("a" + i, args.get(i));
        }
        return execute(
                OP_EXECUTE_CONCURRENT,
                arguments,
                resultDescriptor);
    }

    /**
     * Exports the subgraph's template — its initial variables and recorded operations, as
     * captured when this {@link Subgraph} was built — as a standalone {@link Snapshot}. This
     * always reflects the template's original values, not the state of any particular
     * {@link #execute} / {@link #executeConcurrent} call.
     *
     * <p>To validate the intermediate steps of one specific invocation, substitute that call's
     * actual argument values into this snapshot with {@code environment.copyWith(...)}, then
     * replay it with {@code environment.compute(...)} — this reconstructs a full, independent
     * CPG for that single call, with every intermediate variable tracked, separate from the
     * folded outer context.</p>
     *
     * @return a snapshot of the subgraph template
     */
    public Snapshot extractSubgraph() {
            List<Snapshot.Variable> variablesList = getValue().variables()
                    .stream()
                    .sorted(Comparator.comparing(it -> it.getVariableTrack().getNumericId()))
                    .map(WrappedVariable::snapshot)
                    .toList();

            List<Snapshot.Operation> operationsList = getValue().operations()
                    .stream()
                    .sorted(Comparator.comparing(it -> it.getOperationTrack().getNumericId()))
                    .map(WrappedOperation::snapshot)
                    .toList();

            return new Snapshot(getVariableTrack().getDescriptor(), variablesList, operationsList);
    }
}
