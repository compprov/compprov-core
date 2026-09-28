package io.compprov.core.serde;

import io.compprov.core.Snapshot;
import io.compprov.core.meta.Descriptor;
import tools.jackson.core.JacksonException;
import tools.jackson.core.JsonParser;
import tools.jackson.databind.DeserializationContext;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.deser.std.StdDeserializer;
import tools.jackson.databind.exc.InvalidFormatException;

import java.util.ArrayList;
import java.util.List;

public class SnapshotDeserializer extends StdDeserializer<Snapshot> {

    public SnapshotDeserializer() {
        super(Snapshot.class);
    }

    @Override
    public Snapshot deserialize(JsonParser p, DeserializationContext ctxt) throws JacksonException {
        JsonNode node = ctxt.readTree(p);
        String version = node.path("version").asString(Snapshot.CURRENT_VERSION);
        return switch (version) {
            case Snapshot.CURRENT_VERSION -> parseV10(node, ctxt);
            default ->
                    throw new InvalidFormatException(p, "Invalid snapshot version: " + version, version, Snapshot.class);
        };
    }

    private Snapshot parseV10(JsonNode node, DeserializationContext ctxt) {
        final var listOfVariables = ctxt.getTypeFactory().constructCollectionType(ArrayList.class, Snapshot.Variable.class);
        final var listOfOperations = ctxt.getTypeFactory().constructCollectionType(ArrayList.class, Snapshot.Operation.class);

        final var descriptor = ctxt.readTreeAsValue(node.path("descriptor"), Descriptor.class);
        List<Snapshot.Variable> variables = ctxt.readTreeAsValue(node.path("variables"), listOfVariables);
        List<Snapshot.Operation> operations = ctxt.readTreeAsValue(node.path("operations"), listOfOperations);

        return new Snapshot(descriptor, variables, operations);
    }
}
