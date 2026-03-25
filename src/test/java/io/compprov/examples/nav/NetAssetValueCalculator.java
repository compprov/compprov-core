package io.compprov.examples.nav;

import io.compprov.core.meta.Meta;
import io.compprov.core.variable.ValueWithDescriptor;
import io.compprov.examples.nav.model.Amount;
import io.compprov.examples.nav.model.Currency;
import io.compprov.examples.nav.model.DataProvider;
import io.compprov.examples.nav.model.Rate;
import io.compprov.examples.nav.wrapped.NavComputationContext;
import io.compprov.examples.nav.wrapped.WrappedAmount;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

import static io.compprov.core.meta.Descriptor.descriptor;
import static org.junit.jupiter.api.Assertions.assertEquals;

public class NetAssetValueCalculator {

    @Test
    public void calculate() {

        NavComputationContext ctx = new NavComputationContext(descriptor("Nav calculation example"));
        DataProvider dataProvider = new DataProvider();

        //get rates
        final var btcUsdRate = ctx.wrap(
                dataProvider.fetchBtcUsdPrice(),
                descriptor("BTC/USD rate", Meta.of("origin", "Binance")));
        final var ethUsdRate = ctx.wrap(
                dataProvider.fetchEthUsdPrice(),
                descriptor("ETH/USD rate", Meta.of("origin", "Binance")));
        final var usdcUsdRate = ctx.wrap(
                dataProvider.fetchUsdcUsdPrice(),
                descriptor("USDC/USD rate", Meta.of("origin", "Binance")));

        //get assets
        final var binanceBtcAmount = ctx.wrap(
                dataProvider.fetchBinanceBtcAmount(),
                descriptor("BTC balance", Meta.of("source", "Binance")));
        final var binanceEthAmount = ctx.wrap(
                dataProvider.fetchBinanceEthAmount(),
                descriptor("ETH balance", Meta.of("source", "Binance")));
        final var binanceUsdcAmount = ctx.wrap(
                dataProvider.fetchBinanceUsdcAmount(),
                descriptor("USDC balance", Meta.of("source", "Binance")));
        final var stakedEthAmount = ctx.wrap(
                dataProvider.fetchStakedEthAmount(),
                descriptor("ETH balance", Meta.of("source", "Stake")));
        final var morphoUsdcAmount = ctx.wrap(
                dataProvider.fetchMorphoUsdcAmount(),
                descriptor("USDC balance", Meta.of("source", "Morpho")));

        //convert to usd and sum
        final var nav = binanceBtcAmount.convert(btcUsdRate, descriptor("BTC->USD"))
                .addBulk(List.of(
                                binanceEthAmount.convert(ethUsdRate, descriptor("ETH->USD")),
                                binanceUsdcAmount.convert(usdcUsdRate, descriptor("USDC->USD")),
                                binanceUsdcAmount.convert(usdcUsdRate, descriptor("USDC->USD")),
                                stakedEthAmount.convert(ethUsdRate, descriptor("ETH->USD")),
                                morphoUsdcAmount.convert(usdcUsdRate, descriptor("USDC->USD"))),
                        descriptor("Assets sum"));

        //store this JSON with the result
        final var netAssetValueSnapshot = ctx.snapshot();
        final var provenanceGraph = ctx.getEnvironment().toJson(netAssetValueSnapshot);
        //store(provenanceGraph)

        final var result = nav.getValue();
        assertEquals(new Amount(Currency.USD, new BigDecimal("432287.39")), result);
    }

    @Test
    public void reproduce() throws IOException {

        final var model = NetAssetValueCalculator.class.getResourceAsStream("/snapshots/nav.json").readAllBytes();
        final var snapshot = NavComputationContext.environment.fromJson(model);

        //recover calculations
        final var recalculated = NavComputationContext.environment.compute(snapshot);
        final var result = recalculated.findSingleVariable("Assets sum").getValue();

        assertEquals(new Amount(Currency.USD, new BigDecimal("432287.39")), result);
    }

    @Test
    public void simulate() throws IOException {

        final var model = NetAssetValueCalculator.class.getResourceAsStream("/snapshots/nav.json").readAllBytes();
        final var snapshot = NavComputationContext.environment.fromJson(model);
        final var recalculated = NavComputationContext.environment.compute(snapshot);

        final var btcPriceId = recalculated.findSingleVariable("BTC/USD rate").getVariableTrack().getId();
        final var btcGrowSimulation = NavComputationContext.environment.copyWith(
                snapshot,
                descriptor("Nav with increased BTC price: 92695.63"),
                Map.of(btcPriceId, new ValueWithDescriptor(
                        descriptor("BTC/USD rate", Meta.of("origin", "Simulation")),
                        new Rate(Currency.BTC, Currency.USD, new BigDecimal("92685.63")))));

        //simulate
        final var simulation = NavComputationContext.environment.compute(btcGrowSimulation);
        final WrappedAmount simulatedNav = (WrappedAmount) simulation.findSingleVariable("Assets sum");
        assertEquals(new Amount(Currency.USD, new BigDecimal("482759.68")), simulatedNav.getValue());
    }
}
