package io.compprov.core.serde;

import io.compprov.core.Snapshot;
import io.compprov.core.meta.Descriptor;
import io.compprov.core.operation.OperationTrack;
import io.compprov.core.operation.WrappedArgumentId;
import tools.jackson.core.JacksonException;
import tools.jackson.core.JsonParser;
import tools.jackson.databind.DeserializationContext;
import tools.jackson.databind.JavaType;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.deser.std.StdDeserializer;

import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;

public class OperationDeserializer extends StdDeserializer<Snapshot.Operation> {

    public OperationDeserializer() {
        super(Snapshot.Operation.class);
    }

    @Override
    public Snapshot.Operation deserialize(JsonParser p, DeserializationContext ctxt) throws JacksonException {
        JsonNode node = ctxt.readTree(p);

        JavaType listOfWrappedArgumentIdType = ctxt.getTypeFactory().constructCollectionType(ArrayList.class, WrappedArgumentId.class);
        JavaType mapOfArgumentsType = ctxt.getTypeFactory().constructMapType(LinkedHashMap.class, String.class, String.class);

        final var trackNode = node.get("track");
        int numericId = trackNode.path("numericId").asInt();
        ZonedDateTime startedAt = ZonedDateTime.parse(trackNode.path("startedAt").asString());
        ZonedDateTime finishedAt = ZonedDateTime.parse(trackNode.path("finishedAt").asString());
        Descriptor descriptor = ctxt.readTreeAsValue(trackNode.path("descriptor"), Descriptor.class);
        String wrapperClass = trackNode.path("wrapperClass").asString();
        final var operationTrack = new OperationTrack(numericId, startedAt, finishedAt, descriptor, wrapperClass);

        final var argumentsNode = node.get("arguments");
        List<WrappedArgumentId> argumentsList;
        if (argumentsNode.isArray()) {
            //to preserve order in JSON
            argumentsList = ctxt.readTreeAsValue(argumentsNode, listOfWrappedArgumentIdType);
        } else {
            //backward compatibility
            LinkedHashMap<String, String> argumentsMap = ctxt.readTreeAsValue(argumentsNode, mapOfArgumentsType);
            argumentsList = argumentsMap.entrySet()
                    .stream()
                    .map(entry -> new WrappedArgumentId(entry.getKey(), entry.getValue()))
                    .toList();
        }

        final var resultId = node.get("resultId").asString();
        return new Snapshot.Operation(operationTrack, argumentsList, resultId);
    }
}
