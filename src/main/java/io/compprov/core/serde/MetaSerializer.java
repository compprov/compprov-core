package io.compprov.core.serde;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;
import io.compprov.core.meta.Meta;

import java.io.IOException;

public class MetaSerializer extends StdSerializer<Meta> {

    public MetaSerializer() {
        this(null);
    }

    public MetaSerializer(Class<Meta> t) {
        super(t);
    }

    @Override
    public void serialize(Meta value, JsonGenerator gen, SerializerProvider provider) throws IOException {
        gen.writeObject(value.getParameters());
    }
}