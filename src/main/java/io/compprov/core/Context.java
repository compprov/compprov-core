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
import java.time.ZoneId;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collectors;

/** Thread-safe computation context that accumulates the Calculation Provenance Graph (CPG). */
public class Context {

    /**
     * Point-in-time CPG snapshot. Variables are keyed by UUID (ordered by creation time);
     * operations are ordered by start time.
     */
    public record ContextRecord(Map<UUID, WrappedVariable> variables,
                                List<WrappedOperation> operations) {
    }

    protected final ConcurrentHashMap<Class<?>, VariableWrapper<?>> wrappers = new ConcurrentHashMap<>();
    protected final ConcurrentHashMap<VariableTrack, WrappedVariable> variables = new ConcurrentHashMap<>();
    protected final ConcurrentHashMap<OperationTrack, WrappedOperation> operations = new ConcurrentHashMap<>();

    protected final Clock clock;
    protected final ZoneId zoneId;
    protected final ObjectMapper mapper;

    public Context(Clock clock, ZoneId zoneId, ObjectMapper mapper) {
        this.clock = Objects.requireNonNull(clock, "clock");
        this.zoneId = Objects.requireNonNull(zoneId, "zoneId");
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

    @SuppressWarnings("unchecked")
    private <T> WrappedVariable wrap(T value, Descriptor descriptor, VariableKind variableKind) {
        Objects.requireNonNull(value, "value");
        Objects.requireNonNull(descriptor, "descriptor");

        VariableWrapper wrapper = wrappers.get(value.getClass());
        if (wrapper == null) {
            throw new NullPointerException("Wrapper for %s is not found".formatted(value.getClass()));
        }

        final var track = new VariableTrack(
                UUID.randomUUID(),
                clock.instant().atZone(zoneId),
                variableKind,
                descriptor,
                value.getClass());
        final var wrapped = wrapper.wrap(this, track, value);
        variables.put(wrapped.getVariableTrack(), wrapped);
        return wrapped;
    }

    // Run the computation without any lock — this is where parallelism happens.
    public WrappedVariable executeOperation(Supplier<?> computation,
                                            List<WrappedVariable> input,
                                            Descriptor opDescriptor,
                                            Descriptor resultDescriptor) {
        Objects.requireNonNull(computation, "computation");
        Objects.requireNonNull(opDescriptor, "opDescriptor");
        Objects.requireNonNull(resultDescriptor, "resultDescriptor");

        final var started = clock.instant().atZone(zoneId);
        final var result = computation.get();
        final var finished = clock.instant().atZone(zoneId);

        Objects.requireNonNull(result, "computation must not return null");
        final var wrappedResult = wrap(result, resultDescriptor, VariableKind.PRODUCED);

        final var wrappedOperation = new WrappedOperation(
                new OperationTrack(
                        UUID.randomUUID(),
                        started,
                        finished,
                        opDescriptor,
                        wrappedResult.getClass()),
                input,
                wrappedResult);
        operations.put(wrappedOperation.getOperationTrack(), wrappedOperation);

        return wrappedResult;
    }

    public ContextRecord export() {
        Map<UUID, WrappedVariable> variablesMap = variables.values()
                .stream()
                .sorted(Comparator.comparing(v -> v.getVariableTrack().getCreatedAt()))
                .collect(Collectors.toMap(
                        v -> v.getVariableTrack().getId(),
                        Function.identity(),
                        (a, b) -> a,
                        LinkedHashMap::new));

        List<WrappedOperation> operationsList = operations.values()
                .stream()
                .sorted(Comparator.comparing(op -> op.getOperationTrack().getStartedAt()))
                .toList();

        return new ContextRecord(variablesMap, operationsList);
    }

    public String toJson() {
        try {
            return mapper.writerWithDefaultPrettyPrinter().writeValueAsString(export());
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("JSON serialization failed", e);
        }
    }
}
