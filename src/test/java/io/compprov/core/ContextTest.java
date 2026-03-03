package io.compprov.core;

import io.compprov.core.variable.VariableKind;
import io.compprov.core.wrappers.WrappedMathContext;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;

import static org.junit.jupiter.api.Assertions.*;

public class ContextTest {

    private WrappedMathContext mc(DefaultContext ctx, int precision, RoundingMode rm) {
        return ctx.wrapMathContext(new MathContext(precision, rm), Descriptor.descriptor("mc"));
    }

    @Test
    public void generates_json() {
        final var context = new DefaultContext();
        final var mc = mc(context, 3, RoundingMode.DOWN);
        final var x = context.wrapBigDecimal(new BigDecimal("1"), Descriptor.descriptor("x"));
        final var y = context.wrapBigDecimal(new BigDecimal("2"), Descriptor.descriptor("y"));
        final var z = x.add(y, mc, null);
        final var result = x.add(z, mc, Descriptor.descriptor("result"));

        String json = context.toJson();
        assertNotNull(json);

        String humanLog = context.toHumanReadableLog();
        assertNotNull(humanLog);

        System.out.println(json);
        System.out.println(humanLog);
    }

    @Test
    public void context_tracks_variable_count() {
        final var context = new DefaultContext();
        final var mc = mc(context, 3, RoundingMode.DOWN);
        final var x = context.wrapBigDecimal(new BigDecimal("3"), Descriptor.descriptor("x"));
        final var y = context.wrapBigDecimal(new BigDecimal("4"), Descriptor.descriptor("y"));
        x.add(y, mc, null);

        // x, y, mc, result = 4 variables, 1 operation
        final var record = context.export();
        assertEquals(4, record.variables().size());
        assertEquals(1, record.operations().size());
    }

    @Test
    public void context_operations_chain_correctly() {
        final var context = new DefaultContext();
        final var mc = mc(context, 3, RoundingMode.DOWN);
        final var a = context.wrapBigDecimal(new BigDecimal("10"), Descriptor.descriptor("a"));
        final var b = context.wrapBigDecimal(new BigDecimal("5"), Descriptor.descriptor("b"));
        final var sum = a.add(b, mc, null);
        final var product = sum.multiply(b, mc, null);

        // a, b, mc, sum, product = 5 variables; add, multiply = 2 operations
        final var record = context.export();
        assertEquals(5, record.variables().size());
        assertEquals(2, record.operations().size());
    }

    @Test
    public void named_result_descriptor_is_preserved() {
        final var context = new DefaultContext();
        final var mc = mc(context, 3, RoundingMode.DOWN);
        final var x = context.wrapBigDecimal(new BigDecimal("7"), Descriptor.descriptor("x"));
        final var y = context.wrapBigDecimal(new BigDecimal("3"), Descriptor.descriptor("y"));
        final var result = x.subtract(y, mc, Descriptor.descriptor("diff"));

        assertEquals("diff", result.getVariableTrack().getDescriptor().getName());
        assertEquals(new BigDecimal("4"), result.getValue());
    }

    @Test
    public void variable_track_contains_underlying_value_class_name() {
        final var context = new DefaultContext();
        final var x = context.wrapBigDecimal(new BigDecimal("1"), Descriptor.descriptor("x"));

        assertEquals("java.math.BigDecimal", x.getVariableTrack().getValueClass().getName());
    }

    @Test
    public void operation_track_contains_wrapper_class_name() {
        final var context = new DefaultContext();
        final var mc = mc(context, 3, RoundingMode.DOWN);
        final var x = context.wrapBigDecimal(new BigDecimal("1"), Descriptor.descriptor("x"));
        final var y = context.wrapBigDecimal(new BigDecimal("2"), Descriptor.descriptor("y"));
        x.add(y, mc, null);

        final var op = context.export().operations().get(0);
        assertEquals("io.compprov.core.wrappers.WrappedBigDecimal", op.getOperationTrack().getWrapperClass().getName());
    }

    @Test
    public void fromJson_restores_variables_and_operations() {
        final var ctx = new DefaultContext();
        final var mc = mc(ctx, 3, RoundingMode.DOWN);
        final var x = ctx.wrapBigDecimal(new BigDecimal("3"), Descriptor.descriptor("x"));
        final var y = ctx.wrapBigDecimal(new BigDecimal("4"), Descriptor.descriptor("y"));
        x.add(y, mc, Descriptor.descriptor("sum"));

        String json = ctx.toJson();

        final var restored = new DefaultContext();
        Context.ContextRecord record = restored.fromJson(json);

        assertEquals(ctx.export().variables().size(), record.variables().size());
        assertEquals(ctx.export().operations().size(), record.operations().size());

        // spot-check a variable: descriptor name and value round-trip correctly
        var restoredX = record.variables().get("i_2"); // mc=i_1, x=i_2
        assertEquals("x", restoredX.getDescriptor().getName());
        assertEquals(new BigDecimal("3"), restoredX.getValue());
        assertEquals(VariableKind.INPUT, restoredX.getVariableTrack().getKind());

        // operation input/result IDs are preserved
        var op = record.operations().get(0);
        assertEquals("add", op.getOperationTrack().getDescriptor().getName());
        assertFalse(op.getInputIds().isEmpty());
        assertNotNull(op.getResultId());
    }

    @Test
    public void big_integer_track_contains_correct_class_names() {
        final var context = new DefaultContext();
        final var a = context.wrapBigInteger(java.math.BigInteger.valueOf(6), Descriptor.descriptor("a"));
        final var b = context.wrapBigInteger(java.math.BigInteger.valueOf(3), Descriptor.descriptor("b"));
        a.divide(b, null);

        assertEquals("java.math.BigInteger", a.getVariableTrack().getValueClass().getName());
        final var op = context.export().operations().get(0);
        assertEquals("io.compprov.core.wrappers.WrappedBigInteger", op.getOperationTrack().getWrapperClass().getName());
    }
}
