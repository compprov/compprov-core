package io.compprov.core.serde;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;
import io.compprov.core.Snapshot;
import io.compprov.core.variable.VariableTrack;
import io.compprov.core.variable.VariableWrapper;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class VariableDeserializer extends StdDeserializer<Snapshot.Variable> {

    private final Map<Class<?>, VariableWrapper<?>> wrappers;
    private final Map<String, Class<?>> wrapperClasses;

    public VariableDeserializer(Map<Class<?>, VariableWrapper<?>> wrappers) {
        super(Snapshot.Variable.class);
        this.wrappers = wrappers;
        wrapperClasses = new ConcurrentHashMap<>();
    }

    @Override
    public Snapshot.Variable deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
        ObjectMapper mapper = (ObjectMapper) p.getCodec();
        JsonNode node = mapper.readTree(p);

        VariableTrack track = mapper.convertValue(node.path("track"), VariableTrack.class);
        Class<?> valueClass = checkValueClassWrapper(track.getValueClass());
        Object value = mapper.convertValue(node.path("value"), valueClass);

        return new Snapshot.Variable(track, value);
    }

    private Class<?> checkValueClassWrapper(String className) {

        var valueClass = wrapperClasses.get(className);
        if (valueClass != null) {
            return valueClass;
        }

        valueClass = wrappers.keySet().stream()
                .filter(c -> c.getName().equals(className))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Unknown value class: " + className));
        wrapperClasses.put(className, valueClass);

        return valueClass;
    }
}
