package io.compprov.examples.pi;

import io.compprov.core.ComputationEnvironment;
import io.compprov.core.DataContext;
import io.compprov.core.DefaultComputationContext;
import io.compprov.core.DefaultComputationEnvironment;
import io.compprov.core.Subgraph;
import io.compprov.core.wrappers.WrappedBigDecimal;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.math.BigDecimal;
import java.math.MathContext;
import java.util.List;
import java.util.Random;

import static io.compprov.core.meta.Descriptor.descriptor;
import static java.math.RoundingMode.DOWN;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class PiCalculator {

    private static final ComputationEnvironment ENVIRONMENT = DefaultComputationEnvironment.create();
    private static final String ACTUAL_PI = "3.14159265358979323846";

    @Test
    public void calculate() {

        int totalPointsBD = 100000;
        Random random = new Random();

        //subgraph
        var subctx = new DefaultComputationContext(ENVIRONMENT,
                new DataContext(descriptor("Pi calculation step with x,y points")));
        {
            final var x = subctx.wrapBigDecimal(BigDecimal.valueOf(random.nextDouble()), descriptor("x"));
            final var y = subctx.wrapBigDecimal(BigDecimal.valueOf(random.nextDouble()), descriptor("y"));
            var mc = subctx.wrapMathContext(MathContext.DECIMAL128, descriptor("computation precision"));
            var roundingMc = subctx.wrapMathContext(new MathContext(0, DOWN), descriptor("0/1 math-context"));
            var one = subctx.wrapBigDecimal(BigDecimal.ONE, descriptor("constant 1"));
            var two = subctx.wrapInteger(2, descriptor("constant 2 integer"));
            // (x^2 + y^2) <= 1 - in circle, counter should be increased
            final var xSquared = x.pow(two, mc, descriptor("x^2"));
            final var ySquared = y.pow(two, mc, descriptor("y^2"));
            final var dist = xSquared.add(ySquared, mc, descriptor("x^2 + y^2"));
            final var distRound = dist.setScale(roundingMc, descriptor("0/1 distance"));
            final var inCircle = one.subtract(distRound, mc, descriptor("inCircle"));
        }


        var ctx = new DefaultComputationContext(ENVIRONMENT,
                new DataContext(descriptor("Pi calculation with %s points".formatted(totalPointsBD))));
        var mc = ctx.wrapMathContext(MathContext.DECIMAL128, descriptor("computation precision"));
        var totalPoints = ctx.wrapBigDecimal(BigDecimal.valueOf(totalPointsBD), descriptor("Total points"));
        var four = ctx.wrapBigDecimal(BigDecimal.valueOf(4), descriptor("constant 4"));

        var pi = ctx.wrapBigDecimal(new BigDecimal(ACTUAL_PI), descriptor("actual Pi"));

        var counter = ctx.wrapBigDecimal(BigDecimal.ZERO, descriptor("initial counter"));
        var piStepSubgraph = ctx.wrapSubgraph(
                new Subgraph(
                        subctx,
                        List.of(
                                subctx.findSingleVariable("x").getVariableTrack().getId(),
                                subctx.findSingleVariable("y").getVariableTrack().getId()
                        ),
                        subctx.findSingleVariable("inCircle").getVariableTrack().getId()),
                subctx.descriptor());
        for (long i = 0; i < totalPoints.getValue().longValue(); i++) {

            final var x = ctx.wrapBigDecimal(BigDecimal.valueOf(random.nextDouble()), descriptor("x_" + i));
            final var y = ctx.wrapBigDecimal(BigDecimal.valueOf(random.nextDouble()), descriptor("y_" + i));
            final var inCircle = (WrappedBigDecimal) piStepSubgraph.execute(List.of(x, y), descriptor("inCircle " + i));

            //count in-circle cases
            counter = counter.add(inCircle, mc, descriptor("counter " + i));
        }

        // (pointsInsideBD / totalPointsBD)
        final var ratio = counter.divide(totalPoints, mc, descriptor("ratio"));

        // ratio * 4
        final var estimatedPi = ratio.multiply(four, mc, descriptor("estimated Pi"));
        final var error = estimatedPi.subtract(pi, mc, descriptor("error")).abs(mc, descriptor("|error|"));
        assertTrue(error.getValue().compareTo(BigDecimal.valueOf(0.02)) <= 0);
    }

    @Test
    public void reproduce() throws IOException {

        final var model = PiCalculator.class.getResourceAsStream("/snapshots/pi_with_fragmentation.json").readAllBytes();
        final var snapshot = ENVIRONMENT.fromJson(model);

        //recover calculations
        final var recalculated = ENVIRONMENT.compute(snapshot);
        final var result = recalculated.findSingleVariable("estimated Pi").getValue();
        System.out.println(ENVIRONMENT.toHumanReadableLog(snapshot));

        //To reduce GH repository storage, there are just 100 cycles
        assertEquals(new BigDecimal("3.24"), result);
    }
}
