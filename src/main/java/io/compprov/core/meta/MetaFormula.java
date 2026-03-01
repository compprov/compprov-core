package io.compprov.core.meta;

public record MetaFormula(String formula) implements Meta {
    public static MetaFormula formula(String formula) {
        return new MetaFormula(formula);
    }
}
