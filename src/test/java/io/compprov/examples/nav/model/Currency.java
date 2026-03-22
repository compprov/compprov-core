package io.compprov.examples.nav.model;

public enum Currency {

    BTC(8),

    ETH(18),

    USDC(6),

    USD(2);

    private final int decimals;

    Currency(int decimals) {
        this.decimals = decimals;
    }

    public int getDecimals() {
        return decimals;
    }
}
