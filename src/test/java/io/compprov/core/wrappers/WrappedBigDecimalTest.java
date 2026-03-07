package io.compprov.core.wrappers;

import io.compprov.core.DataContext;
import io.compprov.core.DefaultComputationContext;
import io.compprov.core.DefaultComputationEnvironment;
import io.compprov.core.meta.Descriptor;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests for {@link WrappedBigDecimal} — verifies that every operation
 * produces the correct numerical result and is recorded in the CPG.
 */
public class WrappedBigDecimalTest {

    private DefaultComputationContext ctx;

    @BeforeEach
    void setUp() {
        ctx = new DefaultComputationContext(
                new DefaultComputationEnvironment(),
                new DataContext(Descriptor.descriptor("test")));
    }

    private WrappedBigDecimal wrap(String value, String name) {
        return ctx.wrapBigDecimal(new BigDecimal(value), Descriptor.descriptor(name));
    }

    private WrappedMathContext mc(int precision, RoundingMode rm) {
        return ctx.wrapMathContext(new MathContext(precision, rm), Descriptor.descriptor("mc"));
    }

    private WrappedMathContext mc(int precision) {
        return mc(precision, RoundingMode.HALF_UP);
    }

    private void assertValue(WrappedBigDecimal result, String expected) {
        assertEquals(0, new BigDecimal(expected).compareTo(result.getValue()),
                "Expected value %s but got %s".formatted(expected, result.getValue()));
    }

    private void assertOperationCount(int expected) {
        assertEquals(expected, ctx.snapshot().operations().size());
    }

    private void assertVariableCount(int expected) {
        assertEquals(expected, ctx.snapshot().variables().size());
    }

    @Test
    void add_with_math_context() {
        var a = wrap("1.23456", "a");
        var b = wrap("2.34567", "b");
        var result = a.add(b, mc(4), null);

        assertEquals(4, result.getValue().precision());
        assertOperationCount(1);
    }

    @Test
    void subtract_with_math_context() {
        var a = wrap("5.678", "a");
        var b = wrap("1.234", "b");
        var result = a.subtract(b, mc(3), null);

        assertEquals(3, result.getValue().precision());
    }

    @Test
    void multiply_with_math_context() {
        var a = wrap("1.23456789", "a");
        var b = wrap("9.87654321", "b");
        var result = a.multiply(b, mc(6), null);

        assertEquals(6, result.getValue().precision());
    }

    @Test
    void divide_with_math_context() {
        var a = wrap("22", "a");
        var b = wrap("7", "b");
        var result = a.divide(b, mc(5), null);

        assertEquals(5, result.getValue().precision());
    }

    @Test
    void pow_with_math_context() {
        var a = wrap("2", "a");
        var n = ctx.wrapInteger(10, Descriptor.descriptor("n"));
        var result = a.pow(n, mc(4), null);

        assertEquals(4, result.getValue().precision());
    }

    @Test
    void sqrt_basic() {
        var a = wrap("9", "a");
        var result = a.sqrt(mc(MathContext.DECIMAL64.getPrecision(), MathContext.DECIMAL64.getRoundingMode()), null);

        // sqrt(9) == 3 within DECIMAL64 precision
        assertEquals(0, new BigDecimal("3").compareTo(result.getValue().round(new MathContext(1))));
    }

    @Test
    void round_basic() {
        var a = wrap("3.14159", "a");
        var result = a.round(mc(3), null);

        assertValue(result, "3.14");
    }

    @Test
    void set_scale() {
        var a = wrap("3.14159", "a");
        var result = a.setScale(mc(2, RoundingMode.HALF_UP), null);

        assertEquals(2, result.getValue().scale());
        assertValue(result, "3.14");
    }

    @Test
    void scale_by_power_of_ten() {
        var a = wrap("1.23", "a");
        var n = ctx.wrapInteger(2, Descriptor.descriptor("n"));
        var result = a.scaleByPowerOfTen(n, null);

        assertValue(result, "123");
    }

    @Test
    void strip_trailing_zeros() {
        var a = wrap("3.14000", "a");
        var result = a.stripTrailingZeros(null);

        assertEquals(0, new BigDecimal("3.14").compareTo(result.getValue()));
        // scale should be reduced
        assertTrue(result.getValue().scale() < a.getValue().scale());
    }

    @Test
    void ulp_basic() {
        var a = wrap("1.00", "a");
        var result = a.ulp(null);

        // ulp of 1.00 (scale 2) is 0.01
        assertValue(result, "0.01");
    }

    @Test
    void move_point_left() {
        var a = wrap("12345", "a");
        var n = ctx.wrapInteger(2, Descriptor.descriptor("n"));
        var result = a.movePointLeft(n, null);

        assertValue(result, "123.45");
    }

    @Test
    void abs_of_negative() {
        var a = wrap("-3.5", "a");
        var result = a.abs(mc(2), null);

        assertValue(result, "3.5");
    }

    @Test
    void negate_basic() {
        var a = wrap("4.2", "a");
        var result = a.negate(mc(2), null);

        assertValue(result, "-4.2");
    }

    @Test
    void plus_applies_math_context() {
        var a = wrap("3.14159", "a");
        var result = a.plus(mc(3), null);

        assertEquals(3, result.getValue().precision());
    }

    @Test
    void divide_to_integral_value() {
        var a = wrap("22", "a");
        var b = wrap("7", "b");
        var result = a.divideToIntegralValue(b, mc(5), null);

        assertValue(result, "3");
    }

    @Test
    void remainder_basic() {
        var a = wrap("17", "a");
        var b = wrap("5", "b");
        var result = a.remainder(b, mc(5), null);

        assertValue(result, "2");
    }

    @Test
    void max_returns_larger() {
        var a = wrap("7", "a");
        var b = wrap("3", "b");
        var result = a.max(b, null);

        assertValue(result, "7");
    }

    @Test
    void min_returns_smaller() {
        var a = wrap("7", "a");
        var b = wrap("3", "b");
        var result = a.min(b, null);

        assertValue(result, "3");
    }

    @Test
    void move_point_right() {
        var a = wrap("1.23", "a");
        var n = ctx.wrapInteger(2, Descriptor.descriptor("n"));
        var result = a.movePointRight(n, null);

        assertValue(result, "123");
    }

    @Test
    void chain_of_operations_produces_correct_graph() {
        // (a + b) * (a - b) = a^2 - b^2
        var mathContext = mc(2, RoundingMode.DOWN);
        var a = wrap("5", "a");
        var b = wrap("3", "b");
        var sum = a.add(b, mathContext, null);       // 8
        var diff = a.subtract(b, mathContext, null);  // 2
        var prod = sum.multiply(diff, mathContext, Descriptor.descriptor("a2_minus_b2")); // 16

        assertValue(prod, "16");
        assertVariableCount(6);   // a, b, mc, sum, diff, prod (mc is shared/reused but registered once)
        assertOperationCount(3);  // add, subtract, multiply
    }

    @Test
    void operation_input_ids_match_tracked_variables() {
        var mathContext = mc(2, RoundingMode.DOWN);
        var a = wrap("10", "a");
        var b = wrap("4", "b");
        var sum = a.add(b, mathContext, null);

        var record = ctx.snapshot();
        var op = record.operations().get(0);

        // The operation's inputs should reference a, b, and mc by ID
        assertEquals(3, op.arguments().size());
        var inputIds = op.arguments().values().stream().toList();
        assertTrue(inputIds.contains(a.getVariableTrack().getId()));
        assertTrue(inputIds.contains(b.getVariableTrack().getId()));
        assertTrue(inputIds.contains(mathContext.getVariableTrack().getId()));

        // The result ID should map to the sum variable
        assertEquals(sum.getVariableTrack().getId(), op.resultId());
    }

    @Test
    void variables_map_keyed_by_uuid() {
        var a = wrap("1", "a");
        var b = wrap("2", "b");

        var record = ctx.snapshot();
        assertTrue(record.variables().stream().map(v -> v.track().getId())
                .filter(id -> id.equals(a.getVariableTrack().getId())).findFirst().isPresent());
        assertTrue(record.variables().stream().map(v -> v.track().getId())
                .filter(id -> id.equals(b.getVariableTrack().getId())).findFirst().isPresent());
    }

    @Test
    void result_variable_present_in_variables_map() {
        var mathContext = mc(2, RoundingMode.DOWN);
        var a = wrap("9", "a");
        var b = wrap("3", "b");
        var sum = a.add(b, mathContext, null);

        var record = ctx.snapshot();
        assertTrue(record.variables().stream().map(v -> v.track().getId())
                .filter(id -> id.equals(sum.getVariableTrack().getId())).findFirst().isPresent());
    }

    @Test
    void json_contains_variables_as_object_and_operations_as_array() {
        var mathContext = mc(2, RoundingMode.DOWN);
        wrap("1", "x");
        wrap("2", "y");
        var a = ctx.wrapBigDecimal(new BigDecimal("1"), Descriptor.descriptor("a"));
        var b = ctx.wrapBigDecimal(new BigDecimal("2"), Descriptor.descriptor("b"));
        a.add(b, mathContext, null);

        String json = ctx.getEnvironment().toJson(ctx.snapshot());
        assertTrue(json.contains("\"variables\" : ["), "variables should be a JSON array:\n" + json);
        assertTrue(json.contains("\"operations\" : ["), "operations should be a JSON array:\n" + json);
    }

    @Test
    void json_operations_contain_input_ids_not_full_objects() {
        var mathContext = mc(2, RoundingMode.DOWN);
        var a = wrap("5", "a");
        var b = wrap("3", "b");
        a.add(b, mathContext, null);

        String json = ctx.getEnvironment().toJson(ctx.snapshot());
        assertTrue(json.contains("\"arguments\""), "inputIds field should be in JSON:\n" + json);
        assertTrue(json.contains("\"resultId\""), "resultId field should be in JSON:\n" + json);
    }
}
