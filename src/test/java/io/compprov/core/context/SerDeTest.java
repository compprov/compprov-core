package io.compprov.core.context;

import io.compprov.core.DataContext;
import io.compprov.core.DefaultComputationContext;
import io.compprov.core.DefaultComputationEnvironment;
import io.compprov.core.Snapshot;
import io.compprov.core.meta.Descriptor;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.zip.GZIPOutputStream;

import static org.junit.jupiter.api.Assertions.*;

public class SerDeTest {

    private static final DefaultComputationEnvironment environment = DefaultComputationEnvironment.create();

    private static Snapshot sampleSnapshot() {
        final var context = new DefaultComputationContext(
                environment,
                new DataContext(Descriptor.descriptor("serde")));
        final var mc = context.wrapMathContext(new MathContext(5, RoundingMode.HALF_UP), Descriptor.descriptor("mc"));
        final var x = context.wrapBigDecimal(new BigDecimal("1.5"), Descriptor.descriptor("x"));
        final var y = context.wrapBigDecimal(new BigDecimal("2.25"), Descriptor.descriptor("y"));
        final var sum = x.add(y, mc, null);
        sum.multiply(y, mc, Descriptor.descriptor("result"));
        return context.snapshot();
    }

    private static Snapshot hydrologySnapshot() throws IOException {
        try (var is = SerDeTest.class.getResourceAsStream("/snapshots/hydrology.json")) {
            assertNotNull(is, "hydrology.json not found");
            return environment.fromJson(is.readAllBytes());
        }
    }

    private static void assertPretty(String json) {
        assertTrue(json.contains("\n"), "pretty JSON must be multi-line");
        assertTrue(json.contains("\n  \"descriptor\" : "), "pretty JSON must be indented");
    }

    private static void assertMinified(String json) {
        assertFalse(json.contains("\n"), "minified JSON must be single-line");
        assertFalse(json.contains(" : "), "minified JSON must not contain pretty separators");
        assertTrue(json.contains("\"descriptor\":"), "minified JSON must use compact separators");
    }

    // ── toJson(Snapshot) ─────────────────────────────────────────────────────

    @Test
    public void toJson_default_isPrettyPrinted() {
        assertPretty(environment.toJson(sampleSnapshot()));
    }

    @Test
    public void toJson_default_roundTrips() {
        final var snapshot = sampleSnapshot();
        assertEquals(snapshot, environment.fromJson(environment.toJson(snapshot)));
    }

    // ── toJson(Snapshot, OutputStream) ───────────────────────────────────────

    @Test
    public void toJsonStream_default_isPrettyPrinted() {
        final var os = new ByteArrayOutputStream();
        environment.toJson(sampleSnapshot(), os);
        assertPretty(os.toString(StandardCharsets.UTF_8));
    }

    @Test
    public void toJsonStream_default_matchesStringVariant() {
        final var snapshot = sampleSnapshot();
        final var os = new ByteArrayOutputStream();
        environment.toJson(snapshot, os);
        assertEquals(environment.toJson(snapshot), os.toString(StandardCharsets.UTF_8));
    }

    @Test
    public void toJsonStream_default_roundTripsViaBytes() {
        final var snapshot = sampleSnapshot();
        final var os = new ByteArrayOutputStream();
        environment.toJson(snapshot, os);
        assertEquals(snapshot, environment.fromJson(os.toByteArray()));
    }

    // ── toJson(Snapshot, boolean) ────────────────────────────────────────────

    @Test
    public void toJson_minifyFalse_isPrettyAndEqualsDefault() {
        final var snapshot = sampleSnapshot();
        final var json = environment.toJson(snapshot, false);
        assertPretty(json);
        assertEquals(environment.toJson(snapshot), json);
    }

    @Test
    public void toJson_minifyTrue_isMinified() {
        assertMinified(environment.toJson(sampleSnapshot(), true));
    }

    @Test
    public void toJson_minifyTrue_roundTrips() {
        final var snapshot = sampleSnapshot();
        assertEquals(snapshot, environment.fromJson(environment.toJson(snapshot, true)));
    }

    @Test
    public void toJson_minified_isShorterThanPretty() {
        final var snapshot = sampleSnapshot();
        assertTrue(environment.toJson(snapshot, false).length() > environment.toJson(snapshot, true).length());
    }

    // ── toJson(Snapshot, OutputStream, boolean) ──────────────────────────────

    @Test
    public void toJsonStream_minifyFalse_isPretty() {
        final var snapshot = sampleSnapshot();
        final var os = new ByteArrayOutputStream();
        environment.toJson(snapshot, os, false);
        assertPretty(os.toString(StandardCharsets.UTF_8));
        assertEquals(environment.toJson(snapshot, false), os.toString(StandardCharsets.UTF_8));
    }

    @Test
    public void toJsonStream_minifyTrue_isMinified() {
        final var snapshot = sampleSnapshot();
        final var os = new ByteArrayOutputStream();
        environment.toJson(snapshot, os, true);
        assertMinified(os.toString(StandardCharsets.UTF_8));
        assertEquals(environment.toJson(snapshot, true), os.toString(StandardCharsets.UTF_8));
    }

    @Test
    public void toJsonStream_minifyTrue_roundTrips() {
        final var snapshot = sampleSnapshot();
        final var os = new ByteArrayOutputStream();
        environment.toJson(snapshot, os, true);
        assertEquals(snapshot, environment.fromJson(os.toByteArray()));
    }

    // ── fromJson ─────────────────────────────────────────────────────────────

    @Test
    public void fromJson_stringAndBytes_produceEqualSnapshots() {
        final var json = environment.toJson(sampleSnapshot());
        assertEquals(environment.fromJson(json), environment.fromJson(json.getBytes(StandardCharsets.UTF_8)));
    }

    @Test
    public void fromJson_stream_matchesBytes() {
        final var bytes = environment.toJson(sampleSnapshot()).getBytes(StandardCharsets.UTF_8);
        assertEquals(environment.fromJson(bytes), environment.fromJson(new ByteArrayInputStream(bytes)));
    }

    @Test
    public void fromJson_invalidString_throwsIllegalArgument() {
        assertThrows(IllegalArgumentException.class, () -> environment.fromJson("{not json"));
    }

    @Test
    public void fromJson_invalidBytes_throwsIllegalArgument() {
        assertThrows(IllegalArgumentException.class,
                () -> environment.fromJson("{not json".getBytes(StandardCharsets.UTF_8)));
    }

    @Test
    public void fromJson_invalidStream_throwsIllegalArgument() {
        assertThrows(IllegalArgumentException.class,
                () -> environment.fromJson(new ByteArrayInputStream("{not json".getBytes(StandardCharsets.UTF_8))));
    }

    // ── toGzip / fromGzip ──────────────────────────────────────────────────────

    @Test
    public void toGzip_producesGzipData() {
        final var zip = environment.toGzip(sampleSnapshot());
        assertTrue(zip.length > 2);
        // GZIP magic header 0x1f 0x8b
        assertEquals((byte) 0x1f, zip[0]);
        assertEquals((byte) 0x8b, zip[1]);
    }

    @Test
    public void toGzip_fromGzip_roundTrips() {
        final var snapshot = sampleSnapshot();
        assertEquals(snapshot, environment.fromGzip(environment.toGzip(snapshot)));
    }

    @Test
    public void toGzip_fromGzipStream_roundTrips() {
        final var snapshot = sampleSnapshot();
        assertEquals(snapshot, environment.fromGzip(new ByteArrayInputStream(environment.toGzip(snapshot))));
    }

    @Test
    public void fromGzip_invalidData_throwsIllegalArgument() {
        assertThrows(IllegalArgumentException.class,
                () -> environment.fromGzip("not gzip".getBytes(StandardCharsets.UTF_8)));
    }

    @Test
    public void fromGzip_invalidStream_throwsIllegalArgument() {
        assertThrows(IllegalArgumentException.class,
                () -> environment.fromGzip(new ByteArrayInputStream("not gzip".getBytes(StandardCharsets.UTF_8))));
    }

    @Test
    public void fromGzip_validGzipWithInvalidJson_throwsIllegalArgument() throws IOException {
        final var baos = new ByteArrayOutputStream();
        try (var gzip = new GZIPOutputStream(baos)) {
            gzip.write("{not json".getBytes(StandardCharsets.UTF_8));
        }
        assertThrows(IllegalArgumentException.class, () -> environment.fromGzip(baos.toByteArray()));
    }

    // ── hydrology.json ───────────────────────────────────────────────────────

    @Test
    public void hydrology_roundTripsInAllFormats() throws IOException {
        final var snapshot = hydrologySnapshot();
        assertEquals(snapshot, environment.fromJson(environment.toJson(snapshot)));
        assertEquals(snapshot, environment.fromJson(environment.toJson(snapshot, true)));
        assertEquals(snapshot, environment.fromGzip(environment.toGzip(snapshot)));
    }

    @Test
    public void hydrology_sizes_pretty_gt_minified_gt_zipped() throws IOException {
        final var snapshot = hydrologySnapshot();

        final var pretty = environment.toJson(snapshot, false);
        final var minified = environment.toJson(snapshot, true);
        final var zipped = environment.toGzip(snapshot);

        final int prettySize = pretty.getBytes(StandardCharsets.UTF_8).length;
        final int minifiedSize = minified.getBytes(StandardCharsets.UTF_8).length;
        final int zippedSize = zipped.length;

        System.out.println("=== hydrology.json: non-minified (" + prettySize + " bytes) ===");
        System.out.println(pretty);
        System.out.println("=== hydrology.json: minified (" + minifiedSize + " bytes) ===");
        System.out.println(minified);
        // binary gzip output is printed as Base64 so it stays readable in the console
        System.out.println("=== hydrology.json: zipped (" + zippedSize + " bytes, Base64) ===");
        System.out.println(Base64.getEncoder().encodeToString(zipped));

        assertTrue(prettySize > minifiedSize,
                "non-minified (" + prettySize + ") must be greater than minified (" + minifiedSize + ")");
        assertTrue(minifiedSize > zippedSize,
                "minified (" + minifiedSize + ") must be greater than zipped (" + zippedSize + ")");
    }
}
