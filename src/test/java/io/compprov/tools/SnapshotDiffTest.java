package io.compprov.tools;

import io.compprov.core.DataContext;
import io.compprov.core.DefaultComputationContext;
import io.compprov.core.DefaultComputationEnvironment;
import io.compprov.core.Snapshot;
import io.compprov.core.meta.Descriptor;
import io.compprov.core.meta.Meta;
import io.compprov.core.variable.ValueWithDescriptor;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import static io.compprov.core.meta.Descriptor.descriptor;
import static org.junit.jupiter.api.Assertions.*;

/*
 * Base computation (IDs in a fresh context):
 *   i_1 mc, i_2 x, i_3 y
 *   op_1: add(x, y)          -> o_4 "sum"
 *   op_2: subtract(x, sum)   -> o_5 "result"
 */
class SnapshotDiffTest {

    private static final DefaultComputationEnvironment env = DefaultComputationEnvironment.create();

    private static DefaultComputationContext newContext(String name) {
        return new DefaultComputationContext(env, new DataContext(descriptor(name)));
    }

    private static Snapshot compute(String name, String x, String y) {
        final var ctx = newContext(name);
        final var mc = ctx.wrapMathContext(new MathContext(10, RoundingMode.HALF_UP), descriptor("mc"));
        final var vx = ctx.wrapBigDecimal(new BigDecimal(x), descriptor("x"));
        final var vy = ctx.wrapBigDecimal(new BigDecimal(y), descriptor("y"));
        final var sum = vx.add(vy, mc, descriptor("sum"));
        vx.subtract(sum, mc, descriptor("result"));
        return ctx.snapshot();
    }

    private static Set<String> leftIds(List<SnapshotDiff.VariablePair> pairs) {
        return pairs.stream().map(p -> p.left().track().getId()).collect(Collectors.toSet());
    }

    private static Set<String> ids(List<Snapshot.Variable> vars) {
        return vars.stream().map(v -> v.track().getId()).collect(Collectors.toSet());
    }

    @Test
    void identicalRuns_noDifferences() {
        final var diff = new SnapshotDiff(compute("a", "1", "2"), compute("b", "1", "2"));
        assertTrue(diff.isIdentical(), diff.toReport());
        assertEquals(2, diff.matchedOperations().size());
    }

    @Test
    void replayOfSameSnapshot_noDifferences() {
        final var original = compute("a", "1", "2");
        final var replayed = env.compute(original).snapshot();
        assertTrue(new SnapshotDiff(original, replayed).isIdentical());
    }

    @Test
    void whatIfReplay_classifiesInputAndPropagatedChanges() {
        final var original = compute("original", "1", "2");
        final var updated = env.copyWith(original, descriptor("what-if"),
                Map.of("i_3", new ValueWithDescriptor(descriptor("y"), new BigDecimal("-12"))));
        final var replayed = env.compute(updated).snapshot();

        final var diff = new SnapshotDiff(original, replayed);
        assertTrue(diff.isStructurallyEqual());
        assertEquals(Set.of("i_3"), leftIds(diff.inputChanges()));
        assertEquals(Set.of("o_4", "o_5"), leftIds(diff.propagatedChanges()));
        assertTrue(diff.unexplainedChanges().isEmpty());
        assertEquals(new BigDecimal("12"), diff.rightOf("o_5").orElseThrow().value());
        System.out.println(diff.toReport());
    }

    @Test
    void renamedInput_reportedAsDescriptorChange() {
        final var original = compute("original", "1", "2");
        final var updated = env.copyWith(original, descriptor("what-if"),
                Map.of("i_3", new ValueWithDescriptor(descriptor("y_scenario"), new BigDecimal("2"))));
        final var diff = new SnapshotDiff(original, env.compute(updated).snapshot());

        assertTrue(diff.valueChanges().isEmpty());
        assertEquals(Set.of("i_3"), leftIds(diff.variableDescriptorChanges()));
    }

    @Test
    void tamperedResult_reportedAsUnexplained() {
        final var original = compute("original", "1", "2");
        // Same operation over the same inputs, but o_4 carries a different value
        final var tampered = new Snapshot(descriptor("tampered"),
                original.variables().stream()
                        .map(v -> v.track().getId().equals("o_4")
                                ? new Snapshot.Variable(v.track(), new BigDecimal("4"))
                                : v)
                        .toList(),
                original.operations());

        final var diff = new SnapshotDiff(original, tampered);
        assertTrue(diff.inputChanges().isEmpty());
        assertEquals(Set.of("o_4"), leftIds(diff.unexplainedChanges()));
        // o_5 was not recomputed in the tampered snapshot, so its value is unchanged
        assertTrue(diff.propagatedChanges().isEmpty());
    }

    @Test
    void differentOperation_reportedAsStructuralDifference() {
        final var original = compute("original", "1", "2");

        final var ctx = newContext("changed");
        final var mc = ctx.wrapMathContext(new MathContext(10, RoundingMode.HALF_UP), descriptor("mc"));
        final var x = ctx.wrapBigDecimal(new BigDecimal("1"), descriptor("x"));
        final var y = ctx.wrapBigDecimal(new BigDecimal("2"), descriptor("y"));
        final var sum = x.add(y, mc, descriptor("sum"));
        x.multiply(sum, mc, descriptor("result"));

        final var diff = new SnapshotDiff(original, ctx.snapshot());
        assertFalse(diff.isStructurallyEqual());
        assertEquals(Set.of("o_5"), ids(diff.leftOnlyVariables()));
        assertEquals(Set.of("o_5"), ids(diff.rightOnlyVariables()));
        assertEquals(1, diff.leftOnlyOperations().size());
        assertTrue(diff.valueChanges().isEmpty());
    }

    @Test
    void shiftedIds_matchedByStructure() {
        final var original = compute("original", "1", "2");

        // An extra input wrapped first shifts every ID by one
        final var ctx = newContext("shifted");
        ctx.wrapBigDecimal(new BigDecimal("100"), descriptor("extra"));
        final var mc = ctx.wrapMathContext(new MathContext(10, RoundingMode.HALF_UP), descriptor("mc"));
        final var x = ctx.wrapBigDecimal(new BigDecimal("1"), descriptor("x"));
        final var y = ctx.wrapBigDecimal(new BigDecimal("5"), descriptor("y"));
        final var sum = x.add(y, mc, descriptor("sum"));
        x.subtract(sum, mc, descriptor("result"));
        final var shifted = ctx.snapshot();

        final var byId = new SnapshotDiff(original, shifted);
        assertTrue(byId.matchedOperations().isEmpty(), "IDs are misaligned, nothing should match by ID");

        final var byStructure = new SnapshotDiff(original, shifted,
                SnapshotDiff.Matching.BY_STRUCTURE, java.util.Objects::equals);
        assertTrue(byStructure.leftOnlyVariables().isEmpty());
        assertEquals(Set.of("extra"), byStructure.rightOnlyVariables().stream()
                .map(v -> v.track().getDescriptor().getName()).collect(Collectors.toSet()));
        assertEquals(Set.of("i_3"), leftIds(byStructure.inputChanges()));
        assertEquals("i_4", byStructure.rightOf("i_3").orElseThrow().track().getId());
        assertEquals(Set.of("o_4", "o_5"), leftIds(byStructure.propagatedChanges()));
    }

    @Test
    void customValueEquality_ignoresScale() {
        final var left = compute("a", "1", "2");
        final var right = compute("b", "1.00", "2");

        final var diff = new SnapshotDiff(left, right);
        assertTrue(diff.isIdentical(), diff.toReport());
    }

    @Test
    void consumerOrderedBeforeProducer_rejected() {
        final var original = compute("original", "1", "2");
        // Swap operation IDs: subtract (consumes o_4) becomes op_1, add (produces o_4) becomes op_2
        final var swapped = original.operations().stream()
                .map(op -> new Snapshot.Operation(
                        new io.compprov.core.operation.OperationTrack(
                                3 - op.track().getNumericId(), op.track().getStartedAt(), op.track().getFinishedAt(),
                                op.track().getDescriptor(), op.track().getWrapperClass()),
                        op.arguments(), op.resultId()))
                .toList();
        final var broken = new Snapshot(descriptor("broken"), original.variables(), swapped);

        assertThrows(IllegalArgumentException.class, () -> new SnapshotDiff(original, broken));
    }

    @Test
    void operationMetaChange_reportedButStillMatched() {
        final var original = compute("original", "1", "2");
        final var op = original.operations().get(0);
        final var changedOp = new Snapshot.Operation(
                new io.compprov.core.operation.OperationTrack(
                        op.track().getNumericId(), op.track().getStartedAt(), op.track().getFinishedAt(),
                        new Descriptor(op.track().getDescriptor().getName(), Meta.formula("x + y")),
                        op.track().getWrapperClass()),
                op.arguments(), op.resultId());
        final var changed = new Snapshot(descriptor("changed"), original.variables(),
                List.of(changedOp, original.operations().get(1)));

        final var diff = new SnapshotDiff(original, changed);
        assertTrue(diff.isStructurallyEqual());
        assertEquals(1, diff.operationDescriptorChanges().size());
    }
}
