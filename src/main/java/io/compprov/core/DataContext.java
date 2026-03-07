package io.compprov.core;

import io.compprov.core.meta.Descriptor;
import io.compprov.core.operation.WrappedOperation;
import io.compprov.core.variable.WrappedVariable;

import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

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
                .map(it -> new Snapshot.Operation(it.getOperationTrack(), it.getArguments(), it.getResultId()))
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
