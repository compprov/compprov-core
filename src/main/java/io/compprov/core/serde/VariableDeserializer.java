package io.compprov.core.serde;

import io.compprov.core.Snapshot;
import io.compprov.core.variable.VariableTrack;
import io.compprov.core.variable.VariableWrapper;
import tools.jackson.core.JacksonException;
import tools.jackson.core.JsonParser;
import tools.jackson.databind.DatabindException;
import tools.jackson.databind.DeserializationContext;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.deser.std.StdDeserializer;

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
    public Snapshot.Variable deserialize(JsonParser p, DeserializationContext ctxt) throws JacksonException {
        JsonNode node = ctxt.readTree(p);

        VariableTrack track = ctxt.readTreeAsValue(node.path("track"), VariableTrack.class);
        Class<?> valueClass = checkValueClassWrapper(p, track.getValueClass());
        Object value = ctxt.readTreeAsValue(node.path("value"), valueClass);

        return new Snapshot.Variable(track, value);
    }

    private Class<?> checkValueClassWrapper(JsonParser p, String className) {

        var valueClass = wrapperClasses.get(className);
        if (valueClass != null) {
            return valueClass;
        }

        for (var clazz : wrappers.keySet()) {
            if (clazz.getName().equals(className)) {
                wrapperClasses.put(className, clazz);
                return clazz;
            }
        }
        throw DatabindException.from(p, "Unknown value class: " + className);
    }
}
