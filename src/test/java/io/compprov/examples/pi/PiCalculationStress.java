package io.compprov.examples.pi;

import io.compprov.core.ComputationEnvironment;
import io.compprov.core.DataContext;
import io.compprov.core.DefaultComputationContext;
import io.compprov.core.DefaultComputationEnvironment;
import io.compprov.core.Subgraph;
import io.compprov.core.wrappers.WrappedBigDecimal;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.math.MathContext;
import java.nio.file.Files;
import java.util.List;
import java.util.Random;

import static io.compprov.core.meta.Descriptor.descriptor;
import static java.math.RoundingMode.DOWN;

public class PiCalculationStress {

    private static final ComputationEnvironment ENVIRONMENT = DefaultComputationEnvironment.create();
    private static final String ACTUAL_PI = "3.14159265358979323846";
    private static final Random random = new Random();

    //@Test skipped, too huge for automatic testing
    public void test() throws Exception {

        int step = 1000;
        int totalPointsMax = 250000;

        //warm up (heap memory allocation etc)
        BigDecimal warmUpAmount = BigDecimal.valueOf(totalPointsMax);
        calculatePure(warmUpAmount);
        calculateCompProv(warmUpAmount);
        calculateCompProvWithFragmentation(warmUpAmount);

        System.out.println("N\tJPC time\tCompProv time\tCompProv frag time\tCPG memory\tCPG frag memory");
        for (int tp = 10000; tp <= totalPointsMax; tp += step) {
            System.gc();
            System.runFinalization();
            Thread.sleep(1000);
            BigDecimal tpBD = BigDecimal.valueOf(tp);
            long pureTime = calculatePure(tpBD);
            long compProvTime = calculateCompProv(tpBD);
            long compProvTimeFragmentation = calculateCompProvWithFragmentation(tpBD);
            long cpgSize = Files.size(new File("pi.json").toPath());
            long cpgFragSize = Files.size(new File("pi_frag.json").toPath());
            System.out.println("%s\t%s\t%s\t%s\t%s\t%s".formatted(tp, pureTime, compProvTime, compProvTimeFragmentation, cpgSize, cpgFragSize));
        }
    }

    public long calculatePure(BigDecimal totalPointsBD) {

        long nano = System.nanoTime();

        var mc = MathContext.DECIMAL128;
        var four = BigDecimal.valueOf(4);
        var two = 2;
        var one = BigDecimal.ONE;
        var pi = new BigDecimal(ACTUAL_PI);

        int counter = 0;
        for (long i = 0; i < totalPointsBD.longValue(); i++) {

            final var x = BigDecimal.valueOf(random.nextDouble());
            final var y = BigDecimal.valueOf(random.nextDouble());

            // (x^2 + y^2) <= 1 - in circle, counter should be increased
            final var xSquared = x.pow(two, mc);
            final var ySquared = y.pow(two, mc);
            final var dist = xSquared.add(ySquared, mc);
            if (dist.compareTo(one) <= 0) {
                counter++;
            }
        }

        // (pointsInsideBD / totalPointsBD)
        final var ratio = BigDecimal.valueOf(counter).divide(totalPointsBD, mc);

        // ratio * 4
        final var estimatedPi = ratio.multiply(four, mc);
        final var error = estimatedPi.subtract(pi, mc).abs(mc);

        return System.nanoTime() - nano;
    }

    public long calculateCompProv(BigDecimal totalPointsBD) throws IOException {

        long nano = System.nanoTime();
        var ctx = new DefaultComputationContext(ENVIRONMENT,
                new DataContext(descriptor("Pi calculation with %s points".formatted(totalPointsBD))));
        var mc = ctx.wrapMathContext(MathContext.DECIMAL128, descriptor("computation precision"));
        var roundingMc = ctx.wrapMathContext(new MathContext(0, DOWN), descriptor("0/1 math-context"));
        var totalPoints = ctx.wrapBigDecimal(totalPointsBD, descriptor("Total points"));
        var four = ctx.wrapBigDecimal(BigDecimal.valueOf(4), descriptor("constant 4"));
        var two = ctx.wrapInteger(2, descriptor("constant 2 integer"));
        var one = ctx.wrapBigDecimal(BigDecimal.ONE, descriptor("constant 1"));
        var pi = ctx.wrapBigDecimal(new BigDecimal(ACTUAL_PI), descriptor("actual Pi"));

        var counter = ctx.wrapBigDecimal(BigDecimal.ZERO, descriptor("initial counter"));
        for (long i = 0; i < totalPoints.getValue().longValue(); i++) {

            final var x = ctx.wrapBigDecimal(BigDecimal.valueOf(random.nextDouble()), descriptor("x_" + i));
            final var y = ctx.wrapBigDecimal(BigDecimal.valueOf(random.nextDouble()), descriptor("y_" + i));

            // (x^2 + y^2) <= 1 - in circle, counter should be increased
            final var xSquared = x.pow(two, mc, descriptor("x_%s^2".formatted(i)));
            final var ySquared = y.pow(two, mc, descriptor("y_%s^2".formatted(i)));
            final var dist = xSquared.add(ySquared, mc, descriptor("x_%s^2 + y_%s^2".formatted(i, i)));
            final var distRound = dist.setScale(roundingMc, descriptor("0/1 distance " + i));
            final var inCircle = one.subtract(distRound, mc, descriptor("inCircle " + i));

            //count in-circle cases
            counter = counter.add(inCircle, mc, descriptor("counter " + i));
        }

        // (pointsInsideBD / totalPointsBD)
        final var ratio = counter.divide(totalPoints, mc, descriptor("ratio"));

        // ratio * 4
        final var estimatedPi = ratio.multiply(four, mc, descriptor("estimated Pi"));
        final var error = estimatedPi.subtract(pi, mc, descriptor("error")).abs(mc, descriptor("|error|"));

        nano = System.nanoTime() - nano;

        FileOutputStream fos = new FileOutputStream("pi.json");
        final var snapshot = ctx.snapshot();
        int nodes = snapshot.variables().size() + snapshot.operations().size();
        int edges = snapshot.operations().size() + snapshot.operations().stream().mapToInt(operation -> operation.arguments().size()).sum();
        ENVIRONMENT.toJson(snapshot, fos);
        fos.close();
        return nano;
    }

    public long calculateCompProvWithFragmentation(BigDecimal totalPointsBD) throws IOException {

        long nano = System.nanoTime();

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
        var totalPoints = ctx.wrapBigDecimal(totalPointsBD, descriptor("Total points"));
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

        nano = System.nanoTime() - nano;

        FileOutputStream fos = new FileOutputStream("pi_frag.json");
        final var snapshot = ctx.snapshot();
        int nodes = snapshot.variables().size() + snapshot.operations().size();
        int edges = snapshot.operations().size() + snapshot.operations().stream().mapToInt(operation -> operation.arguments().size()).sum();
        ENVIRONMENT.toJson(snapshot, fos);
        fos.close();
        return nano;
    }
}
