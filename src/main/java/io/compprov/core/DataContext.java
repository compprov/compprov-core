package io.compprov.core;

import io.compprov.core.meta.Descriptor;
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
 * <p>{@link ComputationContext} and its subclasses can read and write fields directly
 * for performance ans simplicity. Any class that extends {@code DataContext} or
 * {@code ComputationContext} takes on responsibility for maintaining consistency
 * between the two stores.</p>
 */
public class DataContext {
    public final AtomicInteger variableCounter = new AtomicInteger(0);
    public final AtomicInteger operationCounter = new AtomicInteger(0);
    public final ConcurrentHashMap<String, WrappedVariable> variables = new ConcurrentHashMap<>();
    public final ConcurrentHashMap<String, WrappedOperation> operations = new ConcurrentHashMap<>();
    public final ConcurrentHashMap<String, WrappedOperation> producedBy = new ConcurrentHashMap<>();
    public final Descriptor contextDescriptor;

    public DataContext(Descriptor contextDescriptor) {
        this.contextDescriptor = Objects.requireNonNull(contextDescriptor);
    }

    public Snapshot snapshot() {
        List<Snapshot.Variable> variablesList = variables.values()
                .stream()
                .sorted(Comparator.comparing(it -> it.getVariableTrack().getNumericId()))
                .map(WrappedVariable::snapshot)
                .toList();

        List<Snapshot.Operation> operationsList = operations.values()
                .stream()
                .sorted(Comparator.comparing(it -> it.getOperationTrack().getNumericId()))
                .map(WrappedOperation::snapshot)
                .toList();

        return new Snapshot(contextDescriptor, variablesList, operationsList);
    }

    public int nextOperation() {
        return operationCounter.incrementAndGet();
    }

    public int nextVariable() {
        return variableCounter.incrementAndGet();
    }
}
