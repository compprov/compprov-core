package io.compprov.core;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.module.SimpleModule;
import io.compprov.core.meta.Descriptor;
import io.compprov.core.variable.ValueWithDescriptor;
import io.compprov.core.variable.VariableTrack;
import io.compprov.core.variable.VariableWrapper;

import java.io.IOException;
import java.time.Clock;
import java.time.ZoneId;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Thread-safe.
 */
public class ComputationEnvironment {

    protected final Clock clock;
    protected final ZoneId zoneId;
    protected final ObjectMapper mapper;
    protected final boolean requireInputDescriptor;
    protected final boolean requireResultDescriptor;

    protected final ConcurrentHashMap<Class<?>, VariableWrapper<?>> wrappers = new ConcurrentHashMap<>();

    /**
     * @param clock                   the clock used for timestamps
     * @param zoneId                  the time zone for timestamps
     * @param mapper                  the Jackson mapper used for serialization
     * @param requireInputDescriptor  when {@code true}, null is not allowed for the descriptor in wrap methods
     * @param requireResultDescriptor when {@code true}, null is not allowed for the descriptor in executeOperation
     */
    public ComputationEnvironment(Clock clock, ZoneId zoneId, ObjectMapper mapper,
                                  boolean requireInputDescriptor, boolean requireResultDescriptor) {
        this.clock = Objects.requireNonNull(clock, "clock");
        this.zoneId = Objects.requireNonNull(zoneId, "zoneId");
        this.mapper = Objects.requireNonNull(mapper, "mapper");
        this.requireInputDescriptor = requireInputDescriptor;
        this.requireResultDescriptor = requireResultDescriptor;
    }

    public void registerWrapper(Class<?> type, VariableWrapper<?> wrapper) {
        Objects.requireNonNull(type, "type");
        Objects.requireNonNull(wrapper, "wrapper");
        wrappers.put(type, wrapper);
    }

    public <T> void registerWrapper(Class<T> type, VariableWrapper<T> wrapper, JsonDeserializer<T> deserializer) {
        Objects.requireNonNull(type, "type");
        Objects.requireNonNull(wrapper, "wrapper");
        Objects.requireNonNull(deserializer, "deserializer");
        wrappers.put(type, wrapper);
        SimpleModule module = new SimpleModule();
        module.addDeserializer(type, deserializer);
        mapper.registerModule(module);
    }

    public String toJson(Snapshot snapshot) {
        try {
            return mapper.writerWithDefaultPrettyPrinter().writeValueAsString(snapshot);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("JSON serialization failed", e);
        }
    }

    public Snapshot fromJson(String json) {
        try {
            return mapper.readValue(json, Snapshot.class);
        } catch (JsonProcessingException e) {
            throw new IllegalArgumentException("Unable to deserialize JSON", e);
        }
    }

    public Snapshot fromJson(byte[] json) {
        try {
            return mapper.readValue(json, Snapshot.class);
        } catch (IOException e) {
            throw new IllegalArgumentException("Unable to deserialize JSON", e);
        }
    }

    public Snapshot copyWith(Snapshot snapshot, Descriptor descriptor, Map<String, ValueWithDescriptor> updates) {
        List<Snapshot.Variable> variables = new ArrayList<>();
        snapshot.variables().forEach(variable -> {
            final var update = updates.get(variable.track().getId());
            if (update == null) {
                variables.add(variable);
            } else {
                variables.add(new Snapshot.Variable(
                        new VariableTrack(
                                variable.track().getNumericId(),
                                clock.instant().atZone(zoneId),
                                variable.track().getKind(),
                                update.descriptor(),
                                update.value().getClass().getName()),
                        update.value()));
            }
        });
        return new Snapshot(descriptor, variables, snapshot.operations());
    }

    public String toHumanReadableLog(Snapshot snapshot) {
        Map<String, Snapshot.Variable> variableMap = new HashMap<>();
        snapshot.variables().forEach(v -> variableMap.put(v.track().getId(), v));
        StringBuilder stringBuilder = new StringBuilder();
        snapshot.operations()
                .forEach(operation -> {
                    stringBuilder.append(operation.track().getStartedAt().toString());
                    stringBuilder.append(": ");
                    stringBuilder.append(operation.track().getDescriptor().getName());
                    stringBuilder.append("->");
                    stringBuilder.append(operation.track().getDescriptor().getMeta().first());

                    //mapping through the variable id
                    stringBuilder.append("\r\n\t");
                    stringBuilder.append(writeValueSingleString(
                            operation.arguments().entrySet()
                                    .stream()
                                    .map(e -> "%s=%s".formatted(
                                            e.getKey(),
                                            e.getValue()))
                                    .toList()
                    ));
                    stringBuilder.append("->");
                    stringBuilder.append(operation.resultId());

                    //mapping through the variable name
                    stringBuilder.append("\r\n\t");
                    stringBuilder.append(writeValueSingleString(
                            operation.arguments().entrySet()
                                    .stream()
                                    .map(e -> "%s=%s".formatted(
                                            e.getKey(),
                                            variableMap.get(e.getValue()).track().getDescriptor().getName()))
                                    .toList()
                    ));
                    stringBuilder.append("->");
                    stringBuilder.append(variableMap.get(operation.resultId()).track().getDescriptor().getName());

                    //mapping through the variable value
                    stringBuilder.append("\r\n\t");
                    stringBuilder.append(writeValueSingleString(
                            operation.arguments().entrySet()
                                    .stream()
                                    .map(e -> "%s=%s".formatted(
                                            e.getKey(),
                                            variableMap.get(e.getValue()).value()))
                                    .toList()
                    ));
                    stringBuilder.append("->");
                    stringBuilder.append(variableMap.get(operation.resultId()).value());
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

    public ObjectMapper getMapper() {
        return mapper;
    }

    public ComputationContext compute(Snapshot snapshot) {
        Objects.requireNonNull(this, "environment");
        Objects.requireNonNull(snapshot, "snapshot");

        return new ComputationContext(this, new DataContext(snapshot.descriptor()), snapshot);
    }
}
