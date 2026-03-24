/**
 * Reproduces the interferometric gauge block calibration described in:
 * <em>Provenance in the Context of Metrological Traceability</em>,
 * Ryan M. White, NRC Canada, Metrology 2025, 5(3), 52.
 * DOI: <a href="https://doi.org/10.3390/metrology5030052">10.3390/metrology5030052</a>
 *
 * <h2>Domain background</h2>
 * <p>A <em>gauge block</em> is a precision length artefact used to realise and
 * disseminate the SI metre. Calibration is performed by optical interferometry:
 * monochromatic light reflected off the gauge block and a reference flat produces
 * a fringe pattern whose fractional order is measured, and the integer order is
 * resolved by the <em>method of exact fractions</em> — comparing fringe patterns
 * at three independent laser wavelengths simultaneously.
 *
 * <h2>What this example computes</h2>
 * <p>The calibration chain follows three correction stages:
 * <ol>
 *   <li><b>Wavelength correction</b> — three calibrated vacuum wavelengths
 *       (TESA SG-L 543 nm, TESA SG-O 612 nm, Spectra 117A-1 HeNe 633 nm) are
 *       divided by the refractive index of air computed from the Ciddor equation,
 *       yielding the effective wavelengths in the measurement medium.</li>
 *   <li><b>Interferometric length</b> — the primary (HeNe) air wavelength and
 *       the observed fringe order {@code m + f} give the raw length via
 *       {@code L_raw = (m + f) * lambda_air / 2}.</li>
 *   <li><b>Thermal correction</b> — the raw length is corrected to the ISO 1
 *       reference temperature (20 °C) using the tungsten carbide thermal expansion
 *       coefficient α = 4.23 × 10⁻⁶ K⁻¹ from the paper:
 *       {@code L_cal = L_raw / (1 + alpha * deltaT)}.</li>
 * </ol>
 * <p>The length deviation from the nominal 7 mm is computed and compared against
 * the paper's reported result of <strong>+2 nm</strong> (expanded uncertainty
 * U = 31 nm at k = 2).
 *
 * <h2>compprov usage pattern</h2>
 * <p>Unlike the NAV example (which demonstrates wrapping custom domain types),
 * this example shows that the built-in {@code WrappedBigDecimal} arithmetic is
 * sufficient for a pure-scalar formula chain. All inputs are wrapped via
 * {@code DefaultComputationContext.wrapBigDecimal()} and every arithmetic step
 * records itself into the CPG automatically. The full snapshot — the complete
 * machine-readable audit trail of the calibration — is captured in a single
 * {@code ctx.snapshot()} call, exactly as the paper proposes for metrological
 * traceability.
 *
 * <h2>Classes</h2>
 * <ul>
 *   <li>{@link io.compprov.examples.gaugeblock.GaugeBlockDataProvider} — stub data
 *       source returning the input values from the paper (wavelengths, environmental
 *       conditions, fringe observations, artefact properties).</li>
 *   <li>{@link io.compprov.examples.gaugeblock.GaugeBlockCalibration} — JUnit test
 *       that executes the full formula chain and asserts the deviation equals +2 nm.</li>
 * </ul>
 */
package io.compprov.examples.gaugeblock;