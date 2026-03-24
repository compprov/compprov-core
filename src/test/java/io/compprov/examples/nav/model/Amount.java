package io.compprov.examples.nav.model;

import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;
import java.util.Objects;

import static java.util.Objects.requireNonNull;

public class Amount {
    final Currency currency;
    final BigDecimal amount;

    public Amount(Currency currency, BigDecimal amount) {
        this.currency = requireNonNull(currency);
        this.amount = requireNonNull(amount);
    }

    public Amount add(Amount amount) {
        if (currency != amount.currency) {
            throw new IllegalArgumentException("Currency must fit");
        }

        return new Amount(currency, this.amount.add(amount.amount));
    }

    public Amount convert(Rate rate) {
        if (currency == rate.from()) {
            return new Amount(rate.to(), amount.multiply(rate.rate(), new MathContext(rate.to().getDecimals(), RoundingMode.DOWN)));
        } else if (currency == rate.to()) {
            return new Amount(rate.from(), amount.divide(rate.rate(), new MathContext(rate.from().getDecimals(), RoundingMode.DOWN)));
        }
        throw new IllegalArgumentException("Invalid rate currency");
    }

    public Currency getCurrency() {
        return currency;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    @Override
    public String toString() {
        return "Amount{" +
                "currency=" + currency +
                ", amount=" + amount.toPlainString() +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Amount amount1 = (Amount) o;
        return currency == amount1.currency && amount.compareTo(amount1.amount) == 0;
    }

    @Override
    public int hashCode() {
        return Objects.hash(currency, amount);
    }
}

