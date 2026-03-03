package io.compprov.core.serde;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;

import java.io.IOException;
import java.math.MathContext;
import java.math.RoundingMode;

public class MathContextDeserializer extends StdDeserializer<MathContext> {

    public MathContextDeserializer() {
        super(MathContext.class);
    }

    @Override
    public MathContext deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
        JsonNode node = p.getCodec().readTree(p);
        int precision = node.get("precision").asInt();
        RoundingMode roundingMode = RoundingMode.valueOf(node.get("roundingMode").asText());
        return new MathContext(precision, roundingMode);
    }
}
