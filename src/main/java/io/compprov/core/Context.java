package io.compprov.core;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.compprov.core.meta.Meta;
import io.compprov.core.meta.MetaFormula;
import io.compprov.core.operation.OperationTrack;
import io.compprov.core.operation.WrappedOperation;
import io.compprov.core.variable.VariableKind;
import io.compprov.core.variable.VariableTrack;
import io.compprov.core.variable.VariableWrapper;
import io.compprov.core.variable.WrappedVariable;

import java.time.Clock;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collectors;

/**
 * Thread-safe computation context that accumulates the Calculation Provenance Graph (CPG).
 */
public class Context {

    /**
     * Point-in-time CPG snapshot.
     */
    public record ContextRecord(Descriptor contextDescriptor,
                                Map<String, WrappedVariable> variables,
                                List<WrappedOperation> operations) {
    }

    protected final ConcurrentHashMap<Class<?>, VariableWrapper<?>> wrappers = new ConcurrentHashMap<>();
    protected final ConcurrentHashMap<String, WrappedVariable> variables = new ConcurrentHashMap<>();
    protected final ConcurrentHashMap<String, WrappedOperation> operations = new ConcurrentHashMap<>();

    protected final Clock clock;
    protected final ZoneId zoneId;
    protected final ObjectMapper mapper;
    protected final AtomicInteger variableCounter = new AtomicInteger(0);
    protected final AtomicInteger operationCounter = new AtomicInteger(0);
    protected final boolean requireInputDescriptor;
    protected final boolean requireResultDescriptor;
    protected final Descriptor contextDescriptor;

    /**
     * @param clock
     * @param zoneId
     * @param mapper
     * @param requireInputDescriptor   - When true null is not allowed for descriptor in wrap function
     * @param requireResultDescriptor- When true null is not allowed for descriptor in executeOperation function
     */
    public Context(Clock clock, ZoneId zoneId, ObjectMapper mapper,
                   boolean requireInputDescriptor, boolean requireResultDescriptor,
                   Descriptor contextDescriptor) {
        this.clock = Objects.requireNonNull(clock, "clock");
        this.zoneId = Objects.requireNonNull(zoneId, "zoneId");
        this.mapper = Objects.requireNonNull(mapper, "mapper");
        this.requireInputDescriptor = requireInputDescriptor;
        this.requireResultDescriptor = requireResultDescriptor;
        this.contextDescriptor = contextDescriptor;
    }

    public void registerWrapper(Class<?> type, VariableWrapper<?> wrapper) {
        Objects.requireNonNull(type, "type");
        Objects.requireNonNull(wrapper, "wrapper");
        wrappers.put(type, wrapper);
    }

    public <T> T getVariable(String id, T clazz) {
        return (T) variables.get(id);
    }

    public WrappedOperation getOperation(String id) {
        return operations.get(id);
    }

    public <T> WrappedVariable wrap(T value, Descriptor descriptor) {
        if (requireInputDescriptor) {
            Objects.requireNonNull(descriptor, "descriptor");
        }
        return wrap(value, descriptor, VariableKind.INPUT);
    }

    @SuppressWarnings("unchecked")
    private <T> WrappedVariable wrap(T value, Descriptor descriptor, VariableKind variableKind) {
        Objects.requireNonNull(value, "value");

        VariableWrapper wrapper = wrappers.get(value.getClass());
        if (wrapper == null) {
            throw new NullPointerException("Wrapper for %s is not found".formatted(value.getClass()));
        }

        int numericId = variableCounter.incrementAndGet();
        final var track = new VariableTrack(
                numericId,
                clock.instant().atZone(zoneId),
                variableKind,
                descriptor,
                value.getClass());
        final var wrapped = wrapper.wrap(this, track, value);
        variables.put(wrapped.getVariableTrack().getId(), wrapped);
        return wrapped;
    }

    // Run the computation without any lock — this is where parallelism happens.
    public WrappedVariable executeOperation(Supplier<?> computation,
                                            List<WrappedVariable> input,
                                            Descriptor opDescriptor,
                                            Descriptor resultDescriptor) {
        Objects.requireNonNull(computation, "computation");
        if (requireResultDescriptor) {
            Objects.requireNonNull(opDescriptor, "opDescriptor");
        }

        final var started = clock.instant().atZone(zoneId);
        final var result = computation.get();
        final var finished = clock.instant().atZone(zoneId);

        Objects.requireNonNull(result, "computation must not return null");
        final var wrappedResult = wrap(result, resultDescriptor, VariableKind.OUTPUT);

        final var wrappedOperation = new WrappedOperation(
                new OperationTrack(
                        operationCounter.incrementAndGet(),
                        started,
                        finished,
                        opDescriptor,
                        wrappedResult.getClass()),
                input.stream().map(inputVar -> inputVar.getVariableTrack().getId()).toList(),
                wrappedResult.getVariableTrack().getId());
        operations.put(wrappedOperation.getOperationTrack().getId(), wrappedOperation);

        return wrappedResult;
    }

    public ContextRecord export() {
        Map<String, WrappedVariable> variablesMap = variables.values()
                .stream()
                .sorted(Comparator.comparing(v -> v.getVariableTrack().getNumericId()))
                .collect(Collectors.toMap(
                        v -> v.getVariableTrack().getId(),
                        Function.identity(),
                        (a, b) -> a,
                        LinkedHashMap::new));

        List<WrappedOperation> operationsList = operations.values()
                .stream()
                .sorted(Comparator.comparing(op -> op.getOperationTrack().getNumericId()))
                .toList();

        return new ContextRecord(contextDescriptor, variablesMap, operationsList);
    }

    public String toJson() {
        try {
            return mapper.writerWithDefaultPrettyPrinter().writeValueAsString(export());
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("JSON serialization failed", e);
        }
    }

    /**
     * Populates this context from a JSON string previously produced by {@link #toJson()}.
     *
     * <p>Should be called on a fresh, empty context. Deserialized variables and operations
     * are merged into any existing state, which may cause unexpected behaviour if numeric
     * IDs overlap.</p>
     *
     * <p>Each variable value is deserialized via {@link ObjectMapper#treeToValue} with the
     * {@code valueClass} recorded in the track, so the type must be Jackson-deserializable.
     * A {@link VariableWrapper} for that type must also be registered in this context.</p>
     *
     * @param json JSON produced by {@link #toJson()}
     * @return the populated {@link ContextRecord}
     * @throws IllegalArgumentException if the JSON is malformed or references an unknown class
     */
    @SuppressWarnings("unchecked")
    public ContextRecord fromJson(String json) {
        try {
            JsonNode root = mapper.readTree(json);

            JsonNode variablesNode = root.get("variables");
            if (variablesNode != null) {
                Iterator<Map.Entry<String, JsonNode>> fields = variablesNode.fields();
                while (fields.hasNext()) {
                    Map.Entry<String, JsonNode> entry = fields.next();
                    JsonNode varNode = entry.getValue();

                    VariableTrack track = parseVariableTrack(varNode.get("track"));
                    Class<?> valueClass = track.getValueClass();
                    Object value = mapper.treeToValue(varNode.get("value"), valueClass);

                    VariableWrapper wrapper = wrappers.get(valueClass);
                    if (wrapper == null) {
                        throw new IllegalStateException("No wrapper registered for " + valueClass.getName());
                    }
                    WrappedVariable wrapped = wrapper.wrap(this, track, value);
                    variables.put(track.getId(), wrapped);
                    variableCounter.accumulateAndGet(track.getNumericId(), Math::max);
                }
            }

            JsonNode operationsNode = root.get("operations");
            if (operationsNode != null) {
                for (JsonNode opNode : operationsNode) {
                    OperationTrack track = parseOperationTrack(opNode.get("operationTrack"));
                    List<String> inputIds = new ArrayList<>();
                    opNode.get("inputIds").forEach(n -> inputIds.add(n.asText()));
                    String resultId = opNode.get("resultId").asText();

                    WrappedOperation op = new WrappedOperation(track, inputIds, resultId);
                    operations.put(track.getId(), op);
                    operationCounter.accumulateAndGet(track.getNumericId(), Math::max);
                }
            }

            return export();
        } catch (JsonProcessingException e) {
            throw new IllegalArgumentException("JSON deserialization failed", e);
        }
    }

    protected VariableTrack parseVariableTrack(JsonNode node) {
        int numericId = node.get("numericId").asInt();
        ZonedDateTime createdAt = ZonedDateTime.parse(
                node.get("createdAt").asText(), DateTimeFormatter.ISO_OFFSET_DATE_TIME);
        VariableKind kind = VariableKind.valueOf(node.get("kind").asText());
        Descriptor descriptor = parseDescriptor(node.get("descriptor"));
        String valueClassName = node.get("valueClass").asText();
        try {
            return new VariableTrack(numericId, createdAt, kind, descriptor, Class.forName(valueClassName));
        } catch (ClassNotFoundException e) {
            throw new IllegalArgumentException("Unknown valueClass: " + valueClassName, e);
        }
    }

    protected OperationTrack parseOperationTrack(JsonNode node) {
        int numericId = node.get("numericId").asInt();
        ZonedDateTime startedAt = ZonedDateTime.parse(
                node.get("startedAt").asText(), DateTimeFormatter.ISO_OFFSET_DATE_TIME);
        ZonedDateTime finishedAt = ZonedDateTime.parse(
                node.get("finishedAt").asText(), DateTimeFormatter.ISO_OFFSET_DATE_TIME);
        Descriptor descriptor = parseDescriptor(node.get("descriptor"));
        String wrapperClassName = node.get("wrapperClass").asText();
        try {
            return new OperationTrack(numericId, startedAt, finishedAt, descriptor, Class.forName(wrapperClassName));
        } catch (ClassNotFoundException e) {
            throw new IllegalArgumentException("Unknown wrapperClass: " + wrapperClassName, e);
        }
    }

    protected Descriptor parseDescriptor(JsonNode node) {
        String name = node.get("name").asText();
        return new Descriptor(name, parseMeta(node.get("description")), parseMeta(node.get("origin")));
    }

    protected Meta parseMeta(JsonNode node) {
        if (node == null || node.isEmpty()) return Meta.NO_META;
        if (node.has("formula")) return MetaFormula.formula(node.get("formula").asText());
        return Meta.NO_META;
    }

    public String toHumanReadableLog() {
        StringBuilder stringBuilder = new StringBuilder();
        operations.values()
                .stream()
                .sorted(Comparator.comparing(op -> op.getOperationTrack().getNumericId()))
                .forEach(operation -> {
                    stringBuilder.append(operation.getOperationTrack().getStartedAt().toString());
                    stringBuilder.append(": ");
                    stringBuilder.append(operation.getOperationTrack().getDescriptor().getName());
                    stringBuilder.append("\t");
                    stringBuilder.append(operation.getOperationTrack().getDescriptor().getDescription());
                    stringBuilder.append("\t");
                    stringBuilder.append(writeValueSingleString(
                            operation.getInputIds()
                                    .stream()
                                    .map(inputId -> variables.get(inputId).getDescriptor().getName())
                                    .toList()
                    ));
                    stringBuilder.append("->");
                    stringBuilder.append(variables.get(operation.getResultId()).getDescriptor().getName());
                    stringBuilder.append("\r\n\t");
                    stringBuilder.append(writeValueSingleString(
                            operation.getInputIds()
                                    .stream()
                                    .map(inputId -> variables.get(inputId).getValue())
                                    .toList()
                    ));
                    stringBuilder.append("->");
                    stringBuilder.append(variables.get(operation.getResultId()).getValue());
                    stringBuilder.append("\r\n");
                });
        return stringBuilder.toString();
    }

    private String writeValueSingleString(Object o) {
        try {
            return mapper.writeValueAsString(o);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("JSON serialization failed", e);
        }
    }
}
