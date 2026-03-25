package io.compprov.core.context;

import io.compprov.core.DataContext;
import io.compprov.core.DefaultComputationContext;
import io.compprov.core.DefaultComputationEnvironment;
import io.compprov.core.meta.Descriptor;
import io.compprov.core.variable.ValueWithDescriptor;
import io.compprov.core.wrappers.WrappedMathContext;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;


public class ContextTest {

    private static DefaultComputationEnvironment environment = new DefaultComputationEnvironment();

    private WrappedMathContext mc(DefaultComputationContext ctx, int precision, RoundingMode rm) {
        return ctx.wrapMathContext(new MathContext(precision, rm), Descriptor.descriptor("mc"));
    }

    @Test
    public void generates_output_data() {
        final var context = new DefaultComputationContext(
                environment,
                new DataContext(Descriptor.descriptor("test")));
        final var mc = mc(context, 3, RoundingMode.DOWN);
        final var x = context.wrapBigDecimal(new BigDecimal("1"), Descriptor.descriptor("x"));
        final var y = context.wrapBigDecimal(new BigDecimal("2"), Descriptor.descriptor("y"));
        final var z = x.add(y, mc, null);
        final var result = x.subtract(z, mc, Descriptor.descriptor("result"));

        String json = context.getEnvironment().toJson(context.snapshot());
        assertNotNull(json);

        String humanLog = context.getEnvironment().toHumanReadableLog(context.snapshot());
        assertNotNull(humanLog);

        System.out.println(json);
        System.out.println(humanLog);
    }

    @Test
    public void snapshot_re_calculations() {
        final var context = new DefaultComputationContext(
                environment,
                new DataContext(Descriptor.descriptor("test")));
        final var mc = context.wrapMathContext(new MathContext(3, RoundingMode.DOWN), Descriptor.descriptor("mc"));
        final var x = context.wrapBigDecimal(new BigDecimal("1"), Descriptor.descriptor("x"));
        final var y = context.wrapBigDecimal(new BigDecimal("2"), Descriptor.descriptor("y"));
        final var z = x.add(y, mc, null);
        final var result = x.subtract(z, mc, Descriptor.descriptor("result"));

        String json = environment.toJson(context.snapshot());
        final var recovered = context.getEnvironment().fromJson(json);

        //reproduced
        final var reproduced = environment.compute(recovered);
        assertEquals("1", reproduced.getVariable("i_2").getValue().toString());
        assertEquals("2", reproduced.getVariable("i_3").getValue().toString());
        assertEquals("3", reproduced.getVariable("o_4").getValue().toString());
        assertEquals("-2", reproduced.getVariable("o_5").getValue().toString());

        //updated
        final var updated = context.getEnvironment().copyWith(
                recovered,
                Descriptor.descriptor("updated"),
                Map.of("i_3", new ValueWithDescriptor(Descriptor.descriptor("new_value"), new BigDecimal("-12"))));
        final var updatedComputation = environment.compute(updated);
        assertEquals("1", updatedComputation.getVariable("i_2").getValue().toString());
        assertEquals("-12", updatedComputation.getVariable("i_3").getValue().toString());
        assertEquals("-11", updatedComputation.getVariable("o_4").getValue().toString());
        assertEquals("12", updatedComputation.getVariable("o_5").getValue().toString());
    }

    @Test
    public void context_tracks_variable_count() {
        final var context = new DefaultComputationContext(
                new DefaultComputationEnvironment(),
                new DataContext(Descriptor.descriptor("test")));
        final var mc = mc(context, 3, RoundingMode.DOWN);
        final var x = context.wrapBigDecimal(new BigDecimal("3"), Descriptor.descriptor("x"));
        final var y = context.wrapBigDecimal(new BigDecimal("4"), Descriptor.descriptor("y"));
        x.add(y, mc, null);

        // x, y, mc, result = 4 variables, 1 operation
        final var record = context.snapshot();
        assertEquals(4, record.variables().size());
        assertEquals(1, record.operations().size());
    }

    @Test
    public void context_operations_chain_correctly() {
        final var context = new DefaultComputationContext(
                new DefaultComputationEnvironment(),
                new DataContext(Descriptor.descriptor("test")));
        final var mc = mc(context, 3, RoundingMode.DOWN);
        final var a = context.wrapBigDecimal(new BigDecimal("10"), Descriptor.descriptor("a"));
        final var b = context.wrapBigDecimal(new BigDecimal("5"), Descriptor.descriptor("b"));
        final var sum = a.add(b, mc, null);
        final var product = sum.multiply(b, mc, null);

        // a, b, mc, sum, product = 5 variables; add, multiply = 2 operations
        final var record = context.snapshot();
        assertEquals(5, record.variables().size());
        assertEquals(2, record.operations().size());
    }

    @Test
    public void named_result_descriptor_is_preserved() {
        final var context = new DefaultComputationContext(
                new DefaultComputationEnvironment(),
                new DataContext(Descriptor.descriptor("test")));
        final var mc = mc(context, 3, RoundingMode.DOWN);
        final var x = context.wrapBigDecimal(new BigDecimal("7"), Descriptor.descriptor("x"));
        final var y = context.wrapBigDecimal(new BigDecimal("3"), Descriptor.descriptor("y"));
        final var result = x.subtract(y, mc, Descriptor.descriptor("diff"));

        assertEquals("diff", result.getVariableTrack().getDescriptor().getName());
        assertEquals(new BigDecimal("4"), result.getValue());
    }

    @Test
    public void variable_track_contains_underlying_value_class_name() {
        final var context = new DefaultComputationContext(
                new DefaultComputationEnvironment(),
                new DataContext(Descriptor.descriptor("test")));
        final var x = context.wrapBigDecimal(new BigDecimal("1"), Descriptor.descriptor("x"));

        assertEquals("java.math.BigDecimal", x.getVariableTrack().getValueClass());
    }

    @Test
    public void operation_track_contains_wrapper_class_name() {
        final var context = new DefaultComputationContext(
                new DefaultComputationEnvironment(),
                new DataContext(Descriptor.descriptor("test")));
        final var mc = mc(context, 3, RoundingMode.DOWN);
        final var x = context.wrapBigDecimal(new BigDecimal("1"), Descriptor.descriptor("x"));
        final var y = context.wrapBigDecimal(new BigDecimal("2"), Descriptor.descriptor("y"));
        x.add(y, mc, null);

        final var op = context.snapshot().operations().get(0);
        assertEquals("io.compprov.core.wrappers.WrappedBigDecimal", op.track().getWrapperClass());
    }

    @Test
    public void fromJson_restores_variables_and_operations() {

        final var ctx = new DefaultComputationContext(
                new DefaultComputationEnvironment(),
                new DataContext(Descriptor.descriptor("test")));

        final var mc = mc(ctx, 3, RoundingMode.DOWN);
        final var x = ctx.wrapBigDecimal(new BigDecimal("3"), Descriptor.descriptor("x"));
        final var y = ctx.wrapBigDecimal(new BigDecimal("4"), Descriptor.descriptor("y"));
        x.add(y, mc, Descriptor.descriptor("sum"));

        String json = ctx.getEnvironment().toJson(ctx.snapshot());

        final var newEnv = new DefaultComputationEnvironment();
        final var restored = newEnv.fromJson(json);

        assertEquals(ctx.snapshot(), restored);
    }

    @Test
    public void wrap_throws_when_descriptor_required_and_null() {
        final var context = new DefaultComputationContext(
                new DefaultComputationEnvironment(true, false),
                new DataContext(Descriptor.descriptor("test")));

        assertThrows(IllegalArgumentException.class,
                () -> context.wrapBigDecimal(new BigDecimal("1"), null));
    }

    @Test
    public void big_integer_track_contains_correct_class_names() {
        final var context = new DefaultComputationContext(
                new DefaultComputationEnvironment(),
                new DataContext(Descriptor.descriptor("test")));
        final var a = context.wrapBigInteger(java.math.BigInteger.valueOf(6), Descriptor.descriptor("a"));
        final var b = context.wrapBigInteger(java.math.BigInteger.valueOf(3), Descriptor.descriptor("b"));
        a.divide(b, null);

        assertEquals("java.math.BigInteger", a.getVariableTrack().getValueClass());
        final var op = context.snapshot().operations().get(0);
        assertEquals("io.compprov.core.wrappers.WrappedBigInteger", op.track().getWrapperClass());
    }
}
