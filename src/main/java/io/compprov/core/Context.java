package io.compprov.core;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.compprov.core.operation.OperationTrack;
import io.compprov.core.operation.WrappedOperation;
import io.compprov.core.variable.VariableKind;
import io.compprov.core.variable.VariableTrack;
import io.compprov.core.variable.VariableWrapper;
import io.compprov.core.variable.WrappedVariable;

import java.time.Clock;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;

/**
 * Thread safe
 */
public class Context {

    public record ContextRecord(List<WrappedVariable> variables, List<WrappedOperation> operations) {
    }

    protected final ConcurrentHashMap<Class<?>, VariableWrapper<?>> wrappers = new ConcurrentHashMap<>();
    protected final ConcurrentHashMap<VariableTrack, WrappedVariable> variables = new ConcurrentHashMap<>();
    protected final ConcurrentHashMap<OperationTrack, WrappedOperation> operations = new ConcurrentHashMap<>();

    private final Clock clock;
    private final ObjectMapper mapper;

    public Context(Clock clock, ObjectMapper mapper) {
        this.clock = Objects.requireNonNull(clock, "clock");
        this.mapper = Objects.requireNonNull(mapper, "mapper");
    }

    public void registerWrapper(Class<?> type, VariableWrapper<?> wrapper) {
        Objects.requireNonNull(type, "type");
        Objects.requireNonNull(wrapper, "wrapper");
        wrappers.put(type, wrapper);
    }

    public <T> WrappedVariable wrap(T value, Descriptor descriptor) {
        return wrap(value, descriptor, VariableKind.INPUT);
    }

    private <T> WrappedVariable wrap(T value, Descriptor descriptor, VariableKind variableKind) {
        Objects.requireNonNull(value, "value");
        Objects.requireNonNull(descriptor, "descriptor");

        VariableWrapper wrapper = wrappers.get(value.getClass());
        if (wrapper == null) {
            throw new NullPointerException("Wrapper for %s is not found".formatted(value.getClass()));
        }

        final var wrapped = wrapper.wrap(
                this,
                new VariableTrack(UUID.randomUUID(), clock.instant(), variableKind, descriptor),
                value);
        variables.put(wrapped.getVariableTrack(), wrapped);
        return wrapped;
    }

    public WrappedVariable executeOperation(Supplier<?> computation,
                                            List<WrappedVariable> input,
                                            Descriptor opDescriptor,
                                            Descriptor resultDescriptor) {
        Objects.requireNonNull(computation, "computation");
        Objects.requireNonNull(opDescriptor, "opDescriptor");
        Objects.requireNonNull(resultDescriptor, "resultDescriptor");

        // Run the computation without any lock — this is where parallelism happens.
        final var started = clock.instant();
        final var result = computation.get();
        final var finished = clock.instant();

        Objects.requireNonNull(result, "computation must not return null");
        final var wrappedResult = wrap(result, resultDescriptor, VariableKind.PRODUCED);

        final var wrappedOperation = new WrappedOperation(
                new OperationTrack(UUID.randomUUID(), started, finished, opDescriptor),
                input,
                wrappedResult);
        operations.put(wrappedOperation.getOperationTrack(), wrappedOperation);

        return wrappedResult;
    }

    public ContextRecord export() {
        return new ContextRecord(
                variables.values()
                        .stream()
                        .sorted(Comparator.comparing(variable -> variable.getVariableTrack().getCreatedAt()))
                        .toList(),
                operations.values()
                        .stream()
                        .sorted(Comparator.comparing(operation -> operation.getOperationTrack().getStartedAt()))
                        .toList());
    }

    public String toJson() {
        try {
            return mapper.writerWithDefaultPrettyPrinter().writeValueAsString(export());
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("JSON serialization failed", e);
        }
    }
}
