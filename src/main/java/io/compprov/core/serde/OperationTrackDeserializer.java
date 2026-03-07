package io.compprov.core.serde;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;
import io.compprov.core.meta.Descriptor;
import io.compprov.core.operation.OperationTrack;

import java.io.IOException;
import java.time.ZonedDateTime;

public class OperationTrackDeserializer extends StdDeserializer<OperationTrack> {

    public OperationTrackDeserializer() {
        super(OperationTrack.class);
    }

    @Override
    public OperationTrack deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
        ObjectMapper mapper = (ObjectMapper) p.getCodec();
        JsonNode node = mapper.readTree(p);

        int numericId = node.path("numericId").asInt();
        ZonedDateTime startedAt = ZonedDateTime.parse(node.path("startedAt").asText());
        ZonedDateTime finishedAt = ZonedDateTime.parse(node.path("finishedAt").asText());
        Descriptor descriptor = mapper.convertValue(node.path("descriptor"), Descriptor.class);
        String wrapperClass = node.path("wrapperClass").asText();

        return new OperationTrack(numericId, startedAt, finishedAt, descriptor, wrapperClass);
    }
}
