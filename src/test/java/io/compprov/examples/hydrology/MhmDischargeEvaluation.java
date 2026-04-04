package io.compprov.examples.hydrology;

import io.compprov.core.ComputationEnvironment;
import io.compprov.core.DataContext;
import io.compprov.core.DefaultComputationContext;
import io.compprov.core.DefaultComputationEnvironment;
import io.compprov.core.meta.Meta;
import io.compprov.core.variable.ValueWithDescriptor;
import io.compprov.core.wrappers.WrappedBigDecimal;
import io.compprov.examples.nav.NetAssetValueCalculator;
import io.compprov.examples.nav.wrapped.NavComputationContext;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.math.BigDecimal;
import java.math.MathContext;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static io.compprov.core.meta.Descriptor.descriptor;

/**
 * Evaluates the mHM model output against observed river discharge using the
 * Kling-Gupta Efficiency (KGE) metric, as described in Villamar et al. (2025).
 *
 * <h2>KGE formula (Gupta et al. 2009)</h2>
 * <pre>
 *   KGE = 1 − √[ (r−1)² + (α−1)² + (β−1)² ]
 *
 *   where:
 *     r = Pearson correlation = Σ(devObs · devSim) / √(Σ devObs² · Σ devSim²)
 *     α = variability ratio  = σ_sim / σ_obs  = √(Σ devSim² / Σ devObs²)
 *     β = bias ratio         = μ_sim / μ_obs
 *
 *   KGE=1: Perfect match between simulated and observed data.
 *   KGE>0: Considered good or better than the mean, representing positive skill.
 *   KGE>0.3-0.5: Often considered "behavioral" or good in various hydrologic studies, depending on the strictness of the criteria.
 * </pre>
 *
 * @see <a href="https://doi.org/10.1038/s41597-025-04521-6">Villamar et al. 2025, Sci. Data</a>
 * @see <a href="https://doi.org/10.1016/j.jhydrol.2009.08.003">Gupta et al. 2009 (KGE)</a>
 */
public class MhmDischargeEvaluation {

    private static final LocalDate startDate = LocalDate.of(1991, 1, 1);

    private static final ComputationEnvironment ENVIRONMENT = new DefaultComputationEnvironment();

    @Test
    public void evaluateSimulation00() {
        final var ctx = new DefaultComputationContext(ENVIRONMENT,
                new DataContext(descriptor("mHM discharge evaluation", Meta.of(
                        "metric", "KGE",
                        "location", "Moselle River basin",
                        "date-from", "1991-01-01",
                        "date-to", "1991-06-30",
                        "units", "m3/s",
                        "simulation#", "00"))));
        final var data = new HydrologyDataProvider(ENVIRONMENT.getMapper());

        // Shared constants
        final var mc = ctx.wrapMathContext(MathContext.DECIMAL128, descriptor("computation precision"));
        final var one = ctx.wrapBigDecimal(BigDecimal.ONE, descriptor("constant 1"));
        final var two = ctx.wrapInteger(2, descriptor("constant 2"));
        final var n = ctx.wrapBigDecimal(BigDecimal.valueOf(data.fetchObservedValues().size()), descriptor("daysCount"));

        //wrap values, calculate means, devs, and squired devs
        Map<String, List<WrappedBigDecimal>> values = new HashMap<>();
        Map<String, List<WrappedBigDecimal>> devValues = new HashMap<>();
        Map<String, List<WrappedBigDecimal>> sqDevValues = new HashMap<>();
        for (String key : List.of("observed", "00")) {

            String prefix = "observed".equals(key) ? "observed" : "sim";

            //wrap values
            LocalDate date = startDate;
            final var scalarValues = data.fetchSimulatedValues(key);
            List<WrappedBigDecimal> wrappedValues = new ArrayList<>();
            values.put(prefix, wrappedValues);
            for (var value : scalarValues) {
                wrappedValues.add(ctx.wrapBigDecimal(value, descriptor(prefix + "_" + date)));
                date = date.plusDays(1);
            }

            //calculate mean
            final var mean = wrappedValues.get(0)
                    .addBulk(wrappedValues.subList(1, wrappedValues.size()), mc, descriptor(prefix + "_sum"))
                    .divide(n, mc, descriptor(prefix + "_mean"));

            //calculate devs and sqDevs
            date = startDate;
            List<WrappedBigDecimal> wrappedDevs = new ArrayList<>();
            devValues.put(prefix, wrappedDevs);
            List<WrappedBigDecimal> wrappedSqDevs = new ArrayList<>();
            sqDevValues.put(prefix, wrappedSqDevs);
            for (var wrappedValue : wrappedValues) {
                final var dev = mean.subtract(wrappedValue, mc, descriptor(prefix + "_" + date + "_dev"));
                wrappedDevs.add(dev);

                final var sqDev = dev.multiply(dev, mc, descriptor(prefix + "_" + date + "_sqdev"));
                wrappedSqDevs.add(sqDev);

                date = date.plusDays(1);
            }
        }

        //calculate crossDevs, dev sums, r, a, b and kge
        List<WrappedBigDecimal> obsDevs = devValues.get("observed");
        List<WrappedBigDecimal> simDevs = devValues.get("sim");
        List<WrappedBigDecimal> obsSqDevs = sqDevValues.get("observed");
        List<WrappedBigDecimal> simSqDevs = sqDevValues.get("sim");
        WrappedBigDecimal obsSumSqDevs = obsSqDevs.get(0).addBulk(obsSqDevs.subList(1, obsSqDevs.size()), mc, descriptor("observed_sum_sqdev"));
        WrappedBigDecimal simSumSqDevs = simSqDevs.get(0).addBulk(simSqDevs.subList(1, simSqDevs.size()), mc, descriptor("sim_sum_sqdev"));
        WrappedBigDecimal obsMean = (WrappedBigDecimal) ctx.findSingleVariable("observed_mean");
        WrappedBigDecimal simMean = (WrappedBigDecimal) ctx.findSingleVariable("sim_mean");

        //cross dev sum
        LocalDate date = startDate;
        List<WrappedBigDecimal> crossDevs = new ArrayList<>();
        for (int i = 0; i < obsDevs.size(); i++) {
            final var crossDev = obsDevs.get(i).multiply(simDevs.get(i), mc, descriptor("sim_" + date + "_crossdev"));
            date = date.plusDays(1);
            crossDevs.add(crossDev);
        }
        final var sumCrossDevs = crossDevs.get(0).addBulk(crossDevs.subList(1, crossDevs.size()), mc, descriptor("sim_sum_crossdev"));

        final var r = sumCrossDevs.divide(
                obsSumSqDevs.multiply(simSumSqDevs, mc, null).sqrt(mc, null),
                mc,
                descriptor("r"));
        final var a = simSumSqDevs.divide(obsSumSqDevs, mc, null).sqrt(mc, descriptor("a"));
        final var b = simMean.divide(obsMean, mc, descriptor("b"));

        //1-
        final var kge = one.subtract(
                //r-1
                r.subtract(one, mc, null)
                        //(r-1)^2
                        .pow(two, mc, null)
                        //(r-1)^2+(a-1)^2+(b-1)^2
                        .addBulk(List.of(
                                a.subtract(one, mc, null).pow(two, mc, null),
                                b.subtract(one, mc, null).pow(two, mc, null)
                        ), mc, null)
                        //SQRT[(r-1)^2+(a-1)^2+(b-1)^2]
                        .sqrt(mc, null),
                mc, descriptor("kge")
        );

        var snapshot = ctx.snapshot();
        final var provenanceGraph = ctx.getEnvironment().toJson(snapshot);
        //store(provenanceGraph)

        Assertions.assertEquals(new BigDecimal("0.8686859963808819097039722873919439"), kge.getValue());
    }

    @Test
    public void findBestModelUsingReproducibilityWithSubstitution() throws IOException {
        final var data = new HydrologyDataProvider(ENVIRONMENT.getMapper());
        final var model = NetAssetValueCalculator.class.getResourceAsStream("/snapshots/hydrology.json").readAllBytes();
        final var snapshot = NavComputationContext.environment.fromJson(model);

        //calculate KGE for every hydrological simulation
        Map<String, BigDecimal> kges = new HashMap<>();
        for (String caseId : data.fetchCaseIds()) {

            //prepare substitution map
            Map<String, ValueWithDescriptor> updates = new HashMap<>();
            final var scalarValues = data.fetchSimulatedValues(caseId);
            int simNumericId = snapshot.variables()
                    .stream()
                    .filter(v -> v.track().getDescriptor().getName().equals("sim_" + startDate))
                    .findFirst()
                    .get()
                    .track()
                    .getNumericId();
            LocalDate date = startDate;
            for (var scalarValue : scalarValues) {
                updates.put("i_" + simNumericId++, new ValueWithDescriptor(descriptor("sim_" + date), scalarValue));
                date = date.plusDays(1);
            }

            final var caseSpecificSnapshot = NavComputationContext.environment.copyWith(
                    snapshot,
                    descriptor("mHM discharge evaluation", Meta.of(
                            "metric", "KGE",
                            "location", "Moselle River basin",
                            "date-from", "1991-01-01",
                            "date-to", "1991-06-30",
                            "units", "m3/s",
                            "simulation#", caseId)),
                    updates);
            final var ctx = NavComputationContext.environment.compute(caseSpecificSnapshot);
            final var kge = ctx.findSingleVariable("kge");
            kges.put(caseId, (BigDecimal) kge.getValue());
        }

        //find max kge
        BigDecimal maxKge = new BigDecimal("-1");
        String maxKgeCaseId = "none";
        for (var e : kges.entrySet()) {
            if (e.getValue().compareTo(maxKge) > 0) {
                maxKge = e.getValue();
                maxKgeCaseId = e.getKey();
            }
        }
        Assertions.assertEquals("03", maxKgeCaseId);
        Assertions.assertEquals(new BigDecimal("0.9391174691699493751867141555186104"), maxKge);
    }
}
