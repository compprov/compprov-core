package io.compprov.core.serde;

import tools.jackson.core.JacksonException;
import tools.jackson.core.JsonParser;
import tools.jackson.databind.DeserializationContext;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.deser.std.StdDeserializer;

import java.math.MathContext;
import java.math.RoundingMode;

public class MathContextDeserializer extends StdDeserializer<MathContext> {

    public MathContextDeserializer() {
        super(MathContext.class);
    }

    @Override
    public MathContext deserialize(JsonParser p, DeserializationContext ctxt) throws JacksonException {
        JsonNode node = ctxt.readTree(p);
        int precision = node.get("precision").asInt();
        RoundingMode roundingMode = RoundingMode.valueOf(node.get("roundingMode").asString());
        return new MathContext(precision, roundingMode);
    }
}
