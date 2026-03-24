package io.compprov.examples.hydrology;

import java.math.BigDecimal;
import java.util.List;

/**
 * Stub data provider supplying observed and simulated daily river discharge
 * time series for the Kling-Gupta Efficiency (KGE) evaluation demonstrated in
 * Villamar et al. (2025).
 *
 * <p>The paper uses the Moselle River basin upstream of Perl (catchment area
 * ~11 500 km², altitude 150–1300 m a.m.s.l.) as the study domain, and runs the
 * mesoscale Hydrologic Model (mHM) driven by daily precipitation, temperature,
 * and potential evapotranspiration. It compares two parameter sets P₁ and P₂
 * using the KGE metric and reports that P₁ outperforms P₂ — but it gives
 * <b>no actual discharge values, parameter values, or KGE scores</b>.
 *
 * <p><b>Data provenance of each value:</b>
 * <table border="1">
 *   <tr><th>Method</th><th>Values [m³ s⁻¹]</th><th>Source</th></tr>
 *   <tr><td>fetchObservedDischarge</td><td>100, 200, 300, 400, 500</td>
 *       <td>FABRICATED STUB — synthetic series physically plausible for the Moselle
 *           at Perl (mean ~300 m³ s⁻¹, range 0–3000 m³ s⁻¹ per Fig. 5C).
 *           The paper provides no observed values.</td></tr>
 *   <tr><td>fetchSimulatedDischarge</td><td>120, 210, 300, 390, 480</td>
 *       <td>FABRICATED STUB — engineered so that the mHM parameter set P₁ output
 *           has perfect linear correlation (r = 1) and zero mean bias (β = 1) but
 *           10 % lower variability (α = 0.9) relative to the observed series,
 *           giving KGE = 0.9 exactly. The paper provides no simulated values.</td></tr>
 * </table>
 */
public class HydrologyDataProvider {

    /**
     * Observed daily river discharge at the Moselle at Perl gauge station, m³ s⁻¹.
     *
     * <p><b>Source:</b> FABRICATED STUB. The paper (Villamar et al. 2025, Fig. 5C)
     * shows a ~1000-day observed discharge time series with values up to
     * ~3 × 10³ m³ s⁻¹ but provides no actual numerical readings. This five-day
     * synthetic series (100–500 m³ s⁻¹, mean = 300 m³ s⁻¹) is physically
     * plausible for the Moselle River.
     */
    public List<BigDecimal> fetchObservedDischarge() {
        return List.of(
                new BigDecimal("100"),
                new BigDecimal("200"),
                new BigDecimal("300"),
                new BigDecimal("400"),
                new BigDecimal("500")
        );
    }

    /**
     * Simulated daily river discharge produced by the mHM model under parameter
     * set P₁, m³ s⁻¹.
     *
     * <p><b>Source:</b> FABRICATED STUB. The paper gives no simulation output
     * values. This series is constructed as:
     * {@code q_sim[i] = mu_obs + 0.9 × (q_obs[i] − mu_obs)}, which guarantees:
     * <ul>
     *   <li>r = 1 (perfect Pearson correlation)</li>
     *   <li>β = μ_sim / μ_obs = 1 (no mean bias)</li>
     *   <li>α = σ_sim / σ_obs = 0.9 (10 % underestimation of variability)</li>
     *   <li>KGE = 1 − √[(r−1)² + (α−1)² + (β−1)²] = 1 − 0.1 = <b>0.9 exactly</b></li>
     * </ul>
     */
    public List<BigDecimal> fetchSimulatedDischarge() {
        return List.of(
                new BigDecimal("120"),
                new BigDecimal("210"),
                new BigDecimal("300"),
                new BigDecimal("390"),
                new BigDecimal("480")
        );
    }
}
