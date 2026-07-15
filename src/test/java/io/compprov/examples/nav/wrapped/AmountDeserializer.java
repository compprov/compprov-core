package io.compprov.examples.nav.wrapped;

import io.compprov.examples.nav.model.Amount;
import io.compprov.examples.nav.model.Currency;
import tools.jackson.core.JacksonException;
import tools.jackson.core.JsonParser;
import tools.jackson.databind.DeserializationContext;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.deser.std.StdDeserializer;

import java.math.BigDecimal;

/**
 * Amount might be represented as a record, but here we want to show how to register deserializers for custom types
 */
public class AmountDeserializer extends StdDeserializer<Amount> {

    public AmountDeserializer() {
        super(Amount.class);
    }

    @Override
    public Amount deserialize(JsonParser p, DeserializationContext ctxt) throws JacksonException {
        JsonNode node = ctxt.readTree(p);
        Currency currency = Currency.valueOf(node.get("currency").asString());
        BigDecimal amount = new BigDecimal(node.get("amount").asString());
        return new Amount(currency, amount);
    }
}
