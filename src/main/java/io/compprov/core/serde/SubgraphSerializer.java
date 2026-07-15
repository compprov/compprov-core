package io.compprov.core.serde;

import io.compprov.core.Subgraph;
import io.compprov.core.operation.WrappedOperation;
import io.compprov.core.variable.WrappedVariable;
import tools.jackson.core.JacksonException;
import tools.jackson.core.JsonGenerator;
import tools.jackson.databind.SerializationContext;
import tools.jackson.databind.ser.std.StdSerializer;

public class SubgraphSerializer extends StdSerializer<Subgraph> {

    public SubgraphSerializer() {
        this(null);
    }

    public SubgraphSerializer(Class<Subgraph> t) {
        super(t);
    }

    @Override
    public void serialize(Subgraph value, JsonGenerator gen, SerializationContext ctxt) throws JacksonException {
        gen.writeStartObject();
        gen.writePOJOProperty("argumentIds", value.argumentIds());
        gen.writePOJOProperty("resultId", value.resultId());
        gen.writePOJOProperty("operations", value.operations().stream().map(WrappedOperation::snapshot).toList());
        gen.writePOJOProperty("variables", value.variables().stream().map(WrappedVariable::snapshot).toList());
        gen.writeEndObject();
    }
}
