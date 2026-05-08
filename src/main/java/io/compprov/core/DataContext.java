package io.compprov.core;

import io.compprov.core.meta.Descriptor;
import io.compprov.core.meta.Pair;
import io.compprov.core.operation.WrappedOperation;
import io.compprov.core.variable.WrappedVariable;

import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Holds the mutable state of a computation: the variable and operation stores,
 * and their numeric counters.
 *
 * <p>The maps and counters are intentionally {@code protected} so that
 * {@link ComputationContext} and its subclasses can read and write them directly
 * for performance. Any class that extends {@code DataContext} or
 * {@code ComputationContext} takes on responsibility for maintaining consistency
 * between the two stores.</p>
 */
public class DataContext {
    protected final AtomicInteger variableCounter = new AtomicInteger(0);
    protected final AtomicInteger operationCounter = new AtomicInteger(0);
    protected final ConcurrentHashMap<String, WrappedVariable> variables = new ConcurrentHashMap<>();
    protected final ConcurrentHashMap<String, WrappedOperation> operations = new ConcurrentHashMap<>();
    protected final Descriptor contextDescriptor;

    public DataContext(Descriptor contextDescriptor) {
        this.contextDescriptor = Objects.requireNonNull(contextDescriptor);
    }

    public Snapshot snapshot() {
        List<Snapshot.Variable> variablesList = variables.values()
                .stream()
                .sorted(Comparator.comparing(it -> it.getVariableTrack().getNumericId()))
                .map(it -> new Snapshot.Variable(it.getVariableTrack(), it.getValue()))
                .toList();

        List<Snapshot.Operation> operationsList = operations.values()
                .stream()
                .sorted(Comparator.comparing(it -> it.getOperationTrack().getNumericId()))
                .map(it -> new Snapshot.Operation(it.getOperationTrack(),
                        it.getArguments().entrySet()
                                .stream()
                                .map(entry -> new Pair(entry.getKey(), entry.getValue()))
                                .toList(),
                        it.getResultId()))
                .toList();

        return new Snapshot(contextDescriptor, variablesList, operationsList);
    }

    protected int nextOperation() {
        return operationCounter.incrementAndGet();
    }

    protected int nextVariable() {
        return variableCounter.incrementAndGet();
    }
}
