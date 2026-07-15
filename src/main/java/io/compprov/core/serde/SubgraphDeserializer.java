package io.compprov.core.serde;

import io.compprov.core.ComputationEnvironment;
import io.compprov.core.Snapshot;
import io.compprov.core.Subgraph;
import io.compprov.core.meta.Descriptor;
import tools.jackson.core.JacksonException;
import tools.jackson.core.JsonParser;
import tools.jackson.databind.DeserializationContext;
import tools.jackson.databind.JavaType;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.deser.std.StdDeserializer;

import java.util.ArrayList;
import java.util.List;

public class SubgraphDeserializer extends StdDeserializer<Subgraph> {

    private final ComputationEnvironment environment;

    public SubgraphDeserializer(ComputationEnvironment environment) {
        super(Subgraph.class);
        this.environment = environment;
    }

    @Override
    public Subgraph deserialize(JsonParser p, DeserializationContext ctxt) throws JacksonException {
        JsonNode node = ctxt.readTree(p);

        JavaType listOfStringType = ctxt.getTypeFactory().constructCollectionType(ArrayList.class, String.class);
        JavaType listOfVariableType = ctxt.getTypeFactory().constructCollectionType(ArrayList.class, Snapshot.Variable.class);
        JavaType listOfOperationType = ctxt.getTypeFactory().constructCollectionType(ArrayList.class, Snapshot.Operation.class);

        List<String> arguments = ctxt.readTreeAsValue(node.path("argumentIds"), listOfStringType);
        final var resultId = node.path("resultId").asString();
        List<Snapshot.Variable> variables = ctxt.readTreeAsValue(node.path("variables"), listOfVariableType);
        List<Snapshot.Operation> operations = ctxt.readTreeAsValue(node.path("operations"), listOfOperationType);

        final var ctx = environment.compute(new Snapshot(Descriptor.descriptor(""), variables, operations));
        return new Subgraph(ctx, arguments, resultId);
    }
}
