/**
 * Hydrology use case from Villamar et al. (2025) — model performance evaluation
 * for the mesoscale Hydrologic Model (mHM) applied to the Moselle River basin.
 *
 * <p>Reference:
 * Villamar et al., <em>Archivist: a metadata management tool for facilitating
 * FAIR research</em>, Scientific Data, 2025.
 * DOI: <a href="https://doi.org/10.1038/s41597-025-04521-6">10.1038/s41597-025-04521-6</a>
 *
 * <h2>Domain background</h2>
 * <p>The paper uses the calibration of a spatially distributed hydrological model
 * as a demonstration of metadata provenance management. The study domain is the
 * Moselle River basin upstream of Perl, Luxembourg/Germany (~11 500 km², altitude
 * 150–1300 m a.m.s.l.). The mHM model is driven by daily precipitation, air
 * temperature, and potential evapotranspiration, and produces daily river
 * discharge (m³ s⁻¹) as its primary output. Two parameter sets P₁ and P₂ are
 * compared; P₁ produces better KGE scores than P₂ across measurement stations.
 *
 * <h2>What this example computes</h2>
 * <p>The Kling-Gupta Efficiency (KGE) metric (Gupta et al. 2009) that the paper
 * uses to compare observed and simulated discharge:
 * <pre>
 *   KGE = 1 − √[ (r−1)² + (α−1)² + (β−1)² ]
 *
 *   r = Pearson correlation  = Σ(devObs·devSim) / √(Σ devObs² · Σ devSim²)
 *   α = variability ratio    = σ_sim / σ_obs
 *   β = bias ratio           = μ_sim / μ_obs
 * </pre>
 * <p>KGE = 1 is perfect; values below 0 mean the model is worse than
 * the observed mean as a predictor.
 *
 * <h2>Data disclaimer</h2>
 * <p>The paper provides <b>no actual discharge values, parameter values, or KGE
 * scores</b> — it uses the hydrology model only as a metadata management
 * demonstration. Both the observed and simulated discharge series in
 * {@link io.compprov.examples.hydrology.HydrologyDataProvider} are fabricated
 * synthetic stubs.  The simulated series is engineered so that r = 1, β = 1,
 * α = 0.9, giving <b>KGE = 0.9 exactly</b>, which is verifiable by exact
 * BigDecimal equality and consistent with the paper's qualitative statement that
 * P₁ yields KGE values "around 0.5 and higher".
 *
 * <h2>Classes</h2>
 * <ul>
 *   <li>{@link io.compprov.examples.hydrology.HydrologyDataProvider} — stub data
 *       source providing the observed and simulated discharge time series, with
 *       explicit per-value source documentation.</li>
 *   <li>{@link io.compprov.examples.hydrology.MhmDischargeEvaluation} — JUnit test
 *       that computes KGE step by step (means → deviations → correlation →
 *       variability ratio → bias ratio → KGE), tracking every intermediate
 *       quantity in the CPG, and asserts the result equals 0.9.</li>
 * </ul>
 */
package io.compprov.examples.hydrology;