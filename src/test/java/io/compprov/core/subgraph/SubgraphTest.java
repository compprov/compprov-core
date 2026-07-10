package io.compprov.core.subgraph;

import io.compprov.core.ComputationEnvironment;
import io.compprov.core.DataContext;
import io.compprov.core.DefaultComputationContext;
import io.compprov.core.DefaultComputationEnvironment;
import io.compprov.core.Subgraph;
import io.compprov.core.variable.ValueWithDescriptor;
import io.compprov.core.wrappers.WrappedBigDecimal;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.math.MathContext;
import java.util.List;
import java.util.Map;
import java.util.stream.IntStream;

import static io.compprov.core.meta.Descriptor.descriptor;
import static java.math.RoundingMode.DOWN;
import static org.junit.jupiter.api.Assertions.assertEquals;

public class SubgraphTest {

    private static final ComputationEnvironment ENVIRONMENT = DefaultComputationEnvironment.create();

    @Test
    public void executeConcurrent_isolatesEachCallUnderContention() {

        final var totalCalls = 500;

        //template: square(x) = x^2
        final var templateCtx = new DefaultComputationContext(ENVIRONMENT,
                new DataContext(descriptor("square step")));
        final var templateX = templateCtx.wrapBigDecimal(BigDecimal.ZERO, descriptor("x"));
        final var templateMc = templateCtx.wrapMathContext(MathContext.DECIMAL128, descriptor("computation precision"));
        final var templateTwo = templateCtx.wrapInteger(2, descriptor("constant 2 integer"));
        final var templateXSquared = templateX.pow(templateTwo, templateMc, descriptor("x^2"));

        final var ctx = new DefaultComputationContext(ENVIRONMENT,
                new DataContext(descriptor("concurrent squares")));
        final var squareStep = ctx.wrapSubgraph(
                new Subgraph(templateCtx, List.of(templateX.getVariableTrack().getId()), templateXSquared.getVariableTrack().getId()),
                templateCtx.descriptor());

        // Hammer the SAME WrappedSubgraph instance from multiple threads. If executeConcurrent
        // leaked state between calls (e.g. shared a MutableState like execute() does), some
        // results here would come back corrupted with another call's intermediate value.
        final var results = IntStream.range(0, totalCalls)
                .parallel()
                .mapToObj(i -> {
                    final var xi = ctx.wrapBigDecimal(BigDecimal.valueOf(i), descriptor("x_" + i));
                    final var result = (WrappedBigDecimal) squareStep.executeConcurrent(List.of(xi), descriptor("x_" + i + "^2"));
                    return result.getValue();
                })
                .toList();

        for (int i = 0; i < totalCalls; i++) {
            assertEquals(0, BigDecimal.valueOf((long) i * i).compareTo(results.get(i)),
                    "call %s produced %s".formatted(i, results.get(i)));
        }

        // folding still holds under concurrency: one "execute_concurrent" operation per call,
        // not one per internal template step.
        assertEquals(totalCalls, ctx.snapshot().operations().size());
    }

    @Test
    public void executeConcurrent_synchronizesConcurrentCalls() {

        final var totalCalls = 500;

        //template: square(x) = x^2
        final var templateCtx = new DefaultComputationContext(ENVIRONMENT,
                new DataContext(descriptor("square step")));
        final var templateX = templateCtx.wrapBigDecimal(BigDecimal.ZERO, descriptor("x"));
        final var templateMc = templateCtx.wrapMathContext(MathContext.DECIMAL128, descriptor("computation precision"));
        final var templateTwo = templateCtx.wrapInteger(2, descriptor("constant 2 integer"));
        final var templateXSquared = templateX.pow(templateTwo, templateMc, descriptor("x^2"));

        final var ctx = new DefaultComputationContext(ENVIRONMENT,
                new DataContext(descriptor("concurrent squares")));
        final var squareStep = ctx.wrapSubgraph(
                new Subgraph(templateCtx, List.of(templateX.getVariableTrack().getId()), templateXSquared.getVariableTrack().getId()),
                templateCtx.descriptor());

        // Hammer the SAME WrappedSubgraph instance from multiple threads. If executeConcurrent
        // leaked state between calls (e.g. shared a MutableState like execute() does), some
        // results here would come back corrupted with another call's intermediate value.
        final var results = IntStream.range(0, totalCalls)
                .parallel()
                .mapToObj(i -> {
                    final var xi = ctx.wrapBigDecimal(BigDecimal.valueOf(i), descriptor("x_" + i));
                    final var result = (WrappedBigDecimal) squareStep.execute(List.of(xi), descriptor("x_" + i + "^2"));
                    return result.getValue();
                })
                .toList();

        for (int i = 0; i < totalCalls; i++) {
            assertEquals(0, BigDecimal.valueOf((long) i * i).compareTo(results.get(i)),
                    "call %s produced %s".formatted(i, results.get(i)));
        }

        // folding still holds under concurrency: one "execute_concurrent" operation per call,
        // not one per internal template step.
        assertEquals(totalCalls, ctx.snapshot().operations().size());
    }

    @Test
    public void extractSubgraph_reproducesIntermediateStepsOfOneCall() {

        //template: the Pi Monte Carlo step, x,y -> inCircle (5 tracked operations)
        final var templateCtx = new DefaultComputationContext(ENVIRONMENT,
                new DataContext(descriptor("Pi calculation step with x,y points")));
        final var templateX = templateCtx.wrapBigDecimal(BigDecimal.ZERO, descriptor("x"));
        final var templateY = templateCtx.wrapBigDecimal(BigDecimal.ZERO, descriptor("y"));
        final var templateMc = templateCtx.wrapMathContext(MathContext.DECIMAL128, descriptor("computation precision"));
        final var templateRoundingMc = templateCtx.wrapMathContext(new MathContext(0, DOWN), descriptor("0/1 math-context"));
        final var templateOne = templateCtx.wrapBigDecimal(BigDecimal.ONE, descriptor("constant 1"));
        final var templateTwo = templateCtx.wrapInteger(2, descriptor("constant 2 integer"));
        final var templateXSquared = templateX.pow(templateTwo, templateMc, descriptor("x^2"));
        final var templateYSquared = templateY.pow(templateTwo, templateMc, descriptor("y^2"));
        final var templateDist = templateXSquared.add(templateYSquared, templateMc, descriptor("x^2 + y^2"));
        final var templateDistRound = templateDist.setScale(templateRoundingMc, descriptor("0/1 distance"));
        final var templateInCircle = templateOne.subtract(templateDistRound, templateMc, descriptor("inCircle"));

        final var ctx = new DefaultComputationContext(ENVIRONMENT,
                new DataContext(descriptor("Pi calculation with points")));
        final var piStep = ctx.wrapSubgraph(
                new Subgraph(
                        templateCtx,
                        List.of(templateX.getVariableTrack().getId(), templateY.getVariableTrack().getId()),
                        templateInCircle.getVariableTrack().getId()),
                templateCtx.descriptor());

        // one folded call, deliberately inside the unit circle: 0.3^2 + 0.3^2 = 0.18 <= 1
        final var x42 = ctx.wrapBigDecimal(new BigDecimal("0.3"), descriptor("x_42"));
        final var y42 = ctx.wrapBigDecimal(new BigDecimal("0.3"), descriptor("y_42"));
        final var inCircle42 = (WrappedBigDecimal) piStep.execute(List.of(x42, y42), descriptor("inCircle_42"));

        // folding held: the outer CPG only recorded ONE operation for this call, not five.
        assertEquals(1, ctx.snapshot().operations().size());

        // reproduce point 42's intermediate steps as their own independent CPG
        final var templateSnapshot = piStep.extractSubgraph();
        final var callSnapshot = ENVIRONMENT.copyWith(
                templateSnapshot,
                descriptor("Pi step replay for point 42"),
                Map.of(
                        templateX.getVariableTrack().getId(), new ValueWithDescriptor(descriptor("x"), x42.getValue()),
                        templateY.getVariableTrack().getId(), new ValueWithDescriptor(descriptor("y"), y42.getValue())));
        final var replay = ENVIRONMENT.compute(callSnapshot);

        // every intermediate step is now tracked, exactly as if folding had never happened
        assertEquals(5, replay.snapshot().operations().size());
        assertEquals(0, new BigDecimal("0.09").compareTo((BigDecimal) replay.findSingleVariable("x^2").getValue()));
        assertEquals(0, new BigDecimal("0.09").compareTo((BigDecimal) replay.findSingleVariable("y^2").getValue()));
        assertEquals(0, new BigDecimal("0.18").compareTo((BigDecimal) replay.findSingleVariable("x^2 + y^2").getValue()));
        assertEquals(0, ((BigDecimal) inCircle42.getValue())
                .compareTo((BigDecimal) replay.findSingleVariable("inCircle").getValue()));
    }
}
