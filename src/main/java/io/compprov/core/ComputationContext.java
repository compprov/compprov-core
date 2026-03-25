package io.compprov.core;

import io.compprov.core.meta.Descriptor;
import io.compprov.core.operation.OperationTrack;
import io.compprov.core.operation.WrappedOperation;
import io.compprov.core.variable.VariableKind;
import io.compprov.core.variable.VariableTrack;
import io.compprov.core.variable.VariableWrapper;
import io.compprov.core.variable.WrappedVariable;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Objects;
import java.util.function.Predicate;

import static io.compprov.core.operation.WrappedOperation.operation;

/**
 * <p>Thread-safe class, except for the {@link #snapshot()} operation.</p>
 * <p>Feel free to override and expand this class as you need.</p>
 */
public class ComputationContext {
    protected final ComputationEnvironment environment;
    protected final DataContext data;

    public ComputationContext(ComputationEnvironment environment, DataContext data) {
        Objects.requireNonNull(environment, "environment");
        Objects.requireNonNull(data, "data");
        this.environment = environment;
        this.data = data;
    }

    public ComputationContext(ComputationEnvironment environment, DataContext data, Snapshot snapshot) {
        this(environment, data);
        snapshot.variables().forEach(this::wrapSnapshotVariable);
        snapshot.operations().forEach(this::executeSnapshotOperation);
    }

    /**
     * Produces a point-in-time snapshot of the CPG. Do not modify this context while this method is running.
     *
     * @return a consistent snapshot of all variables and operations recorded so far
     */
    public Snapshot snapshot() {
        return data.snapshot();
    }

    /**
     * Thread-safe. Wraps the given value as an input variable and registers it in this context.
     */
    public <T> WrappedVariable wrap(T value, Descriptor descriptor) {
        return wrap(value, descriptor, VariableKind.INPUT);
    }

    /**
     * Thread-safe. Executes the operation identified by {@code opDescriptor} on the given named arguments,
     * records the operation in the CPG, and returns the wrapped result.
     *
     * @param arguments        ordered map of argument name to wrapped variable; must not be empty
     * @param opDescriptor     descriptor of the operation being performed
     * @param resultDescriptor descriptor for the result variable, or {@code null} to auto-generate one
     * @return the wrapped result variable
     */
    public WrappedVariable executeOperation(LinkedHashMap<String, WrappedVariable> arguments,
                                            Descriptor opDescriptor,
                                            Descriptor resultDescriptor) {
        Objects.requireNonNull(arguments, "arguments");
        Objects.requireNonNull(opDescriptor, "opDescriptor");

        final var started = environment.clock.instant().atZone(environment.zoneId);
        final var caller = arguments.values().iterator().next();
        final var functionToCall = caller.getFunction(opDescriptor);
        final var result = functionToCall.apply(arguments.values().stream().map(WrappedVariable::getValue).toList());
        final var finished = environment.clock.instant().atZone(environment.zoneId);

        Objects.requireNonNull(result, "computation must not return null");
        final var wrappedResult = wrap(result, resultDescriptor, VariableKind.OUTPUT);

        final var mappingArguments = new LinkedHashMap<String, String>();
        arguments.forEach((k, v) -> mappingArguments.put(k, v.getVariableTrack().getId()));

        final var wrappedOperation = operation(
                new OperationTrack(
                        data.nextOperation(),
                        started,
                        finished,
                        opDescriptor,
                        wrappedResult.getClass().getName()),
                mappingArguments,
                wrappedResult.getVariableTrack().getId());
        data.operations.put(wrappedOperation.getOperationTrack().getId(), wrappedOperation);

        return wrappedResult;
    }

    private <T> WrappedVariable wrap(T value, Descriptor descriptor, VariableKind variableKind) {
        Objects.requireNonNull(value, "value");

        if (descriptor == null) {
            boolean descriptorRequired = switch (variableKind) {
                case INPUT -> environment.requireInputDescriptor;
                case OUTPUT -> environment.requireResultDescriptor;
            };
            if (descriptorRequired) {
                throw new IllegalArgumentException("Descriptor required");
            }
        }

        VariableWrapper wrapper = environment.wrappers.get(value.getClass());
        if (wrapper == null) {
            throw new IllegalStateException("Wrapper for %s is not found".formatted(value.getClass()));
        }

        int numericId = data.nextVariable();
        final var track = new VariableTrack(
                numericId,
                environment.clock.instant().atZone(environment.zoneId),
                variableKind,
                descriptor,
                value.getClass().getName());
        final var wrapped = wrapper.wrap(this, track, value);
        data.variables.put(wrapped.getVariableTrack().getId(), wrapped);
        return wrapped;
    }

    private <T> WrappedVariable wrapSnapshotVariable(Snapshot.Variable variable) {
        Objects.requireNonNull(variable, "variable");

        VariableWrapper wrapper = environment.wrappers.get(variable.value().getClass());
        if (wrapper == null) {
            throw new NullPointerException("Wrapper for %s is not found".formatted(variable.value().getClass()));
        }

        final var createdAt = variable.track().getKind() == VariableKind.INPUT
                ? variable.track().getCreatedAt() : environment.clock.instant().atZone(environment.zoneId);
        final var track = new VariableTrack(
                variable.track().getNumericId(),
                createdAt,
                variable.track().getKind(),
                variable.track().getDescriptor(),
                variable.value().getClass().getName());
        final var wrapped = wrapper.wrap(this, track, variable.value());
        data.variables.put(wrapped.getVariableTrack().getId(), wrapped);
        data.variableCounter.set(variable.track().getNumericId());
        return wrapped;
    }

    private WrappedVariable executeSnapshotOperation(Snapshot.Operation operation) {
        Objects.requireNonNull(operation, "operation");

        final var arguments = operation.arguments().values().stream().map(id -> data.variables.get(id)).toList();

        final var started = environment.clock.instant().atZone(environment.zoneId);
        final var caller = arguments.get(0);
        final var functionToCall = caller.getFunction(operation.track().getDescriptor());
        final var newResultValue = functionToCall.apply(arguments.stream().map(WrappedVariable::getValue).toList());
        final var finished = environment.clock.instant().atZone(environment.zoneId);

        Objects.requireNonNull(newResultValue, "computation must not return null");
        final var oldResult = data.variables.get(operation.resultId());
        final var reWrappedResult = wrapSnapshotVariable(new Snapshot.Variable(oldResult.getVariableTrack(), newResultValue));

        final var wrappedOperation = operation(
                new OperationTrack(
                        operation.track().getNumericId(),
                        started,
                        finished,
                        operation.track().getDescriptor(),
                        newResultValue.getClass().getName()),
                operation.arguments(),
                reWrappedResult.getVariableTrack().getId());
        data.operations.put(wrappedOperation.getOperationTrack().getId(), wrappedOperation);
        data.operationCounter.set(wrappedOperation.getOperationTrack().getNumericId());

        return reWrappedResult;
    }

    public ComputationEnvironment getEnvironment() {
        return environment;
    }

    public WrappedVariable getVariable(String id) {
        return data.variables.get(id);
    }

    public List<WrappedVariable> findVariables(Predicate<WrappedVariable> predicate) {
        return data.variables.values().stream().filter(predicate).toList();
    }

    /**
     * @param predicate
     * @return null if not found
     * @throws IllegalStateException if more than one result is found
     */
    public WrappedVariable findSingleVariable(Predicate<WrappedVariable> predicate) {
        final var results = findVariables(predicate);
        if (results.isEmpty()) {
            return null;
        }
        if (results.size() > 1) {
            throw new IllegalStateException("Expected a single result, but found " + results.size());
        }
        return results.get(0);
    }

    public WrappedVariable findSingleVariable(String name) {
        return findSingleVariable(it -> it.getVariableTrack().getDescriptor().getName().equals(name));
    }

    public List<WrappedVariable> findVariables(String name) {
        return findVariables(it -> it.getVariableTrack().getDescriptor().getName().equals(name));
    }

    public WrappedOperation getOperation(String id) {
        return data.operations.get(id);
    }
}
