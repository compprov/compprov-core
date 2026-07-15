package io.compprov.core.serde;

import io.compprov.core.meta.Meta;
import io.compprov.core.meta.Pair;
import tools.jackson.core.JacksonException;
import tools.jackson.core.JsonGenerator;
import tools.jackson.databind.SerializationContext;
import tools.jackson.databind.ser.std.StdSerializer;

public class MetaSerializer extends StdSerializer<Meta> {

    public MetaSerializer() {
        this(null);
    }

    public MetaSerializer(Class<Meta> t) {
        super(t);
    }

    @Override
    public void serialize(Meta value, JsonGenerator gen, SerializationContext ctxt) throws JacksonException {
        gen.writePOJO(value.getParameters().entrySet()
                .stream()
                .map(entry -> new Pair(entry.getKey(), entry.getValue()))
                .toList());
    }
}