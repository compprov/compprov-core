package io.compprov.core.wrappers;

import io.compprov.core.DataContext;
import io.compprov.core.DefaultComputationContext;
import io.compprov.core.DefaultComputationEnvironment;
import io.compprov.core.meta.Descriptor;
import io.compprov.core.wrappers.primitive.WrappedInteger;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for {@link WrappedBigDecimals} — a tracked {@code BigDecimal[]}, used to pack/unpack
 * multiple values through a single subgraph argument or result slot (see
 * {@link WrappedBigDecimal#array} and {@code AtmosphericDragDescentStress} for the graph-folding
 * pattern this supports).
 */
public class WrappedBigDecimalsTest {

    private static final DefaultComputationEnvironment environment = DefaultComputationEnvironment.create();
    private DefaultComputationContext ctx;

    @BeforeEach
    void setUp() {
        ctx = new DefaultComputationContext(
                environment,
                new DataContext(Descriptor.descriptor("test")));
    }

    private WrappedBigDecimals wrap(BigDecimal[] values, String name) {
        return ctx.wrapBigDecimals(values, Descriptor.descriptor(name));
    }

    private WrappedInteger index(int i) {
        return ctx.wrapInteger(i, Descriptor.descriptor("index_" + i));
    }

    private void assertValue(WrappedBigDecimal result, String expected) {
        assertEquals(0, new BigDecimal(expected).compareTo(result.getValue()),
                "Expected value %s but got %s".formatted(expected, result.getValue()));
    }

    @Test
    void extract_first_element() {
        var arr = wrap(new BigDecimal[]{new BigDecimal("1.5"), new BigDecimal("2.5")}, "arr");
        var result = arr.extract(index(0), null);

        assertValue(result, "1.5");
    }

    @Test
    void extract_last_element() {
        var arr = wrap(new BigDecimal[]{new BigDecimal("1.5"), new BigDecimal("2.5")}, "arr");
        var result = arr.extract(index(1), null);

        assertValue(result, "2.5");
    }

    @Test
    void extract_tracks_operation_and_variables() {
        var arr = wrap(new BigDecimal[]{BigDecimal.ONE, BigDecimal.TEN}, "arr");
        arr.extract(index(0), null);

        assertEquals(1, ctx.snapshot().operations().size());
        // arr, index, result
        assertEquals(3, ctx.snapshot().variables().size());
    }

    @Test
    void extract_with_descriptor() {
        var arr = wrap(new BigDecimal[]{BigDecimal.ONE, BigDecimal.TEN}, "arr");
        var result = arr.extract(index(1), Descriptor.descriptor("second"));

        assertEquals("second", result.getVariableTrack().getDescriptor().getName());
    }

    @Test
    void extract_out_of_bounds_throws() {
        var arr = wrap(new BigDecimal[]{BigDecimal.ONE}, "arr");
        assertThrows(ArrayIndexOutOfBoundsException.class, () -> arr.extract(index(5), null));
    }

    @Test
    void extract_null_index_throws_npe() {
        var arr = wrap(new BigDecimal[]{BigDecimal.ONE}, "arr");
        assertThrows(NullPointerException.class, () -> arr.extract(null, null));
    }

    // ── array() — packing scalars into a WrappedBigDecimals ─────────────────────

    @Test
    void array_builds_array_from_scalars() {
        var a = ctx.wrapBigDecimal(new BigDecimal("1"), Descriptor.descriptor("a"));
        var b = ctx.wrapBigDecimal(new BigDecimal("2"), Descriptor.descriptor("b"));
        var c = ctx.wrapBigDecimal(new BigDecimal("3"), Descriptor.descriptor("c"));

        var result = a.array(List.of(b, c), null);

        assertEquals(3, result.getValue().length);
        assertEquals(0, new BigDecimal("1").compareTo(result.getValue()[0]));
        assertEquals(0, new BigDecimal("2").compareTo(result.getValue()[1]));
        assertEquals(0, new BigDecimal("3").compareTo(result.getValue()[2]));
    }

    @Test
    void array_then_extract_round_trip() {
        // Pack two scalars into one array and pull a value back out -- the pattern used to
        // return multiple values from a single folded-subgraph invocation.
        var a = ctx.wrapBigDecimal(new BigDecimal("10"), Descriptor.descriptor("a"));
        var b = ctx.wrapBigDecimal(new BigDecimal("20"), Descriptor.descriptor("b"));
        var packed = a.array(List.of(b), null);

        var unpacked = packed.extract(index(1), null);

        assertValue(unpacked, "20");
    }

    @Test
    void array_null_throws_npe() {
        var a = ctx.wrapBigDecimal(BigDecimal.ONE, Descriptor.descriptor("a"));
        assertThrows(NullPointerException.class, () -> a.array(null, null));
    }

    // ── JSON serialization / deserialization via snapshot ───────────────────────

    @Test
    void json_output_contains_array_value() {
        var arr = wrap(new BigDecimal[]{BigDecimal.ONE, BigDecimal.TEN}, "arr");
        arr.extract(index(0), null);

        String json = ctx.getEnvironment().toJson(ctx.snapshot());
        assertTrue(json.contains("\"variables\" : ["), "variables should be a JSON array:\n" + json);
        assertTrue(json.contains("\"operations\" : ["), "operations should be a JSON array:\n" + json);
    }

    @Test
    void snapshot_round_trip_reproduces_extracted_value() {
        var arr = wrap(new BigDecimal[]{new BigDecimal("3.14"), new BigDecimal("2.71")}, "arr");
        var result = arr.extract(index(0), null);

        String json = environment.toJson(ctx.snapshot());
        var snapshot = environment.fromJson(json);
        var reproduced = environment.compute(snapshot);

        var reproducedResult = (BigDecimal) reproduced.getVariable(result.getVariableTrack().getId()).getValue();
        assertEquals(0, result.getValue().compareTo(reproducedResult));
    }

    @Test
    void snapshot_round_trip_preserves_array_elements() {
        var arr = wrap(new BigDecimal[]{new BigDecimal("3.14"), new BigDecimal("2.71")}, "arr");

        String json = environment.toJson(ctx.snapshot());
        var snapshot = environment.fromJson(json);
        var reproduced = environment.compute(snapshot);

        var reproducedArray = (BigDecimal[]) reproduced.getVariable(arr.getVariableTrack().getId()).getValue();
        assertEquals(2, reproducedArray.length);
        assertEquals(0, new BigDecimal("3.14").compareTo(reproducedArray[0]));
        assertEquals(0, new BigDecimal("2.71").compareTo(reproducedArray[1]));
    }

    @Test
    void snapshot_round_trip_preserves_packed_array_from_array_op() {
        var a = ctx.wrapBigDecimal(new BigDecimal("5"), Descriptor.descriptor("a"));
        var b = ctx.wrapBigDecimal(new BigDecimal("6"), Descriptor.descriptor("b"));
        var packed = a.array(List.of(b), null);

        String json = environment.toJson(ctx.snapshot());
        var snapshot = environment.fromJson(json);
        var reproduced = environment.compute(snapshot);

        var reproducedArray = (BigDecimal[]) reproduced.getVariable(packed.getVariableTrack().getId()).getValue();
        assertEquals(2, reproducedArray.length);
        assertEquals(0, new BigDecimal("5").compareTo(reproducedArray[0]));
        assertEquals(0, new BigDecimal("6").compareTo(reproducedArray[1]));
    }
}
