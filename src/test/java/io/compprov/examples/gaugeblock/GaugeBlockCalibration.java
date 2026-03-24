package io.compprov.examples.gaugeblock;

import io.compprov.core.ComputationEnvironment;
import io.compprov.core.DataContext;
import io.compprov.core.DefaultComputationContext;
import io.compprov.core.DefaultComputationEnvironment;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;

import static io.compprov.core.meta.Descriptor.descriptor;
import static io.compprov.core.meta.Meta.of;
import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Reproduces the interferometric gauge block calibration documented in White (2025).
 *
 * <p>The measurement uses the <em>method of exact fractions</em>: three calibrated
 * laser wavelengths are compared against the gauge block by optical interferometry,
 * and the redundancy across wavelengths resolves the otherwise ambiguous integer
 * fringe order.  The calibrated length is then obtained by applying two corrections:
 *
 * <ol>
 *   <li><b>Wavelength correction</b> — the vacuum wavelength of the primary laser
 *       is divided by the refractive index of air {@code n}, computed here from the
 *       Birch &amp; Downs (1993) modified Edlén formula (a close variant of the
 *       Ciddor 1996 equation).  The formula takes air temperature, pressure, humidity
 *       (via the partial pressure of water vapor), and CO₂ concentration as inputs
 *       and produces a fully-traced {@code n} value in the CPG.</li>
 *   <li><b>Thermal correction</b> — the raw interferometric length is corrected to
 *       the ISO 1 reference temperature of 20 °C using the tungsten carbide thermal
 *       expansion coefficient α = 4.23 × 10⁻⁶ K⁻¹ from the paper.</li>
 * </ol>
 *
 * <p>Complete formula chain:
 * <pre>
 *   ── Ciddor / Birch-Downs (refractive index of air) ──────────────────────────
 *   sigma     = 1 / (lambda_vac3 [µm])                     wavenumber [µm⁻¹]
 *   N_s       = 8342.54 + 2406147/(130-σ²) + 15998/(38.9-σ²)   standard refractivity [×10⁻⁸]
 *   N_s_co2   = N_s × (1 + 0.534e-6 × (xCO₂ - 450))            CO₂ correction
 *   N_tp      = N_s_co2 × P × (1 + P·1e-8·(0.601-0.00972·T))   T/P correction
 *                        / (96095.43 × (1 + 0.003661·T))
 *   f_enh     = 1.00070 + 3.7e-8·P - 1.24e-7·T²                water vapor enhancement
 *   pv        = h × f_enh × svp                                  partial vapor pressure [Pa]
 *   N_v       = -(3.8020 - 0.0384·σ²) × pv × 1e-3              water vapor correction [×10⁻⁸]
 *   n         = 1 + (N_tp + N_v) × 1e-8
 *
 *   ── Interferometric length ───────────────────────────────────────────────────
 *   lambda_air = lambda_vac3 / n
 *   L_raw      = (m + f) × lambda_air / 2
 *
 *   ── Thermal correction ──────────────────────────────────────────────────────
 *   L_cal      = L_raw / (1 + alpha × (T_part - T_ref))
 *
 *   ── Result ──────────────────────────────────────────────────────────────────
 *   deltaL     = L_cal - L_nom
 * </pre>
 *
 * <p>Paper result: deltaL = +2 nm  (expanded uncertainty U = 31 nm at k = 2).
 *
 * @see <a href="https://doi.org/10.3390/metrology5030052">White 2025, Metrology 5(3), 52</a>
 */
public class GaugeBlockCalibration {

    private static final ComputationEnvironment ENVIRONMENT = new DefaultComputationEnvironment();

    @Test
    public void calibrate() {
        var ctx = new DefaultComputationContext(ENVIRONMENT,
                new DataContext(descriptor("NRC 91A 7mm gauge block calibration, White 2025")));
        var data = new GaugeBlockDataProvider();

        // ── Shared constants ──────────────────────────────────────────────────────
        var mc  = ctx.wrapMathContext(MathContext.DECIMAL128, descriptor("computation precision"));
        var one = ctx.wrapBigDecimal(BigDecimal.ONE,       descriptor("constant 1"));
        var two = ctx.wrapBigDecimal(new BigDecimal("2"),  descriptor("constant 2"));

        // ── Measurement standards: calibrated vacuum wavelengths ──────────────────
        // All three wavelengths feed the method of exact fractions.
        // Only the primary (HeNe, lambda_vac3) is used for the final length formula;
        // the others anchor the integer-order resolution step.
        ctx.wrapBigDecimal(data.fetchLambdaVac1(),
                descriptor("lambda_vac1: TESA SG-L vacuum wavelength, nm",
                        of("report", "OFS-2024-0006", "date", "2024-05-02")));
        ctx.wrapBigDecimal(data.fetchLambdaVac2(),
                descriptor("lambda_vac2: TESA SG-O vacuum wavelength, nm",
                        of("report", "OFS-2024-0005", "date", "2024-04-30")));
        var lambdaVac3 = ctx.wrapBigDecimal(data.fetchLambdaVac3(),
                descriptor("lambda_vac3: Spectra 117A-1 HeNe vacuum wavelength, nm",
                        of("report", "OFS-2024-0002", "date", "2024-05-08")));

        // ══════════════════════════════════════════════════════════════════════════
        // CIDDOR EQUATION  (Birch & Downs 1993 modified Edlén formula)
        // Computes the refractive index of moist air n from T, P, humidity, CO₂.
        // ══════════════════════════════════════════════════════════════════════════

        // ── Environmental inputs ──────────────────────────────────────────────────
        var airTemp = ctx.wrapBigDecimal(data.fetchAirTemperature(),
                descriptor("T_air: air temperature, degC"));
        var airPressure = ctx.wrapBigDecimal(data.fetchAirPressure(),
                descriptor("P_air: air pressure, Pa"));
        var humidity = ctx.wrapBigDecimal(data.fetchRelativeHumidity(),
                descriptor("h: relative humidity"));
        // svp is pre-computed from the Wexler (1976) formula at T=20°C;
        // the exp() sub-step is not tracked in the CPG.
        var svp = ctx.wrapBigDecimal(data.fetchSaturationVaporPressure(),
                descriptor("svp: saturation vapor pressure at T_air (Wexler 1976), Pa"));
        var co2ppm = ctx.wrapBigDecimal(data.fetchCO2MoleFraction(),
                descriptor("xCO2: CO2 mole fraction, ppm"));

        // ── Step 1: wavenumber of the primary laser ───────────────────────────────
        // sigma = 1 / lambda_vac3[µm]  = 1000 / lambda_vac3[nm]
        var cNmToUm = ctx.wrapBigDecimal(new BigDecimal("1000"),
                descriptor("nm-to-um conversion factor"));
        var lambdaVac3um = lambdaVac3.divide(cNmToUm, mc,
                descriptor("lambda_vac3 in um"));
        var sigma = one.divide(lambdaVac3um, mc,
                descriptor("sigma: vacuum wavenumber, um^-1"));
        var sigmaSq = sigma.multiply(sigma, mc,
                descriptor("sigma^2, um^-2"));

        // ── Step 2: standard refractivity N_s (Sellmeier dispersion formula) ─────
        // Dry air at standard conditions: T=15°C, P=101325 Pa, xCO2=450 ppm.
        // N_s = 8342.54 + 2406147/(130 - sigma^2) + 15998/(38.9 - sigma^2)  [×10^-8]
        var cK0    = ctx.wrapBigDecimal(new BigDecimal("8342.54"),  descriptor("Sellmeier k0 [×10^-8]"));
        var cK1    = ctx.wrapBigDecimal(new BigDecimal("2406147"),  descriptor("Sellmeier k1 numerator"));
        var cK2    = ctx.wrapBigDecimal(new BigDecimal("15998"),    descriptor("Sellmeier k2 numerator"));
        var cUvRes = ctx.wrapBigDecimal(new BigDecimal("130"),      descriptor("Sellmeier UV resonance, um^-2"));
        var cIrRes = ctx.wrapBigDecimal(new BigDecimal("38.9"),     descriptor("Sellmeier IR resonance, um^-2"));

        var denomUv = cUvRes.subtract(sigmaSq, mc, descriptor("130 - sigma^2"));
        var denomIr = cIrRes.subtract(sigmaSq, mc, descriptor("38.9 - sigma^2"));
        var termUv  = cK1.divide(denomUv, mc, descriptor("2406147 / (130 - sigma^2)"));
        var termIr  = cK2.divide(denomIr, mc, descriptor("15998 / (38.9 - sigma^2)"));
        var Ns      = cK0.add(termUv, mc, descriptor("Ns partial"))
                         .add(termIr, mc, descriptor("N_s: standard refractivity [×10^-8]"));

        // ── Step 3: CO₂ correction ────────────────────────────────────────────────
        // Standard reference is 450 ppm; factor = 1 + 0.534e-6 × (xCO2 - 450)
        var cCO2Ref   = ctx.wrapBigDecimal(new BigDecimal("450"),          descriptor("CO2 reference concentration, ppm"));
        var cCO2Coeff = ctx.wrapBigDecimal(new BigDecimal("0.000000534"),  descriptor("CO2 correction coefficient"));
        var deltaCO2  = co2ppm.subtract(cCO2Ref, mc, descriptor("xCO2 - 450"));
        var co2Factor = one.add(cCO2Coeff.multiply(deltaCO2, mc, descriptor("CO2 correction term")),
                                mc, descriptor("CO2 correction factor"));
        var NsCO2 = Ns.multiply(co2Factor, mc, descriptor("N_s corrected for CO2 [×10^-8]"));

        // ── Step 4: temperature and pressure correction (Birch & Downs 1993) ─────
        // N_tp = N_s_co2 × P × (1 + P·1e-8·(0.601 - 0.00972·T)) / (96095.43 × (1 + 0.003661·T))
        var cBdDenom = ctx.wrapBigDecimal(new BigDecimal("96095.43"),  descriptor("B&D denominator constant"));
        var cBdA     = ctx.wrapBigDecimal(new BigDecimal("0.601"),     descriptor("B&D pressure coefficient a"));
        var cBdB     = ctx.wrapBigDecimal(new BigDecimal("0.00972"),   descriptor("B&D pressure coefficient b"));
        var c1e8     = ctx.wrapBigDecimal(new BigDecimal("1E-8"),      descriptor("scale factor 1e-8"));
        var cBdTherm = ctx.wrapBigDecimal(new BigDecimal("0.003661"),  descriptor("B&D thermal coefficient"));

        var bdTempTerm  = cBdA.subtract(cBdB.multiply(airTemp, mc, descriptor("b*T")),
                                        mc, descriptor("a - b*T_air"));
        var bdPressCorr = one.add(
                airPressure.multiply(c1e8, mc, descriptor("P*1e-8"))
                           .multiply(bdTempTerm, mc, descriptor("P*1e-8*(a - b*T_air)")),
                mc, descriptor("pressure correction factor (1 + P*1e-8*(a-b*T))"));
        var bdThermCorr = one.add(cBdTherm.multiply(airTemp, mc, descriptor("0.003661*T_air")),
                                  mc, descriptor("thermal denominator factor (1 + 0.003661*T)"));
        var bdDenomTP   = cBdDenom.multiply(bdThermCorr, mc,
                descriptor("B&D full denominator (96095.43*(1+0.003661*T))"));
        var Ntp = NsCO2.multiply(airPressure, mc, descriptor("N_s_co2 * P"))
                       .multiply(bdPressCorr,  mc, descriptor("N_s_co2 * P * pressCorr"))
                       .divide(bdDenomTP,      mc, descriptor("N_tp: dry air refractivity [×10^-8]"));

        // ── Step 5: water vapor enhancement factor ────────────────────────────────
        // f_enh = 1.00070 + 3.7e-8 * P - 1.24e-7 * T²
        var cFenhAlpha = ctx.wrapBigDecimal(new BigDecimal("1.00070"),  descriptor("water vapor enhancement alpha"));
        var cFenhBeta  = ctx.wrapBigDecimal(new BigDecimal("3.7E-8"),   descriptor("water vapor enhancement beta"));
        var cFenhGamma = ctx.wrapBigDecimal(new BigDecimal("1.24E-7"),  descriptor("water vapor enhancement gamma"));
        var airTempSq  = airTemp.multiply(airTemp, mc, descriptor("T_air^2"));
        var fEnh = cFenhAlpha
                .add(cFenhBeta.multiply(airPressure, mc, descriptor("beta*P_air")),
                     mc, descriptor("alpha + beta*P"))
                .subtract(cFenhGamma.multiply(airTempSq, mc, descriptor("gamma*T_air^2")),
                          mc, descriptor("f_enh: water vapor enhancement factor"));

        // ── Step 6: partial pressure of water vapor ───────────────────────────────
        var pv = humidity.multiply(fEnh, mc, descriptor("h * f_enh"))
                         .multiply(svp,  mc, descriptor("pv: partial pressure of water vapor, Pa"));

        // ── Step 7: water vapor correction to refractivity ────────────────────────
        // N_v = -(3.8020 - 0.0384 * sigma^2) * pv * 1e-3   [×10^-8]
        var cEdlenW1 = ctx.wrapBigDecimal(new BigDecimal("3.8020"), descriptor("Edlen water vapor coefficient W1"));
        var cEdlenW2 = ctx.wrapBigDecimal(new BigDecimal("0.0384"), descriptor("Edlen water vapor coefficient W2"));
        var c1e3     = ctx.wrapBigDecimal(new BigDecimal("1E-3"),    descriptor("scale factor 1e-3"));
        var wvTerm   = cEdlenW1.subtract(cEdlenW2.multiply(sigmaSq, mc, descriptor("W2*sigma^2")),
                                         mc, descriptor("W1 - W2*sigma^2"));
        // Water vapor reduces n relative to dry air, so the correction is negative.
        var Nv = wvTerm.multiply(pv,   mc, descriptor("(W1-W2*sigma^2)*pv"))
                       .multiply(c1e3, mc, descriptor("water vapor refractivity magnitude [×10^-8]"))
                       .negate(mc, descriptor("N_v: water vapor refractivity correction [×10^-8]"));

        // ── Step 8: total refractivity → refractive index ─────────────────────────
        // n = 1 + (N_tp + N_v) × 1e-8
        var Ntotal   = Ntp.add(Nv, mc, descriptor("N_total = N_tp + N_v [×10^-8]"));
        var nMinusOne = Ntotal.multiply(c1e8, mc, descriptor("n-1 = N_total * 1e-8"));
        var n = one.add(nMinusOne, mc, descriptor("n: refractive index of air (Ciddor / Birch-Downs)"));

        // ══════════════════════════════════════════════════════════════════════════
        // INTERFEROMETRIC LENGTH
        // L_raw = (m + f) × lambda_air / 2
        // ══════════════════════════════════════════════════════════════════════════

        var lambdaAir3 = lambdaVac3.divide(n, mc,
                descriptor("lambda_air3: HeNe air wavelength, nm"));
        // Half-wavelength is the fundamental interference unit for a reflection setup.
        var halfLambdaAir3 = lambdaAir3.divide(two, mc,
                descriptor("lambda_air3/2: half-wavelength, nm"));

        // m: integer order from method of exact fractions. f: observed fringe fraction.
        var m = ctx.wrapBigDecimal(data.fetchIntegerFringeOrder(),
                descriptor("m: integer fringe order (method of exact fractions)"));
        var f = ctx.wrapBigDecimal(data.fetchFractionalFringeOrder(),
                descriptor("f: fractional fringe order (observed)"));

        var totalOrder = m.add(f, mc, descriptor("m+f: total fringe order"));
        var rawLength  = totalOrder.multiply(halfLambdaAir3, mc,
                descriptor("L_raw: raw interferometric length, nm"));

        // ══════════════════════════════════════════════════════════════════════════
        // THERMAL CORRECTION
        // L_cal = L_raw / (1 + alpha * (T_part - T_ref))
        // ══════════════════════════════════════════════════════════════════════════

        var alpha = ctx.wrapBigDecimal(data.fetchThermalExpansionCoefficient(),
                descriptor("alpha: thermal expansion coefficient of tungsten carbide, K^-1",
                        of("source", "White 2025, Appendix B")));
        var tPart = ctx.wrapBigDecimal(data.fetchPartTemperature(),
                descriptor("T_part: part temperature, degC"));
        var tRef  = ctx.wrapBigDecimal(data.fetchReferenceTemperature(),
                descriptor("T_ref: ISO 1 reference temperature, degC"));

        var deltaT        = tPart.subtract(tRef, mc, descriptor("deltaT: T_part - T_ref, K"));
        var thermalFactor = one.add(alpha.multiply(deltaT, mc, descriptor("alpha*deltaT")),
                                    mc, descriptor("1 + alpha*deltaT: thermal correction factor"));
        var calibratedLength = rawLength.divide(thermalFactor, mc,
                descriptor("L_cal: thermally corrected length, nm"));

        // ══════════════════════════════════════════════════════════════════════════
        // DEVIATION FROM NOMINAL
        // ══════════════════════════════════════════════════════════════════════════

        var nominalLength = ctx.wrapBigDecimal(data.fetchNominalLength(),
                descriptor("L_nom: nominal gauge block length (7 mm), nm",
                        of("source", "White 2025")));

        var deviation = calibratedLength.subtract(nominalLength, mc,
                descriptor("deltaL: length deviation from nominal, nm"));

        // Store the full CPG snapshot alongside the result (as the paper proposes).
        var snapshot = ctx.snapshot();

        // Paper (White 2025, Appendix B) reports the deviation as +2 nm
        // with expanded uncertainty U = 31 nm (k = 2).
        assertEquals(new BigDecimal("2"),
                deviation.getValue().setScale(0, RoundingMode.HALF_UP));
    }
}
