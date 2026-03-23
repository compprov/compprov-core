package io.compprov.examples.hydrology;

import io.compprov.core.ComputationEnvironment;
import io.compprov.core.DataContext;
import io.compprov.core.DefaultComputationContext;
import io.compprov.core.DefaultComputationEnvironment;
import io.compprov.core.wrappers.WrappedBigDecimal;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.math.MathContext;
import java.util.ArrayList;
import java.util.List;

import static io.compprov.core.meta.Descriptor.descriptor;
import static io.compprov.core.meta.Meta.of;
import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Evaluates the mHM model output against observed river discharge using the
 * Kling-Gupta Efficiency (KGE) metric, as described in Villamar et al. (2025).
 *
 * <p>The paper applies the mesoscale Hydrologic Model (mHM) to the Moselle River
 * basin upstream of Perl (~11 500 km²) and compares two parameter sets P₁ and P₂
 * using KGE. It reports that P₁ outperforms P₂ but provides no actual numerical
 * values — see {@link HydrologyDataProvider} for the data provenance.
 *
 * <h2>KGE formula (Gupta et al. 2009)</h2>
 * <pre>
 *   KGE = 1 − ED
 *
 *   ED  = √[ (r−1)² + (α−1)² + (β−1)² ]
 *
 *   where:
 *     r = Pearson correlation = Σ(devObs · devSim) / √(Σ devObs² · Σ devSim²)
 *     α = variability ratio  = σ_sim / σ_obs  = √(Σ devSim² / Σ devObs²)
 *     β = bias ratio         = μ_sim / μ_obs
 * </pre>
 *
 * <p>A KGE of 1 is perfect; values below 0 indicate that the simulation is worse
 * than using the observed mean as a predictor.
 *
 * <p>The synthetic dataset is engineered to give <b>KGE = 0.9 exactly</b>
 * (r = 1, β = 1, α = 0.9) so the result can be verified by exact BigDecimal equality.
 * Every intermediate quantity — means, deviations, correlation, variability ratio,
 * and bias ratio — is tracked in the CPG.
 *
 * @see <a href="https://doi.org/10.1038/s41597-025-04521-6">Villamar et al. 2025, Sci. Data</a>
 * @see <a href="https://doi.org/10.1016/j.jhydrol.2009.08.003">Gupta et al. 2009 (KGE)</a>
 */
public class MhmDischargeEvaluation {

    private static final ComputationEnvironment ENVIRONMENT = new DefaultComputationEnvironment();

    @Test
    public void evaluateParameterSetP1() {
        var ctx = new DefaultComputationContext(ENVIRONMENT,
                new DataContext(descriptor("mHM discharge evaluation — Moselle at Perl, parameter set P1")));
        var data = new HydrologyDataProvider();

        // ── Shared constants ──────────────────────────────────────────────────────
        var mc  = ctx.wrapMathContext(MathContext.DECIMAL128, descriptor("computation precision"));
        var one = ctx.wrapBigDecimal(BigDecimal.ONE, descriptor("constant 1"));

        // ── Observed discharge (gauge station Perl) ───────────────────────────────
        var obsValues = data.fetchObservedDischarge();
        var qObs = new ArrayList<WrappedBigDecimal>(obsValues.size());
        for (int i = 0; i < obsValues.size(); i++) {
            qObs.add(ctx.wrapBigDecimal(obsValues.get(i),
                    descriptor("q_obs[" + (i + 1) + "]: observed discharge day " + (i + 1) + ", m3/s",
                            of("station", "Moselle at Perl"))));
        }

        // ── Simulated discharge (mHM model, parameter set P1) ────────────────────
        var simValues = data.fetchSimulatedDischarge();
        var qSim = new ArrayList<WrappedBigDecimal>(simValues.size());
        for (int i = 0; i < simValues.size(); i++) {
            qSim.add(ctx.wrapBigDecimal(simValues.get(i),
                    descriptor("q_sim[" + (i + 1) + "]: simulated discharge day " + (i + 1) + ", m3/s",
                            of("model", "mHM", "parameter_set", "P1"))));
        }

        var n = ctx.wrapBigDecimal(
                new BigDecimal(obsValues.size()),
                descriptor("n: number of observation days"));

        // ══════════════════════════════════════════════════════════════════════════
        // STEP 1 — MEANS
        // ══════════════════════════════════════════════════════════════════════════

        var sumObs = qObs.get(0).addBulk(qObs.subList(1, qObs.size()), mc,
                descriptor("sum of observed discharge, m3/s"));
        var muObs = sumObs.divide(n, mc,
                descriptor("mu_obs: mean observed discharge, m3/s"));

        var sumSim = qSim.get(0).addBulk(qSim.subList(1, qSim.size()), mc,
                descriptor("sum of simulated discharge, m3/s"));
        var muSim = sumSim.divide(n, mc,
                descriptor("mu_sim: mean simulated discharge, m3/s"));

        // ══════════════════════════════════════════════════════════════════════════
        // STEP 2 — DEVIATIONS FROM MEAN
        // ══════════════════════════════════════════════════════════════════════════

        var devObs = new ArrayList<WrappedBigDecimal>(qObs.size());
        var devSim = new ArrayList<WrappedBigDecimal>(qSim.size());
        for (int i = 0; i < qObs.size(); i++) {
            devObs.add(qObs.get(i).subtract(muObs, mc,
                    descriptor("devObs[" + (i + 1) + "] = q_obs[" + (i + 1) + "] - mu_obs, m3/s")));
            devSim.add(qSim.get(i).subtract(muSim, mc,
                    descriptor("devSim[" + (i + 1) + "] = q_sim[" + (i + 1) + "] - mu_sim, m3/s")));
        }

        // ══════════════════════════════════════════════════════════════════════════
        // STEP 3 — SQUARED DEVIATIONS AND CROSS PRODUCTS
        // ══════════════════════════════════════════════════════════════════════════

        var sqDevObs  = new ArrayList<WrappedBigDecimal>(qObs.size());
        var sqDevSim  = new ArrayList<WrappedBigDecimal>(qSim.size());
        var crossProds = new ArrayList<WrappedBigDecimal>(qObs.size());
        for (int i = 0; i < devObs.size(); i++) {
            sqDevObs.add(devObs.get(i).multiply(devObs.get(i), mc,
                    descriptor("devObs[" + (i + 1) + "]^2")));
            sqDevSim.add(devSim.get(i).multiply(devSim.get(i), mc,
                    descriptor("devSim[" + (i + 1) + "]^2")));
            crossProds.add(devObs.get(i).multiply(devSim.get(i), mc,
                    descriptor("cross[" + (i + 1) + "] = devObs * devSim")));
        }

        // ══════════════════════════════════════════════════════════════════════════
        // STEP 4 — SUMS NEEDED FOR r AND α
        // ══════════════════════════════════════════════════════════════════════════

        // sumSqObs = 100 000,  sumSqSim = 81 000,  sumCross = 90 000
        var sumSqObs = sqDevObs.get(0).addBulk(sqDevObs.subList(1, sqDevObs.size()), mc,
                descriptor("sum of squared observed deviations"));
        var sumSqSim = sqDevSim.get(0).addBulk(sqDevSim.subList(1, sqDevSim.size()), mc,
                descriptor("sum of squared simulated deviations"));
        var sumCross = crossProds.get(0).addBulk(crossProds.subList(1, crossProds.size()), mc,
                descriptor("sum of cross products devObs*devSim"));

        // ══════════════════════════════════════════════════════════════════════════
        // STEP 5 — PEARSON CORRELATION COEFFICIENT r
        // r = sumCross / sqrt(sumSqObs * sumSqSim)
        // With these data: 90000 / sqrt(100000 * 81000) = 90000 / 90000 = 1
        // ══════════════════════════════════════════════════════════════════════════

        var denomR = sumSqObs.multiply(sumSqSim, mc, descriptor("sumSqObs * sumSqSim"))
                             .sqrt(mc, descriptor("sqrt(sumSqObs * sumSqSim)"));
        var r = sumCross.divide(denomR, mc,
                descriptor("r: Pearson linear correlation coefficient"));

        // ══════════════════════════════════════════════════════════════════════════
        // STEP 6 — VARIABILITY RATIO α = σ_sim / σ_obs = sqrt(sumSqSim / sumSqObs)
        // With these data: sqrt(81000 / 100000) = sqrt(0.81) = 0.9
        // ══════════════════════════════════════════════════════════════════════════

        var alpha = sumSqSim.divide(sumSqObs, mc, descriptor("sumSqSim / sumSqObs"))
                            .sqrt(mc, descriptor("alpha: variability ratio sigma_sim / sigma_obs"));

        // ══════════════════════════════════════════════════════════════════════════
        // STEP 7 — BIAS RATIO β = μ_sim / μ_obs
        // With these data: 300 / 300 = 1
        // ══════════════════════════════════════════════════════════════════════════

        var beta = muSim.divide(muObs, mc,
                descriptor("beta: bias ratio mu_sim / mu_obs"));

        // ══════════════════════════════════════════════════════════════════════════
        // STEP 8 — KGE = 1 − √[ (r−1)² + (α−1)² + (β−1)² ]
        // With these data: 1 − sqrt(0 + 0.01 + 0) = 1 − 0.1 = 0.9
        // ══════════════════════════════════════════════════════════════════════════

        var rErr     = r.subtract(one, mc, descriptor("r - 1"));
        var alphaErr = alpha.subtract(one, mc, descriptor("alpha - 1"));
        var betaErr  = beta.subtract(one, mc, descriptor("beta - 1"));

        var edSq = rErr.multiply(rErr, mc, descriptor("(r-1)^2"))
                       .addBulk(List.of(
                               alphaErr.multiply(alphaErr, mc, descriptor("(alpha-1)^2")),
                               betaErr.multiply(betaErr,  mc, descriptor("(beta-1)^2"))
                       ), mc, descriptor("ED^2 = (r-1)^2 + (alpha-1)^2 + (beta-1)^2"));

        var ed  = edSq.sqrt(mc, descriptor("ED: Euclidean distance in KGE space"));
        var kge = one.subtract(ed, mc, descriptor("KGE: Kling-Gupta Efficiency"));

        // Store the full CPG snapshot (provenance record) alongside the score.
        var snapshot = ctx.snapshot();

        // Villamar et al. (2025) report P1 outperforms P2 with KGE values mostly
        // above 0.5.  With the synthetic P1 data (r=1, β=1, α=0.9), KGE = 0.9 exactly.
        assertEquals(0, kge.getValue().compareTo(new BigDecimal("0.9")));
    }
}
