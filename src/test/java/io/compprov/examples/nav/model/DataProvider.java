package io.compprov.examples.nav.model;

import java.math.BigDecimal;

public class DataProvider {
    public Rate fetchBtcUsdPrice() {
        return new Rate(Currency.BTC, Currency.USD, new BigDecimal("68989.72"));
    }

    public Rate fetchEthUsdPrice() {
        return new Rate(Currency.ETH, Currency.USD, new BigDecimal("2083.31"));
    }

    public Rate fetchUsdcUsdPrice() {
        return new Rate(Currency.USDC, Currency.USD, new BigDecimal("1.01"));
    }

    public Amount fetchBinanceBtcAmount() {
        return new Amount(Currency.BTC, new BigDecimal("2.13"));
    }

    public Amount fetchBinanceEthAmount() {
        return new Amount(Currency.ETH, new BigDecimal("2.13"));
    }

    public Amount fetchBinanceUsdcAmount() {
        return new Amount(Currency.USDC, new BigDecimal("532.9"));
    }

    public Amount fetchMorphoUsdcAmount() {
        return new Amount(Currency.USDC, new BigDecimal("221114.9"));
    }

    public Amount fetchStakedEthAmount() {
        return new Amount(Currency.ETH, new BigDecimal("5.91"));
    }
}
