package io.compprov.core.serde;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;
import io.compprov.core.Snapshot;
import io.compprov.core.variable.VariableTrack;

import java.io.IOException;

public class VariableDeserializer extends StdDeserializer<Snapshot.Variable> {

    public VariableDeserializer() {
        super(Snapshot.Variable.class);
    }

    @Override
    public Snapshot.Variable deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
        ObjectMapper mapper = (ObjectMapper) p.getCodec();
        JsonNode node = mapper.readTree(p);

        VariableTrack track = mapper.convertValue(node.path("track"), VariableTrack.class);
        Object value;
        try {
            value = mapper.convertValue(node.path("value"), Class.forName(track.getValueClass()));
        } catch (ClassNotFoundException e) {
            throw new IllegalArgumentException("Unknown value class: " + track.getValueClass(), e);
        }

        return new Snapshot.Variable(track, value);
    }
}
