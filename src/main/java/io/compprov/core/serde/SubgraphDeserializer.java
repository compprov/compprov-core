package io.compprov.core.serde;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;
import io.compprov.core.ComputationEnvironment;
import io.compprov.core.Snapshot;
import io.compprov.core.Subgraph;
import io.compprov.core.meta.Descriptor;

import java.io.IOException;
import java.util.ArrayList;

public class SubgraphDeserializer extends StdDeserializer<Subgraph> {

    private final ComputationEnvironment environment;

    public SubgraphDeserializer(ComputationEnvironment environment) {
        super(Subgraph.class);
        this.environment = environment;
    }

    @Override
    public Subgraph deserialize(JsonParser p, DeserializationContext deserializationContext) throws IOException {

        ObjectMapper mapper = (ObjectMapper) p.getCodec();
        JsonNode node = mapper.readTree(p);

        final var arguments = mapper.convertValue(node.path("argumentIds"), new TypeReference<ArrayList<String>>() {
        });
        final var resultId = node.path("resultId").asText();
        final var variables = mapper.convertValue(node.path("variables"), new TypeReference<ArrayList<Snapshot.Variable>>() {
        });
        final var operations = mapper.convertValue(node.path("operations"), new TypeReference<ArrayList<Snapshot.Operation>>() {
        });

        final var ctx = environment.compute(new Snapshot(Descriptor.descriptor(""), variables, operations));
        return new Subgraph(ctx, arguments, resultId);
    }
}
