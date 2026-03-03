package io.compprov.core.serde;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;
import io.compprov.core.meta.Meta;

import java.io.IOException;

public class NoMetaSerializer extends StdSerializer<Meta.NoMeta> {

    public NoMetaSerializer() {
        this(null);
    }

    public NoMetaSerializer(Class<Meta.NoMeta> t) {
        super(t);
    }

    @Override
    public void serialize(Meta.NoMeta value, JsonGenerator gen, SerializerProvider provider) throws IOException {
        gen.writeStartObject();
        gen.writeEndObject();
    }
}