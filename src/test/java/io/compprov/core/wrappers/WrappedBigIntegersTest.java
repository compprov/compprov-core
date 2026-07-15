package io.compprov.core.wrappers;

import io.compprov.core.DataContext;
import io.compprov.core.DefaultComputationContext;
import io.compprov.core.DefaultComputationEnvironment;
import io.compprov.core.meta.Descriptor;
import io.compprov.core.wrappers.primitive.WrappedInteger;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigInteger;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for {@link WrappedBigIntegers} — a tracked {@code BigInteger[]}, used to pack/unpack
 * multiple values through a single subgraph argument or result slot (see
 * {@link WrappedBigInteger#array} and {@code AtmosphericDragDescentStress} for the same
 * graph-folding pattern applied to {@code BigDecimal}).
 */
public class WrappedBigIntegersTest {

    private static final DefaultComputationEnvironment environment = DefaultComputationEnvironment.create();
    private DefaultComputationContext ctx;

    @BeforeEach
    void setUp() {
        ctx = new DefaultComputationContext(
                environment,
                new DataContext(Descriptor.descriptor("test")));
    }

    private WrappedBigIntegers wrap(BigInteger[] values, String name) {
        return ctx.wrapBigIntegers(values, Descriptor.descriptor(name));
    }

    private WrappedInteger index(int i) {
        return ctx.wrapInteger(i, Descriptor.descriptor("index_" + i));
    }

    private void assertValue(WrappedBigInteger result, long expected) {
        assertEquals(BigInteger.valueOf(expected), result.getValue());
    }

    @Test
    void extract_first_element() {
        var arr = wrap(new BigInteger[]{BigInteger.valueOf(10), BigInteger.valueOf(20)}, "arr");
        var result = arr.extract(index(0), null);

        assertValue(result, 10);
    }

    @Test
    void extract_last_element() {
        var arr = wrap(new BigInteger[]{BigInteger.valueOf(10), BigInteger.valueOf(20)}, "arr");
        var result = arr.extract(index(1), null);

        assertValue(result, 20);
    }

    @Test
    void extract_tracks_operation_and_variables() {
        var arr = wrap(new BigInteger[]{BigInteger.ONE, BigInteger.TEN}, "arr");
        arr.extract(index(0), null);

        assertEquals(1, ctx.snapshot().operations().size());
        // arr, index, result
        assertEquals(3, ctx.snapshot().variables().size());
    }

    @Test
    void extract_with_descriptor() {
        var arr = wrap(new BigInteger[]{BigInteger.ONE, BigInteger.TEN}, "arr");
        var result = arr.extract(index(1), Descriptor.descriptor("second"));

        assertEquals("second", result.getVariableTrack().getDescriptor().getName());
    }

    @Test
    void extract_out_of_bounds_throws() {
        var arr = wrap(new BigInteger[]{BigInteger.ONE}, "arr");
        assertThrows(ArrayIndexOutOfBoundsException.class, () -> arr.extract(index(5), null));
    }

    @Test
    void extract_null_index_throws_npe() {
        var arr = wrap(new BigInteger[]{BigInteger.ONE}, "arr");
        assertThrows(NullPointerException.class, () -> arr.extract(null, null));
    }

    // ── array() — packing scalars into a WrappedBigIntegers ─────────────────────

    @Test
    void array_builds_array_from_scalars() {
        var a = ctx.wrapBigInteger(BigInteger.valueOf(1), Descriptor.descriptor("a"));
        var b = ctx.wrapBigInteger(BigInteger.valueOf(2), Descriptor.descriptor("b"));
        var c = ctx.wrapBigInteger(BigInteger.valueOf(3), Descriptor.descriptor("c"));

        var result = a.array(List.of(b, c), null);

        assertEquals(3, result.getValue().length);
        assertEquals(BigInteger.valueOf(1), result.getValue()[0]);
        assertEquals(BigInteger.valueOf(2), result.getValue()[1]);
        assertEquals(BigInteger.valueOf(3), result.getValue()[2]);
    }

    @Test
    void array_then_extract_round_trip() {
        // Pack two scalars into one array and pull a value back out -- the pattern used to
        // return multiple values from a single folded-subgraph invocation.
        var a = ctx.wrapBigInteger(BigInteger.valueOf(100), Descriptor.descriptor("a"));
        var b = ctx.wrapBigInteger(BigInteger.valueOf(200), Descriptor.descriptor("b"));
        var packed = a.array(List.of(b), null);

        var unpacked = packed.extract(index(1), null);

        assertValue(unpacked, 200);
    }

    @Test
    void array_null_throws_npe() {
        var a = ctx.wrapBigInteger(BigInteger.ONE, Descriptor.descriptor("a"));
        assertThrows(NullPointerException.class, () -> a.array(null, null));
    }

    // ── JSON serialization / deserialization via snapshot ───────────────────────

    @Test
    void json_output_contains_array_value() {
        var arr = wrap(new BigInteger[]{BigInteger.ONE, BigInteger.TEN}, "arr");
        arr.extract(index(0), null);

        String json = ctx.getEnvironment().toJson(ctx.snapshot());
        assertTrue(json.contains("\"variables\" : ["), "variables should be a JSON array:\n" + json);
        assertTrue(json.contains("\"operations\" : ["), "operations should be a JSON array:\n" + json);
    }

    @Test
    void snapshot_round_trip_reproduces_extracted_value() {
        var arr = wrap(new BigInteger[]{BigInteger.valueOf(314), BigInteger.valueOf(271)}, "arr");
        var result = arr.extract(index(0), null);

        String json = environment.toJson(ctx.snapshot());
        var snapshot = environment.fromJson(json);
        var reproduced = environment.compute(snapshot);

        var reproducedResult = reproduced.getVariable(result.getVariableTrack().getId()).getValue();
        assertEquals(result.getValue(), reproducedResult);
    }

    @Test
    void snapshot_round_trip_preserves_array_elements() {
        var arr = wrap(new BigInteger[]{BigInteger.valueOf(314), BigInteger.valueOf(271)}, "arr");

        String json = environment.toJson(ctx.snapshot());
        var snapshot = environment.fromJson(json);
        var reproduced = environment.compute(snapshot);

        var reproducedArray = (BigInteger[]) reproduced.getVariable(arr.getVariableTrack().getId()).getValue();
        assertEquals(2, reproducedArray.length);
        assertEquals(BigInteger.valueOf(314), reproducedArray[0]);
        assertEquals(BigInteger.valueOf(271), reproducedArray[1]);
    }

    @Test
    void snapshot_round_trip_preserves_packed_array_from_array_op() {
        var a = ctx.wrapBigInteger(BigInteger.valueOf(5), Descriptor.descriptor("a"));
        var b = ctx.wrapBigInteger(BigInteger.valueOf(6), Descriptor.descriptor("b"));
        var packed = a.array(List.of(b), null);

        String json = environment.toJson(ctx.snapshot());
        var snapshot = environment.fromJson(json);
        var reproduced = environment.compute(snapshot);

        var reproducedArray = (BigInteger[]) reproduced.getVariable(packed.getVariableTrack().getId()).getValue();
        assertEquals(2, reproducedArray.length);
        assertEquals(BigInteger.valueOf(5), reproducedArray[0]);
        assertEquals(BigInteger.valueOf(6), reproducedArray[1]);
    }
}
