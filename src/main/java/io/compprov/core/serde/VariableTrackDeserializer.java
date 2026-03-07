package io.compprov.core.serde;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;
import io.compprov.core.meta.Descriptor;
import io.compprov.core.variable.VariableKind;
import io.compprov.core.variable.VariableTrack;

import java.io.IOException;
import java.time.ZonedDateTime;

public class VariableTrackDeserializer extends StdDeserializer<VariableTrack> {

    public VariableTrackDeserializer() {
        super(VariableTrack.class);
    }

    @Override
    public VariableTrack deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
        ObjectMapper mapper = (ObjectMapper) p.getCodec();
        JsonNode node = mapper.readTree(p);

        int numericId = node.path("numericId").asInt();
        ZonedDateTime createdAt = ZonedDateTime.parse(node.path("createdAt").asText());
        VariableKind kind = mapper.convertValue(node.path("kind"), VariableKind.class);
        Descriptor descriptor = mapper.convertValue(node.path("descriptor"), Descriptor.class);
        String valueClass = node.path("valueClass").asText();
        return new VariableTrack(numericId, createdAt, kind, descriptor, valueClass);
    }
}
