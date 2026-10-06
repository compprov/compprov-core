package io.compprov.tools;

import io.compprov.core.DataContext;
import io.compprov.core.DefaultComputationContext;
import io.compprov.core.DefaultComputationEnvironment;
import io.compprov.core.Snapshot;
import io.compprov.core.meta.Descriptor;
import io.compprov.core.operation.OperationTrack;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

import java.io.IOException;
import java.math.BigDecimal;
import java.math.MathContext;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;

/*
 * Graph: snapshots/metrology.json — gauge-block interferometry / refractometry
 * 96 variables (40 INPUT, 56 OUTPUT), 56 operations.
 *
 * Notable properties exercised by these tests:
 *  - i_1 (computation precision) is the mc argument in every operation
 *  - i_4, i_5 (unused wavelengths) are never referenced in any op → leaves + unused inputs
 *  - o_96 (deltaL) is the only OUTPUT leaf (the final measurement result)
 *  - op_2 squares o_12: multiply(o_12, o_12) — same variable used twice in one op
 *  - o_96 depends transitively on all 56 operations
 */
class SnapshotNavigatorTest {

    private static Snapshot snapshot;
    private static SnapshotNavigator nav;
    private static DefaultComputationEnvironment env;

    @BeforeAll
    static void setup() throws IOException {
        env = DefaultComputationEnvironment.create();
        byte[] json = SnapshotNavigatorTest.class
                .getResourceAsStream("/snapshots/metrology.json")
                .readAllBytes();
        snapshot = env.fromJson(json);
        nav = new SnapshotNavigator(snapshot);
    }

    // ── helpers ──────────────────────────────────────────────────────────────

    private static Set<String> varIds(List<Snapshot.Variable> vars) {
        return vars.stream().map(v -> v.track().getId()).collect(Collectors.toSet());
    }

    private static Set<String> opIds(List<Snapshot.Operation> ops) {
        return ops.stream().map(o -> o.track().getId()).collect(Collectors.toSet());
    }

    // ── getSnapshot ──────────────────────────────────────────────────────────

    @Test
    void getSnapshot_returnsSameInstance() {
        assertSame(snapshot, nav.getSnapshot());
    }

    // ── variable / operation lookup ──────────────────────────────────────────

    @Test
    void variable_knownId_returnsVariable() {
        var result = nav.variable("i_1");
        assertTrue(result.isPresent());
        assertEquals("i_1", result.get().track().getId());
    }

    @Test
    void variable_unknownId_returnsEmpty() {
        assertTrue(nav.variable("nonexistent").isEmpty());
    }

    @Test
    void operation_knownId_returnsOperation() {
        var result = nav.operation("op_1");
        assertTrue(result.isPresent());
        assertEquals("op_1", result.get().track().getId());
    }

    @Test
    void operation_unknownId_returnsEmpty() {
        assertTrue(nav.operation("nonexistent").isEmpty());
    }

    // ── leaves ───────────────────────────────────────────────────────────────

    @Test
    void leaves_unusedInputsAndFinalOutput() {
        // i_4 and i_5 are INPUT variables never referenced in any operation
        // o_96 (deltaL) is the final computed result, not consumed by any operation
        assertEquals(Set.of("i_4", "i_5", "o_96"), varIds(nav.leaves()));
    }

    // ── inputs ───────────────────────────────────────────────────────────────

    @Test
    void inputs_countAndSpotCheck() {
        List<Snapshot.Variable> inputs = nav.roots();
        assertEquals(40, inputs.size());
        Set<String> ids = varIds(inputs);
        assertTrue(ids.containsAll(Set.of("i_1", "i_4", "i_5", "i_7", "i_95")));
    }

    // ── unused ───────────────────────────────────────────────────────────────

    @Test
    void unused_twoUnconsumedInputVariables() {
        // i_4 (lambda_vac1) and i_5 (lambda_vac2) are present but never consumed
        assertEquals(Set.of("i_4", "i_5"), varIds(nav.unused()));
    }

    // ── produces ─────────────────────────────────────────────────────────────

    @Test
    void produces_unusedInput_returnsEmpty() {
        assertTrue(nav.produces("i_4").isEmpty());
    }

    @Test
    void produces_variableWithTwoConsumers() {
        // i_6 (HeNe wavelength) consumed by op_10 (→ o_26) and op_48 (→ o_82)
        assertEquals(Set.of("o_26", "o_82"), varIds(nav.produces("i_6")));
    }

    @Test
    void produces_universalMcParameter_mostOutputVariables() {
        // i_1 is the mc argument in 55 of the 56 operations (op_9/Exp_double has no mc arg)
        assertEquals(55, varIds(nav.produces("i_1")).size());
    }

    @Test
    void produces_intermediateVariable_singleDownstream() {
        // o_23 (Wexler exponent) consumed by op_9 only → o_24 (svp)
        assertEquals(Set.of("o_24"), varIds(nav.produces("o_23")));
    }

    @Test
    void produces_leaf_returnsEmpty() {
        assertTrue(nav.produces("o_96").isEmpty());
    }

    // ── producedBy ───────────────────────────────────────────────────────────

    @Test
    void producedBy_outputVariable_returnsProducingOp() {
        var result = nav.producedBy("o_12");
        assertTrue(result.isPresent());
        assertEquals("op_1", result.get().track().getId());
    }

    @Test
    void producedBy_inputVariable_returnsEmpty() {
        assertTrue(nav.producedBy("i_7").isEmpty());
    }

    // ── dependsOn ────────────────────────────────────────────────────────────

    @Test
    void dependsOn_returnsDirectArgumentsOfProducingOp() {
        // op_1: add(a=i_7, b=i_11, mc=i_1) → o_12
        assertEquals(Set.of("i_7", "i_11", "i_1"), varIds(nav.dependsOn("o_12")));
    }

    @Test
    void dependsOn_deduplicatesSameVariableUsedTwice() {
        // op_2: multiply(a=o_12, b=o_12, mc=i_1) → o_13 — same variable in both arguments
        // .distinct() must collapse it to a single entry
        assertEquals(Set.of("o_12", "i_1"), varIds(nav.dependsOn("o_13")));
    }

    @Test
    void dependsOn_finalOutput_directArgsOnly() {
        // op_56: subtract(a=o_94, b=i_95, mc=i_1) → o_96
        assertEquals(Set.of("o_94", "i_95", "i_1"), varIds(nav.dependsOn("o_96")));
    }

    @Test
    void dependsOn_inputVariable_returnsEmpty() {
        assertTrue(nav.dependsOn("i_4").isEmpty());
    }

    // ── producesDeep ─────────────────────────────────────────────────────────

    @Test
    void producesDeep_unusedInput_returnsEmpty() {
        assertTrue(nav.producesDeep("i_4").isEmpty());
    }

    @Test
    void producesDeep_inputConsumedByFinalOpOnly() {
        // i_95 (L_nom) only consumed by op_56 → o_96 (leaf)
        assertEquals(Set.of("o_96"), varIds(nav.producesDeep("i_95")));
    }

    @Test
    void producesDeep_transitivelyReachesFinalOutput() {
        // i_11 (Kelvin offset) → o_12 → ... → o_96; both direct hop and final output present
        Set<String> result = varIds(nav.producesDeep("i_11"));
        assertTrue(result.contains("o_12"), "must include direct first hop");
        assertTrue(result.contains("o_96"), "must reach final output");
        assertFalse(result.contains("o_86"), "o_86 is only reachable via i_84/i_85, not from i_11");
    }

    @Test
    void producesDeep_withStop_haltAtStopVariable() {
        // o_12 should appear in result (stop does not exclude the boundary itself),
        // but nothing downstream of o_12 should be traversed
        Set<String> result = varIds(nav.producesDeep("i_11", Set.of("o_12")));
        assertTrue(result.contains("o_12"), "stop variable itself must be included");
        assertFalse(result.contains("o_96"), "downstream of stop must not appear");
    }

    // ── producedByDeep ───────────────────────────────────────────────────────

    @Test
    void producedByDeep_singleHopVariable() {
        // o_12 produced by op_1 only; its args (i_7, i_11, i_1) are all inputs
        assertEquals(Set.of("op_1"), opIds(nav.producedByDeep("o_12")));
    }

    @Test
    void producedByDeep_inputVariable_returnsEmpty() {
        assertTrue(nav.producedByDeep("i_7").isEmpty());
    }

    @Test
    void producedByDeep_finalOutput_allOpsInAncestry() {
        // o_96 is the result of a calculation that uses every single operation
        assertEquals(56, nav.producedByDeep("o_96").size());
    }

    @Test
    void producedByDeep_withStop_limitsAncestryToTopOp() {
        // Stopping at o_94 cuts the entire production chain — only the last op survives
        // op_56: subtract(o_94, i_95, mc=i_1) → o_96
        assertEquals(Set.of("op_56"), opIds(nav.producedByDeep("o_96", Set.of("o_94"))));
    }

    // ── dependsOnDeep ────────────────────────────────────────────────────────

    @Test
    void dependsOnDeep_singleHop_directInputsOnly() {
        // o_12 depends on i_7, i_11, i_1 — all are inputs with no further deps
        assertEquals(Set.of("i_7", "i_11", "i_1"), varIds(nav.dependsOnDeep("o_12")));
    }

    @Test
    void dependsOnDeep_inputVariable_returnsEmpty() {
        assertTrue(nav.dependsOnDeep("i_7").isEmpty());
    }

    @Test
    void dependsOnDeep_finalOutput_fullAncestryExcludingUnusedInputs() {
        // 38 used inputs (40 total − i_4 − i_5) + 55 intermediate outputs (56 total − o_96) = 93
        Set<String> result = varIds(nav.dependsOnDeep("o_96"));
        assertEquals(93, result.size());
        assertFalse(result.contains("i_4"), "i_4 is unused — not reachable by any operation");
        assertFalse(result.contains("i_5"), "i_5 is unused — not reachable by any operation");
        assertFalse(result.contains("o_96"), "a variable cannot depend on itself");
        assertTrue(result.contains("i_7"),  "T_air feeds the refractivity chain");
        assertTrue(result.contains("i_1"),  "mc parameter used by all ops");
    }

    @Test
    void dependsOnDeep_withStop_haltAtBoundaryVariables() {
        // Stopping at o_61 (dry-air refractivity) and o_78 (water-vapor refractivity):
        // those two boundary variables must still appear in the result,
        // but i_7 (T_air), which is only reachable through the sub-chain behind them, must not
        Set<String> result = varIds(nav.dependsOnDeep("o_96", Set.of("o_61", "o_78")));
        assertTrue(result.contains("o_61"), "boundary variable must be included");
        assertTrue(result.contains("o_78"), "boundary variable must be included");
        assertFalse(result.contains("i_7"), "T_air is only reachable past the stop boundary");
    }

    // ── cpgOf ────────────────────────────────────────────────────────────────

    @Test
    void cpgOf_singleOperation_exactContents() {
        // o_12 (T_K) produced by op_1(i_7, i_11, mc=i_1)
        Snapshot sub = nav.cpgOf("o_12", Set.of());
        assertEquals(Set.of("i_1", "i_7", "i_11", "o_12"), varIds(sub.variables()));
        assertEquals(Set.of("op_1"), opIds(sub.operations()));
    }

    @Test
    void cpgOf_finalOutput_fullGraphWithoutUnusedInputs() {
        // 94 variables: all 96 minus the two unused inputs (i_4, i_5)
        Snapshot sub = nav.cpgOf("o_96", Set.of());
        assertEquals(94, sub.variables().size());
        assertEquals(56, sub.operations().size());
    }

    @Test
    void cpgOf_withStop_retainsOnlyTopLayer() {
        // Stopping at o_94: only the final subtraction op survives
        Snapshot sub = nav.cpgOf("o_96", Set.of("o_94"));
        assertEquals(Set.of("o_94", "i_95", "i_1", "o_96"), varIds(sub.variables()));
        assertEquals(Set.of("op_56"), opIds(sub.operations()));
    }

    @Test
    void cpgOf_variablesSortedByNumericId() {
        List<Integer> ids = nav.cpgOf("o_96", Set.of()).variables().stream()
                .map(v -> v.track().getNumericId())
                .toList();
        for (int i = 1; i < ids.size(); i++) {
            assertTrue(ids.get(i - 1) < ids.get(i), "variables not sorted at index " + i);
        }
    }

    @Test
    void cpgOf_operationsSortedByNumericId() {
        List<Integer> ids = nav.cpgOf("o_96", Set.of()).operations().stream()
                .map(o -> o.track().getNumericId())
                .toList();
        for (int i = 1; i < ids.size(); i++) {
            assertTrue(ids.get(i - 1) < ids.get(i), "operations not sorted at index " + i);
        }
    }

    // ── ordering ─────────────────────────────────────────────────────────────

    @Test
    void roots_leaves_unused_orderedByNumericId() {
        assertSortedVariables(nav.roots());
        assertSortedVariables(nav.leaves());
        assertSortedVariables(nav.unused());
        assertSortedVariables(nav.producesDeep("i_11"));
        assertSortedVariables(nav.dependsOnDeep("o_96"));
    }

    private static void assertSortedVariables(List<Snapshot.Variable> vars) {
        for (int i = 1; i < vars.size(); i++) {
            assertTrue(vars.get(i - 1).track().getNumericId() < vars.get(i).track().getNumericId(),
                    "not sorted at index " + i);
        }
    }

    // ── arguments ────────────────────────────────────────────────────────────

    @Test
    void arguments_keepsPositionAndDuplicates() {
        // op_2: multiply(a=o_12, b=o_12, mc=i_1)
        assertEquals(List.of("o_12", "o_12", "i_1"),
                nav.arguments("op_2").stream().map(v -> v.track().getId()).toList());
    }

    @Test
    void arguments_unknownOperation_throws() {
        assertThrows(IllegalArgumentException.class, () -> nav.arguments("op_999"));
    }

    @Test
    void produces_variableUsedTwiceInOneOp_listedOnce() {
        // op_2 consumes o_12 twice, but o_13 must be listed only once (o_12 has other consumers too)
        assertEquals(List.of("o_13", "o_19", "o_20"),
                nav.produces("o_12").stream().map(v -> v.track().getId()).toList());
    }

    // ── validation ───────────────────────────────────────────────────────────

    private static Snapshot.Operation withNumericId(Snapshot.Operation op, int numericId) {
        return new Snapshot.Operation(
                new OperationTrack(numericId, op.track().getStartedAt(), op.track().getFinishedAt(),
                        op.track().getDescriptor(), op.track().getWrapperClass()),
                op.arguments(), op.resultId());
    }

    @Test
    void validation_consumerBeforeProducer_throws() {
        // op_1 produces o_12, op_2 consumes it; swapping their IDs breaks the order
        final var ops = snapshot.operations().stream()
                .map(op -> switch (op.track().getId()) {
                    case "op_1" -> withNumericId(op, 2);
                    case "op_2" -> withNumericId(op, 1);
                    default -> op;
                })
                .toList();
        final var broken = new Snapshot(Descriptor.descriptor("broken"), snapshot.variables(), ops);
        final var e = assertThrows(IllegalArgumentException.class, () -> new SnapshotNavigator(broken));
        assertTrue(e.getMessage().contains("later operation"), e.getMessage());
    }

    @Test
    void validation_unknownArgument_throws() {
        final var vars = snapshot.variables().stream().filter(v -> !v.track().getId().equals("i_7")).toList();
        final var broken = new Snapshot(Descriptor.descriptor("broken"), vars, snapshot.operations());
        assertThrows(IllegalArgumentException.class, () -> new SnapshotNavigator(broken));
    }

    @Test
    void validation_variableProducedTwice_throws() {
        final var ops = new ArrayList<>(snapshot.operations());
        ops.add(withNumericId(ops.get(ops.size() - 1), 1000));
        final var broken = new Snapshot(Descriptor.descriptor("broken"), snapshot.variables(), ops);
        assertThrows(IllegalArgumentException.class, () -> new SnapshotNavigator(broken));
    }

    // ── large graphs ─────────────────────────────────────────────────────────

    @Test
    void deepTraversal_longChain_noStackOverflow() {
        final var ctx = new DefaultComputationContext(env, new DataContext(Descriptor.descriptor("chain")));
        final var mc = ctx.wrapMathContext(MathContext.DECIMAL64, Descriptor.descriptor("mc"));
        final var one = ctx.wrapBigDecimal(BigDecimal.ONE, Descriptor.descriptor("one"));
        var x = one;
        for (int i = 0; i < 50_000; i++) {
            x = x.add(one, mc, null);
        }
        final var chain = new SnapshotNavigator(ctx.snapshot());
        final var last = x.getVariableTrack().getId();

        assertEquals(50_000, chain.producedByDeep(last).size());
        assertEquals(50_000, chain.producesDeep("i_2").size());
    }

    @Test
    @Timeout(10)
    void deepTraversal_sharedBranches_visitsEachNodeOnce() {
        // x' = (x * y) + (x + y), repeated: each step joins two branches, so paths double every step
        final var ctx = new DefaultComputationContext(env, new DataContext(Descriptor.descriptor("diamonds")));
        final var mc = ctx.wrapMathContext(MathContext.DECIMAL64, Descriptor.descriptor("mc"));
        final var y = ctx.wrapBigDecimal(new BigDecimal("0.5"), Descriptor.descriptor("y"));
        var x = ctx.wrapBigDecimal(BigDecimal.ONE, Descriptor.descriptor("x"));
        for (int i = 0; i < 60; i++) {
            x = x.multiply(y, mc, null).add(x.add(y, mc, null), mc, null);
        }
        final var diamonds = new SnapshotNavigator(ctx.snapshot());
        final var last = x.getVariableTrack().getId();

        assertEquals(180, diamonds.producedByDeep(last).size());
        assertEquals(180, diamonds.producesDeep("i_3").size());
        assertEquals(183, diamonds.cpgOf(last, Set.of()).variables().size());
    }

    @Test
    void usageExample() {
        final var ciddorVar = nav.findSingleVariable(v->v.track().getDescriptor().getName().contains("Ciddor"));
        final var thermallyCorrectdLenVar = nav.findSingleVariable(v->v.track().getDescriptor().getName().contains("thermally corrected"));
        final var subgraph = nav.cpgOf(thermallyCorrectdLenVar.track().getId(), Set.of(ciddorVar.track().getId()));
        assertEquals(8, subgraph.operations().size());
        assertEquals(18, subgraph.variables().size());
        System.out.println(env.toJson(subgraph));
    }
}
