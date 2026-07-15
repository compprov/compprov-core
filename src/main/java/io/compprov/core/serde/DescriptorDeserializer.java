package io.compprov.core.serde;

import io.compprov.core.meta.Descriptor;
import io.compprov.core.meta.Meta;
import io.compprov.core.meta.Pair;
import tools.jackson.core.JacksonException;
import tools.jackson.core.JsonParser;
import tools.jackson.databind.DeserializationContext;
import tools.jackson.databind.JavaType;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.deser.std.StdDeserializer;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;

public class DescriptorDeserializer extends StdDeserializer<Descriptor> {

    public DescriptorDeserializer() {
        super(Descriptor.class);
    }

    @Override
    public Descriptor deserialize(JsonParser p, DeserializationContext ctxt) throws JacksonException {
        JsonNode node = ctxt.readTree(p);
        JavaType listOfPairType = ctxt.getTypeFactory().constructCollectionType(ArrayList.class, Pair.class);
        JavaType mapOfStringObjectType = ctxt.getTypeFactory().constructMapType(LinkedHashMap.class, String.class, Object.class);

        String name = node.get("name").asString();
        JsonNode metaNode = node.get("meta");
        LinkedHashMap<String, Object> metaMap;
        if (metaNode.isArray()) {
            //to preserve order in JSON
            List<Pair> metaList = ctxt.readTreeAsValue(metaNode, listOfPairType);
            metaMap = new LinkedHashMap<>();
            metaList.forEach(pair -> metaMap.put(pair.key(), pair.value()));
        } else {
            //backward compatibility
            metaMap = ctxt.readTreeAsValue(metaNode, mapOfStringObjectType);
        }
        return new Descriptor(name, new Meta(metaMap));
    }
}
