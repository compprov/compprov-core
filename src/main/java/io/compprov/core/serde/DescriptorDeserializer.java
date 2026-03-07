package io.compprov.core.serde;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;
import io.compprov.core.meta.Descriptor;
import io.compprov.core.meta.Meta;

import java.io.IOException;
import java.util.LinkedHashMap;

public class DescriptorDeserializer extends StdDeserializer<Descriptor> {

    public DescriptorDeserializer() {
        super(Descriptor.class);
    }

    @Override
    public Descriptor deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {

        ObjectMapper mapper = (ObjectMapper) p.getCodec();
        JsonNode node = p.getCodec().readTree(p);

        String name = node.get("name").asText();
        JsonNode metaNode = node.get("meta");
        LinkedHashMap<String, Object> metaMap = mapper.convertValue(
                metaNode,
                new TypeReference<LinkedHashMap<String, Object>>() {
                }
        );
        return new Descriptor(name, new Meta(metaMap));
    }
}
