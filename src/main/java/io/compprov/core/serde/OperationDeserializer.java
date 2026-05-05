package io.compprov.core.serde;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;
import io.compprov.core.Snapshot;
import io.compprov.core.meta.Descriptor;
import io.compprov.core.meta.Pair;
import io.compprov.core.operation.OperationTrack;

import java.io.IOException;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;

public class OperationDeserializer extends StdDeserializer<Snapshot.Operation> {

    public OperationDeserializer() {
        super(Snapshot.Variable.class);
    }

    @Override
    public Snapshot.Operation deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
        ObjectMapper mapper = (ObjectMapper) p.getCodec();
        JsonNode node = mapper.readTree(p);

        final var trackNode = node.get("track");
        int numericId = trackNode.path("numericId").asInt();
        ZonedDateTime startedAt = ZonedDateTime.parse(trackNode.path("startedAt").asText());
        ZonedDateTime finishedAt = ZonedDateTime.parse(trackNode.path("finishedAt").asText());
        Descriptor descriptor = mapper.convertValue(trackNode.path("descriptor"), Descriptor.class);
        String wrapperClass = trackNode.path("wrapperClass").asText();
        final var operationTrack = new OperationTrack(numericId, startedAt, finishedAt, descriptor, wrapperClass);

        final var argumentsNode = node.get("arguments");
        List<Pair> argumentsList;
        if (argumentsNode.isArray()) {
            //to preserve order in JSON
            argumentsList = mapper.convertValue(
                    argumentsNode,
                    new TypeReference<ArrayList<Pair>>() {
                    }
            );
        } else {
            //backward compatibility
            LinkedHashMap<String, String> argumentsMap = mapper.convertValue(
                    argumentsNode,
                    new TypeReference<LinkedHashMap<String, String>>() {
                    }
            );
            argumentsList = argumentsMap.entrySet()
                    .stream()
                    .map(entry -> new Pair(entry.getKey(), entry.getValue()))
                    .toList();
        }

        final var resultId = node.get("resultId").asText();
        return new Snapshot.Operation(operationTrack, argumentsList, resultId);
    }
}
