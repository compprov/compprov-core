package io.compprov.core.serde;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;
import io.compprov.core.Subgraph;
import io.compprov.core.operation.WrappedOperation;
import io.compprov.core.variable.WrappedVariable;

import java.io.IOException;

public class SubgraphSerializer extends StdSerializer<Subgraph> {

    public SubgraphSerializer() {
        this(null);
    }

    public SubgraphSerializer(Class<Subgraph> t) {
        super(t);
    }

    @Override
    public void serialize(Subgraph value, JsonGenerator gen, SerializerProvider provider) throws IOException {
        gen.writeStartObject();
        gen.writeObjectField("argumentIds", value.argumentIds());
        gen.writeObjectField("resultId", value.resultId());
        gen.writeObjectField("operations", value.operations().stream().map(WrappedOperation::snapshot).toList());
        gen.writeObjectField("variables", value.variables().stream().map(WrappedVariable::snapshot).toList());
        gen.writeEndObject();
    }
}
