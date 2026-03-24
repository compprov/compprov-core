package io.compprov.examples.nav.model;

import java.math.BigDecimal;

import static java.util.Objects.requireNonNull;

public record Rate(Currency from, Currency to, BigDecimal rate) {

    public Rate {
        requireNonNull(from);
        requireNonNull(to);
        requireNonNull(rate);
    }

    @Override
    public String toString() {
        return "Rate{" +
                "from=" + from +
                ", to=" + to +
                ", rate=" + rate.toPlainString() +
                '}';
    }
}
