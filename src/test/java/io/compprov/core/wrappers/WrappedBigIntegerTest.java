package io.compprov.core.wrappers;

import io.compprov.core.DataContext;
import io.compprov.core.DefaultComputationContext;
import io.compprov.core.DefaultComputationEnvironment;
import io.compprov.core.meta.Descriptor;
import io.compprov.core.wrappers.primitive.WrappedInteger;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigInteger;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for {@link WrappedBigInteger} — verifies that every operation
 * produces the correct numerical result and is recorded in the CPG.
 */
public class WrappedBigIntegerTest {

    private DefaultComputationContext ctx;

    @BeforeEach
    void setUp() {
        ctx = new DefaultComputationContext(
                new DefaultComputationEnvironment(),
                new DataContext(Descriptor.descriptor("test")));
    }

    private WrappedBigInteger wrap(long value, String name) {
        return ctx.wrapBigInteger(BigInteger.valueOf(value), Descriptor.descriptor(name));
    }

    private WrappedBigInteger wrap(String value, String name) {
        return ctx.wrapBigInteger(new BigInteger(value), Descriptor.descriptor(name));
    }

    private WrappedInteger wrapInt(int value) {
        return ctx.wrapInteger(value, Descriptor.descriptor("n"));
    }

    private void assertValue(WrappedBigInteger result, long expected) {
        assertEquals(BigInteger.valueOf(expected), result.getValue());
    }

    private void assertOperationCount(int expected) {
        assertEquals(expected, ctx.snapshot().operations().size());
    }

    private void assertVariableCount(int expected) {
        assertEquals(expected, ctx.snapshot().variables().size());
    }

    @Test
    void add_basic() {
        var a = wrap(3, "a");
        var b = wrap(4, "b");
        var result = a.add(b, null);

        assertValue(result, 7);
        assertOperationCount(1);
        assertVariableCount(3);
    }

    @Test
    void add_with_descriptor() {
        var a = wrap(100, "a");
        var b = wrap(200, "b");
        var result = a.add(b, Descriptor.descriptor("sum"));

        assertValue(result, 300);
        assertEquals("sum", result.getVariableTrack().getDescriptor().getName());
    }

    @Test
    void subtract_basic() {
        var a = wrap(10, "a");
        var b = wrap(3, "b");
        var result = a.subtract(b, null);

        assertValue(result, 7);
    }

    @Test
    void subtract_negative_result() {
        var a = wrap(3, "a");
        var b = wrap(10, "b");
        var result = a.subtract(b, null);

        assertValue(result, -7);
    }

    @Test
    void multiply_basic() {
        var a = wrap(6, "a");
        var b = wrap(7, "b");
        var result = a.multiply(b, null);

        assertValue(result, 42);
    }

    @Test
    void divide_exact() {
        var a = wrap(20, "a");
        var b = wrap(4, "b");
        var result = a.divide(b, null);

        assertValue(result, 5);
    }

    @Test
    void divide_truncates() {
        var a = wrap(17, "a");
        var b = wrap(5, "b");
        var result = a.divide(b, null);

        // Integer division: 17 / 5 = 3 (truncated toward zero)
        assertValue(result, 3);
    }

    @Test
    void remainder_basic() {
        var a = wrap(17, "a");
        var b = wrap(5, "b");
        var result = a.remainder(b, null);

        assertValue(result, 2);
    }

    @Test
    void gcd_basic() {
        var a = wrap(48, "a");
        var b = wrap(18, "b");
        var result = a.gcd(b, null);

        assertValue(result, 6);
    }

    @Test
    void gcd_coprime() {
        var a = wrap(7, "a");
        var b = wrap(13, "b");
        var result = a.gcd(b, null);

        assertValue(result, 1);
    }

    @Test
    void mod_basic() {
        var a = wrap(17, "a");
        var m = wrap(5, "m");
        var result = a.mod(m, null);

        assertValue(result, 2);
    }

    @Test
    void mod_negative_base() {
        // BigInteger.mod always returns non-negative result
        var a = wrap(-7, "a");
        var m = wrap(5, "m");
        var result = a.mod(m, null);

        assertValue(result, 3);
    }

    @Test
    void mod_pow_basic() {
        // 2^10 mod 1000 = 24
        var base = wrap(2, "base");
        var exponent = wrap(10, "exponent");
        var modulus = wrap(1000, "modulus");
        var result = base.modPow(exponent, modulus, null);

        assertValue(result, 24);
        assertVariableCount(4);  // base, exponent, modulus, result
        assertOperationCount(1);

        // All three inputs should be tracked
        var op = ctx.snapshot().operations().get(0);
        assertEquals(3, op.arguments().size());
    }

    @Test
    void mod_inverse_basic() {
        // 3^(-1) mod 7 = 5, because 3 * 5 = 15 ≡ 1 (mod 7)
        var a = wrap(3, "a");
        var m = wrap(7, "m");
        var result = a.modInverse(m, null);

        assertValue(result, 5);
    }

    @Test
    void pow_basic() {
        var a = wrap(2, "a");
        var result = a.pow(wrapInt(10), null);

        assertValue(result, 1024);
    }

    @Test
    void pow_zero() {
        var a = wrap(99, "a");
        var result = a.pow(wrapInt(0), null);

        assertValue(result, 1);
    }

    @Test
    void abs_negative() {
        var a = wrap(-42, "a");
        var result = a.abs(null);

        assertValue(result, 42);
    }

    @Test
    void abs_positive_unchanged() {
        var a = wrap(42, "a");
        var result = a.abs(null);

        assertValue(result, 42);
    }

    @Test
    void negate_positive() {
        var a = wrap(5, "a");
        var result = a.negate(null);

        assertValue(result, -5);
    }

    @Test
    void negate_negative() {
        var a = wrap(-5, "a");
        var result = a.negate(null);

        assertValue(result, 5);
    }

    @Test
    void not_basic() {
        var a = wrap(5, "a");  // ...0101
        var result = a.not(null); // ...1010 = -6 in two's complement

        assertEquals(BigInteger.valueOf(5).not(), result.getValue());
    }

    @Test
    void and_basic() {
        var a = wrap(0b1100, "a"); // 12
        var b = wrap(0b1010, "b"); // 10
        var result = a.and(b, null);    // 0b1000 = 8

        assertValue(result, 8);
    }

    @Test
    void or_basic() {
        var a = wrap(0b1100, "a"); // 12
        var b = wrap(0b1010, "b"); // 10
        var result = a.or(b, null);     // 0b1110 = 14

        assertValue(result, 14);
    }

    @Test
    void xor_basic() {
        var a = wrap(0b1100, "a"); // 12
        var b = wrap(0b1010, "b"); // 10
        var result = a.xor(b, null);    // 0b0110 = 6

        assertValue(result, 6);
    }

    @Test
    void and_not_basic() {
        var a = wrap(0b1100, "a"); // 12
        var b = wrap(0b1010, "b"); // 10
        var result = a.andNot(b, null); // a & ~b = 0b0100 = 4

        assertValue(result, 4);
    }

    @Test
    void shift_left() {
        var a = wrap(1, "a");
        var result = a.shiftLeft(wrapInt(8), null);

        assertValue(result, 256);
    }

    @Test
    void shift_right() {
        var a = wrap(256, "a");
        var result = a.shiftRight(wrapInt(3), null);

        assertValue(result, 32);
    }

    @Test
    void set_bit() {
        var a = wrap(0b1010, "a"); // 10
        var result = a.setBit(wrapInt(0), null);  // 0b1011 = 11

        assertValue(result, 11);
    }

    @Test
    void clear_bit() {
        var a = wrap(0b1111, "a"); // 15
        var result = a.clearBit(wrapInt(1), null); // 0b1101 = 13

        assertValue(result, 13);
    }

    @Test
    void flip_bit() {
        var a = wrap(0b1010, "a"); // 10
        var result = a.flipBit(wrapInt(0), null); // 0b1011 = 11

        assertValue(result, 11);
    }

    @Test
    void sqrt_perfect_square() {
        var a = wrap(144, "a");
        var result = a.sqrt(null);

        assertValue(result, 12);
    }

    @Test
    void sqrt_truncates() {
        var a = wrap(10, "a");
        var result = a.sqrt(null);

        // floor(sqrt(10)) = 3
        assertValue(result, 3);
    }

    @Test
    void max_basic() {
        var a = wrap(7, "a");
        var b = wrap(3, "b");
        var result = a.max(b, null);

        assertValue(result, 7);
    }

    @Test
    void min_basic() {
        var a = wrap(7, "a");
        var b = wrap(3, "b");
        var result = a.min(b, null);

        assertValue(result, 3);
    }

    @Test
    void chain_of_operations_builds_correct_graph() {
        // Compute (a + b) * gcd(a, b)
        var a = wrap(12, "a");
        var b = wrap(8, "b");
        var sum = a.add(b, null);        // 20
        var g = a.gcd(b, null);        // 4
        var prod = sum.multiply(g, Descriptor.descriptor("result")); // 80

        assertValue(prod, 80);
        assertVariableCount(5);  // a, b, sum, g, prod
        assertOperationCount(3); // add, gcd, multiply
        assertEquals("result", prod.getVariableTrack().getDescriptor().getName());
    }

    @Test
    void variables_map_keyed_by_uuid() {
        var a = wrap(1, "a");
        var b = wrap(2, "b");

        var record = ctx.snapshot();
        assertTrue(record.variables().stream().map(v -> v.track().getId())
                .filter(id -> id.equals(a.getVariableTrack().getId())).findFirst().isPresent());
        assertTrue(record.variables().stream().map(v -> v.track().getId())
                .filter(id -> id.equals(b.getVariableTrack().getId())).findFirst().isPresent());
    }

    @Test
    void null_val_throws_npe() {
        var a = wrap(1, "a");
        assertThrows(NullPointerException.class, () -> a.add(null, null));
        assertThrows(NullPointerException.class, () -> a.multiply(null, null));
        assertThrows(NullPointerException.class, () -> a.gcd(null, null));
    }

    @Test
    void json_output_is_valid() {
        var a = wrap(10, "a");
        var b = wrap(3, "b");
        a.add(b, null);

        String json = ctx.getEnvironment().toJson(ctx.snapshot());
        assertNotNull(json);
        assertTrue(json.contains("\"variables\" : ["));
        assertTrue(json.contains("\"operations\" : ["));
        assertTrue(json.contains("\"arguments\""));
        assertTrue(json.contains("\"resultId\""));
    }
}
