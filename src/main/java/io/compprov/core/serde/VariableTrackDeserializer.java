package io.compprov.core.serde;

import io.compprov.core.meta.Descriptor;
import io.compprov.core.variable.VariableKind;
import io.compprov.core.variable.VariableTrack;
import tools.jackson.core.JacksonException;
import tools.jackson.core.JsonParser;
import tools.jackson.databind.DeserializationContext;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.deser.std.StdDeserializer;

import java.time.ZonedDateTime;

public class VariableTrackDeserializer extends StdDeserializer<VariableTrack> {

    public VariableTrackDeserializer() {
        super(VariableTrack.class);
    }

    @Override
    public VariableTrack deserialize(JsonParser p, DeserializationContext ctxt) throws JacksonException {
        JsonNode node = ctxt.readTree(p);

        int numericId = node.path("numericId").asInt();
        ZonedDateTime createdAt = ZonedDateTime.parse(node.path("createdAt").asString());
        VariableKind kind = ctxt.readTreeAsValue(node.path("kind"), VariableKind.class);
        Descriptor descriptor = ctxt.readTreeAsValue(node.path("descriptor"), Descriptor.class);
        String valueClass = node.path("valueClass").asString();
        return new VariableTrack(numericId, createdAt, kind, descriptor, valueClass);
    }
}
