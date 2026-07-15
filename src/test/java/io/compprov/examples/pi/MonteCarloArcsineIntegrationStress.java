package io.compprov.examples.pi;

import io.compprov.core.ComputationEnvironment;
import io.compprov.core.DataContext;
import io.compprov.core.DefaultComputationContext;
import io.compprov.core.DefaultComputationEnvironment;
import io.compprov.core.Subgraph;
import io.compprov.core.wrappers.WrappedBigDecimal;
import io.compprov.core.wrappers.WrappedMathContext;
import io.compprov.core.wrappers.primitive.WrappedInteger;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.math.MathContext;
import java.nio.file.Files;
import java.util.List;
import java.util.Random;

import static io.compprov.core.meta.Descriptor.descriptor;

/**
 * Same graph-fold stress-test shape as {@link io.compprov.examples.pi.PiCalculationStress}, estimating the definite integral of
 * {@code f(x) = 1/sqrt(1-x^2)} over {@code [0, 1]}, whose antiderivative is {@code arcsin(x)}, so the exact
 * value is {@code arcsin(1) - arcsin(0) = pi/2}.
 * <p>
 * {@code f} is unbounded as {@code x} approaches 1, so rejection sampling against a bounding box doesn't apply here
 * (there is no finite {@code Y_TO}). This uses the average-value method instead:
 * {@code integral ~= (b - a) * average(f(x_i))} for x_i drawn uniformly
 * from {@code [0, 1)}. Because {@code Var(f)} is itself infinite (the singularity at x=1 is integrable but
 * {@code f^2} is not), convergence is slower and noisier than the polynomial examples.
 */
public class MonteCarloArcsineIntegrationStress {

    private static final ComputationEnvironment ENVIRONMENT = DefaultComputationEnvironment.create();
    private static final Random random = new Random();

    private static final BigDecimal LOWER_BOUND = BigDecimal.ZERO;
    private static final BigDecimal UPPER_BOUND = BigDecimal.ONE;
    private static final BigDecimal RANGE = UPPER_BOUND.subtract(LOWER_BOUND);

    private static final BigDecimal ACTUAL_PI = new BigDecimal("3.14159265358979323846");

    //@Test skipped, too huge for automatic testing
    public void test() throws Exception {

        final var step = 1000;
        final var totalPointsMax = 250000;

        //warm up (heap memory allocation etc)
        BigDecimal warmUpAmount = BigDecimal.valueOf(totalPointsMax);
        calculatePure(warmUpAmount);
        calculateCompProv(warmUpAmount);

        System.out.println("N\tJPC time\tCompProv time\tCPG memory");
        for (int tp = 10000; tp <= totalPointsMax; tp += step) {
            BigDecimal tpBD = BigDecimal.valueOf(tp);
            final var pureTime = calculatePure(tpBD);

            System.gc();
            System.runFinalization();
            Thread.sleep(1000);
            final var compProvTime = calculateCompProv(tpBD);

            final var cpgSize = Files.size(new File("integral.json").toPath());
            System.out.println("%s\t%s\t%s\t%s".formatted(tp, pureTime, compProvTime, cpgSize));
        }
    }

    public long calculatePure(BigDecimal totalPointsBD) {

        long nano = System.nanoTime();

        final var mc = MathContext.DECIMAL128;
        final var one = BigDecimal.ONE;

        var fxSum = BigDecimal.ZERO;
        for (long i = 0; i < totalPointsBD.longValue(); i++) {

            // random.nextDouble() is always < 1.0, so 1-x^2 never hits zero
            final var x = BigDecimal.valueOf(random.nextDouble());
            final var oneMinusXSquared = one.subtract(x.pow(2, mc), mc);
            final var fx = one.divide(oneMinusXSquared.sqrt(mc), mc);

            fxSum = fxSum.add(fx, mc);
        }

        final var average = fxSum.divide(totalPointsBD, mc);
        final var estimatedIntegral = average.multiply(RANGE, mc);
        final var estimatedPi = estimatedIntegral.multiply(BigDecimal.valueOf(2));
        final var error = ACTUAL_PI.subtract(estimatedPi, mc).abs(mc);

        return System.nanoTime() - nano;
    }

    public long calculateCompProv(BigDecimal totalPointsBD) throws IOException {

        long nano = System.nanoTime();

        //subgraph: a single sample, x -> 1/sqrt(1-x^2)
        final var subctx = new DefaultComputationContext(ENVIRONMENT,
                new DataContext(descriptor("Monte Carlo sample of 1/sqrt(1-x^2)")));
        {
            final var x = subctx.wrapBigDecimal(BigDecimal.ZERO, descriptor("x"));
            final var mc = subctx.wrapMathContext(MathContext.DECIMAL128, descriptor("computation precision"));
            final var one = subctx.wrapBigDecimal(BigDecimal.ONE, descriptor("Constant 1"));
            final var twoInt = subctx.wrapInteger(2, descriptor("Constant 2 (int)"));
            sampleValue(x, one, twoInt, mc, "x");
        }

        final var ctx = new DefaultComputationContext(ENVIRONMENT,
                new DataContext(descriptor("Monte Carlo integration of 1/sqrt(1-x^2) in range [0;1] with %s points".formatted(totalPointsBD))));
        final var mc = ctx.wrapMathContext(MathContext.DECIMAL128, descriptor("computation precision"));
        final var totalPoints = ctx.wrapBigDecimal(totalPointsBD, descriptor("Total points"));
        final var range = ctx.wrapBigDecimal(RANGE, descriptor("integration range"));
        final var two = ctx.wrapBigDecimal(BigDecimal.valueOf(2), descriptor("Constant 2"));
        final var pi = ctx.wrapBigDecimal(ACTUAL_PI, descriptor("Constant Pi"));

        var fxSum = ctx.wrapBigDecimal(BigDecimal.ZERO, descriptor("f(x) sum"));
        final var sampleSubgraph = ctx.wrapSubgraph(
                new Subgraph(
                        subctx,
                        List.of(subctx.findSingleVariable("x").getVariableTrack().getId()),
                        subctx.findSingleVariable("x_fx").getVariableTrack().getId()),
                subctx.descriptor());
        for (long i = 0; i < totalPoints.getValue().longValue(); i++) {

            final var x = ctx.wrapBigDecimal(BigDecimal.valueOf(random.nextDouble()), descriptor("x_" + i));
            final var fx = (WrappedBigDecimal) sampleSubgraph.execute(List.of(x), descriptor("fx_" + i));

            fxSum = fxSum.add(fx, mc, descriptor("counter " + i));
        }

        final var average = fxSum.divide(totalPoints, mc, descriptor("average f(x)"));
        final var estimatedIntegral = average.multiply(range, mc, descriptor("estimated integral"));
        // integral * 2
        final var estimatedPi = estimatedIntegral.multiply(two, mc, descriptor("estimated Pi"));
        final var error = estimatedPi.subtract(pi, mc, descriptor("error")).abs(mc, descriptor("|error|"));

        nano = System.nanoTime() - nano;

        final var fos = new FileOutputStream("integral.json");
        final var snapshot = ctx.snapshot();
        ENVIRONMENT.toJson(snapshot, fos);
        fos.close();
        return nano;
    }

    /**
     * Evaluates {@code f(x) = 1/sqrt(1-x^2)} at the given sample point.
     */
    private static WrappedBigDecimal sampleValue(WrappedBigDecimal x, WrappedBigDecimal one,
                                                 WrappedInteger twoInt,
                                                 WrappedMathContext mc, String namePrefix) {
        final var xSquared = x.pow(twoInt, mc, descriptor(namePrefix + "^2"));
        final var oneMinusXSquared = one.subtract(xSquared, mc, descriptor("1-" + namePrefix + "^2"));
        final var sqrtVal = oneMinusXSquared.sqrt(mc, descriptor("sqrt(1-" + namePrefix + "^2)"));
        return one.divide(sqrtVal, mc, descriptor(namePrefix + "_fx"));
    }
}
