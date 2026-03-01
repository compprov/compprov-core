package io.compprov.core.wrappers;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;
import io.compprov.core.variable.AbstractWrappedVariable;

import java.io.IOException;

public class AbstractWrappedVariableSerializer extends StdSerializer<AbstractWrappedVariable> {

    public AbstractWrappedVariableSerializer() {
        this(null);
    }

    public AbstractWrappedVariableSerializer(Class<AbstractWrappedVariable> t) {
        super(t);
    }

    @Override
    public void serialize(AbstractWrappedVariable value, JsonGenerator gen, SerializerProvider provider) throws IOException {
        gen.writeStartObject();
        gen.writeFieldName("value");
        gen.writeObject(value.getValue());
        gen.writeFieldName("track");
        gen.writeObject(value.getVariableTrack());
        gen.writeEndObject();
    }
}