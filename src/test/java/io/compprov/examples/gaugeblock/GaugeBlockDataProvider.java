package io.compprov.examples.gaugeblock;

import java.math.BigDecimal;

/**
 * Stub data provider supplying the raw measurement inputs for the NRC 91A 7 mm
 * tungsten carbide gauge block calibration described in White (2025).
 *
 * <p>In a real system these values come from calibration certificates,
 * instrument readouts, and environmental sensors. Here they are hard-coded so
 * the example is self-contained and deterministic.
 *
 * <p><b>Data provenance of each value</b> (see individual method Javadoc for details):
 * <table border="1">
 *   <tr><th>Method</th><th>Value</th><th>Source</th></tr>
 *   <tr><td>fetchLambdaVac1</td><td>543.5153892 nm</td><td>White (2025), Appendix B</td></tr>
 *   <tr><td>fetchLambdaVac2</td><td>611.9703724 nm</td><td>White (2025), Appendix B</td></tr>
 *   <tr><td>fetchLambdaVac3</td><td>632.9909778 nm</td><td>White (2025), Appendix B</td></tr>
 *   <tr><td>fetchThermalExpansionCoefficient</td><td>4.23 × 10⁻⁶ K⁻¹</td><td>White (2025), Appendix B</td></tr>
 *   <tr><td>fetchNominalLength</td><td>7 000 000 nm</td><td>White (2025) — "NRC 91A 7 mm gauge block"</td></tr>
 *   <tr><td>fetchReferenceTemperature</td><td>20.000 °C</td><td>ISO 1:2002 — implicit in all gauge block calibrations, not stated in the paper</td></tr>
 *   <tr><td>fetchCO2MoleFraction</td><td>450.0 ppm</td><td>Standard atmospheric value; not stated in the paper</td></tr>
 *   <tr><td>fetchAirTemperature</td><td>20.00 °C</td><td>Fabricated stub — paper lists air temperature as an influence quantity but gives no value</td></tr>
 *   <tr><td>fetchAirPressure</td><td>101 325.0 Pa</td><td>Fabricated stub — paper lists pressure as an influence quantity but gives no value</td></tr>
 *   <tr><td>fetchRelativeHumidity</td><td>0.50</td><td>Fabricated stub — paper lists humidity as an influence quantity but gives no value</td></tr>
 *   <tr><td>fetchSaturationVaporPressure</td><td>2338.0 Pa</td><td>Wexler (1976) formula evaluated at 20 °C; not from the paper. The exp() sub-step is not tracked in the CPG.</td></tr>
 *   <tr><td>fetchPartTemperature</td><td>20.001 °C</td><td>Fabricated stub — paper lists part temperature as an influence quantity but gives no value</td></tr>
 *   <tr><td>fetchIntegerFringeOrder</td><td>22123</td><td>Fabricated stub — back-calculated from nominal length; paper gives no fringe observations</td></tr>
 *   <tr><td>fetchFractionalFringeOrder</td><td>0.23675</td><td>Fabricated stub — chosen so the full formula chain produces ≈ +2 nm; paper gives no fringe observations</td></tr>
 * </table>
 */
public class GaugeBlockDataProvider {

    // ── Measurement standards: calibrated vacuum wavelengths ─────────────────────

    /**
     * Calibrated vacuum wavelength of the TESA SG-L laser (~543 nm, green).
     *
     * <p><b>Source:</b> White (2025), Appendix B — calibration report OFS-2024-0006,
     * dated 2024-05-02. Exact value from the paper: +5.435153892 × 10² nm.
     */
    public BigDecimal fetchLambdaVac1() {
        return new BigDecimal("543.5153892");
    }

    /**
     * Calibrated vacuum wavelength of the TESA SG-O laser (~612 nm, red).
     *
     * <p><b>Source:</b> White (2025), Appendix B — calibration report OFS-2024-0005,
     * dated 2024-04-30. Exact value from the paper: +6.119703724 × 10² nm.
     */
    public BigDecimal fetchLambdaVac2() {
        return new BigDecimal("611.9703724");
    }

    /**
     * Calibrated vacuum wavelength of the Spectra 117A-1 HeNe laser (~633 nm).
     * Used as the primary wavelength for length computation.
     *
     * <p><b>Source:</b> White (2025), Appendix B — calibration report OFS-2024-0002,
     * dated 2024-05-08. Exact value from the paper: +6.329909778 × 10² nm.
     */
    public BigDecimal fetchLambdaVac3() {
        return new BigDecimal("632.9909778");
    }

    // ── Environmental conditions (inputs to the Ciddor equation) ─────────────────

    /**
     * Air temperature in the interferometer during measurement, °C.
     *
     * <p><b>Source:</b> FABRICATED STUB. The paper lists air temperature as an
     * influence quantity in the provenance graph but gives no numerical value.
     * 20.00 °C is the ISO 1 reference temperature and a typical lab value.
     */
    public BigDecimal fetchAirTemperature() {
        return new BigDecimal("20.00");
    }

    /**
     * Atmospheric pressure in the interferometer during measurement, Pa.
     *
     * <p><b>Source:</b> FABRICATED STUB. The paper lists pressure as an influence
     * quantity but gives no numerical value. 101 325 Pa is standard atmosphere.
     */
    public BigDecimal fetchAirPressure() {
        return new BigDecimal("101325.0");
    }

    /**
     * Relative humidity in the interferometer during measurement (dimensionless, 0–1).
     *
     * <p><b>Source:</b> FABRICATED STUB. The paper lists humidity as an influence
     * quantity but gives no numerical value. 0.50 (50 %) is a typical lab value.
     */
    public BigDecimal fetchRelativeHumidity() {
        return new BigDecimal("0.50");
    }

    /**
     * Saturation vapor pressure of water at the measurement temperature, Pa.
     *
     * <p><b>Source:</b> NOT from the paper. Computed from the Wexler (1976) formula:
     * {@code svp = exp(A·T² + B·T + C + D/T)} evaluated at T = 293.15 K, giving
     * approximately 2338 Pa. The {@code exp()} sub-step is <b>not</b> tracked in the
     * CPG because {@code BigDecimal} does not support transcendental functions natively.
     * In production this value would come from a calibrated hygrometer reading or from
     * a separate, fully traced computation chain.
     */
    public BigDecimal fetchSaturationVaporPressure() {
        return new BigDecimal("2338.0");
    }

    /**
     * CO₂ mole fraction of air in the interferometer, ppm.
     *
     * <p><b>Source:</b> NOT from the paper. 450 ppm is the standard atmospheric
     * value used as a default in the Ciddor (1996) equation. The paper does not
     * state a CO₂ concentration.
     */
    public BigDecimal fetchCO2MoleFraction() {
        return new BigDecimal("450.0");
    }

    // ── Artefact properties ───────────────────────────────────────────────────────

    /**
     * Thermal expansion coefficient of tungsten carbide, the material of the
     * NRC 91A gauge block, K⁻¹.
     *
     * <p><b>Source:</b> White (2025), Appendix B — explicitly stated as
     * 4.23 × 10⁻⁶ K⁻¹.
     */
    public BigDecimal fetchThermalExpansionCoefficient() {
        return new BigDecimal("0.00000423");
    }

    /**
     * Part temperature measured during the interferometric observation, °C.
     *
     * <p><b>Source:</b> FABRICATED STUB. The paper lists part temperature as an
     * influence quantity but gives no numerical value. 20.001 °C (0.001 K above
     * reference) is physically realistic and produces a small but non-zero
     * thermal correction.
     */
    public BigDecimal fetchPartTemperature() {
        return new BigDecimal("20.001");
    }

    /**
     * ISO 1 reference temperature for length metrology, °C.
     *
     * <p><b>Source:</b> ISO 1:2002. Not stated in the paper but implicit in any
     * gauge block calibration performed under international standards.
     */
    public BigDecimal fetchReferenceTemperature() {
        return new BigDecimal("20.000");
    }

    // ── Interferometric observations ──────────────────────────────────────────────

    /**
     * Integer fringe order {@code m} for the primary (HeNe) wavelength, as
     * determined by the method of exact fractions applied to all three lasers.
     *
     * <p><b>Source:</b> FABRICATED STUB. The paper gives no fringe observations.
     * This value was back-calculated from the 7 mm nominal length:
     * {@code m = floor(2 · L_nom · n / lambda_vac3) = 22123}.
     */
    public BigDecimal fetchIntegerFringeOrder() {
        return new BigDecimal("22123");
    }

    /**
     * Fractional fringe order {@code f} for the primary wavelength, read from the
     * interferometric fringe pattern.
     *
     * <p><b>Source:</b> FABRICATED STUB. The paper gives no fringe observations.
     * This value was chosen so that the complete formula chain — including the
     * Ciddor-computed refractive index — produces a length deviation of ≈ +1.97 nm,
     * which rounds to the paper's reported result of +2 nm.
     */
    public BigDecimal fetchFractionalFringeOrder() {
        return new BigDecimal("0.23675");
    }

    // ── Artefact nominal length ───────────────────────────────────────────────────

    /**
     * Nominal length of the NRC 91A gauge block expressed in nanometres.
     *
     * <p><b>Source:</b> White (2025) — the paper identifies the artefact as
     * "NRC 91A 7 mm gauge block". 7 mm = 7 000 000 nm.
     */
    public BigDecimal fetchNominalLength() {
        return new BigDecimal("7000000");
    }
}
