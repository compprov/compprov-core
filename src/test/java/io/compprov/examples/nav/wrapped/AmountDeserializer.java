package io.compprov.examples.nav.wrapped;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;
import io.compprov.examples.nav.model.Amount;
import io.compprov.examples.nav.model.Currency;

import java.io.IOException;
import java.math.BigDecimal;
import java.math.MathContext;

/**
 * Amount might be represented as a record, but here we want to show how to register deserializers for custom types
 */
public class AmountDeserializer extends StdDeserializer<Amount> {

    public AmountDeserializer() {
        super(MathContext.class);
    }

    @Override
    public Amount deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
        JsonNode node = p.getCodec().readTree(p);
        Currency currency = Currency.valueOf(node.get("currency").asText());
        BigDecimal amount = node.get("amount").decimalValue();
        return new Amount(currency, amount);
    }
}
