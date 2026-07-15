package io.compprov.examples.physics;

import io.compprov.core.ComputationEnvironment;
import io.compprov.core.DataContext;
import io.compprov.core.DefaultComputationContext;
import io.compprov.core.DefaultComputationEnvironment;
import io.compprov.core.Subgraph;
import io.compprov.core.wrappers.WrappedBigDecimal;
import io.compprov.core.wrappers.WrappedBigDecimals;
import io.compprov.core.wrappers.WrappedMathContext;
import io.compprov.core.wrappers.primitive.WrappedInteger;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.math.MathContext;
import java.nio.file.Files;
import java.util.List;

import static io.compprov.core.meta.Descriptor.descriptor;

/**
 * A physically meaningful stress-test simulating an object's free fall
 * through an atmosphere with non-linear aerodynamic drag.
 * <p>
 * Each iteration integrates the equations of motion over a time step dt,
 * producing roughly 20 interconnected operations per step.
 * Uses subgraph folding to condense the system of atmospheric integration equations
 * into a single reusable template invocation per step.
 * <p>
 * This class exists primarily to demonstrate a specific graph-folding pattern:
 * <b>retrieving multiple values from a folded subgraph's result</b>. A {@code Subgraph}
 * already accepts any number of inputs natively (its {@code argumentIds} is a list), but
 * one {@code execute()} call can only ever produce a single result variable, since
 * {@code resultId} is singular. When a step's output is inherently multi-valued — here,
 * the next altitude {@code y_next} and velocity {@code v_next} — those values are packed
 * into a single {@link WrappedBigDecimals} array via {@code WrappedBigDecimal.array(...)},
 * and read back out with {@code WrappedBigDecimals.extract(index, ...)} wherever an
 * individual component is needed.
 * <p>
 * Note that the subgraph's <i>input</i> ({@code y_v_in}) is packed the same way, even though
 * that isn't strictly necessary — {@code execute(List.of(y, v), ...)} could take the two
 * scalars directly. It's packed anyway so the driving loop in {@link #calculateCompProv} can
 * feed one iteration's packed result straight in as the next iteration's single argument,
 * without an extra unpack/repack round-trip between iterations.
 * <p>
 * See {@link #integrationStep} for the unpack/repack, and {@link #calculateCompProv} for
 * how the packed array is threaded through the per-step subgraph invocation loop.
 */
public class AtmosphericDragDescentStress {

    private static final ComputationEnvironment ENVIRONMENT = DefaultComputationEnvironment.create();

    // Physical Constants
    private static final BigDecimal RHO_0 = new BigDecimal("1.225"); // Sea-level air density (kg/m^3)
    private static final BigDecimal H = new BigDecimal("8500.0");    // Atmospheric scale height (m)
    private static final BigDecimal C_D = new BigDecimal("0.47");    // Drag coefficient (sphere)
    private static final BigDecimal A = new BigDecimal("0.1");       // Cross-sectional area (m^2)
    private static final BigDecimal MASS = new BigDecimal("80.0");    // Mass of the object (kg)
    private static final BigDecimal G = new BigDecimal("9.81");       // Gravitational acceleration (m/s^2)
    private static final BigDecimal DT = new BigDecimal("0.1");       // Time step dt (s)

    // Initial state
    private static final BigDecimal INIT_Y = new BigDecimal("30000.0"); // Starting at 30km altitude
    private static final BigDecimal INIT_V = BigDecimal.ZERO;           // Terminal speed starts from 0

    //@Test skipped, too huge for automatic testing
    public void test() throws Exception {
        final var step = 1000;
        final var totalIterationsMax = 250000;

        // Warm up
        BigDecimal warmUpSteps = BigDecimal.valueOf(totalIterationsMax);
        calculatePure(warmUpSteps);
        calculateCompProv(warmUpSteps);

        System.out.println("N\tPure time\tCompProv time\tCPG memory");
        for (int iterations = 10000; iterations <= totalIterationsMax; iterations += step) {
            BigDecimal iterBD = BigDecimal.valueOf(iterations);
            final var pureTime = calculatePure(iterBD);

            System.gc();
            System.runFinalization();
            Thread.sleep(1000);
            final var compProvTime = calculateCompProv(iterBD);

            final var cpgSize = Files.size(new File("physics_descent.json").toPath());
            System.out.println("%s\t%s\t%s\t%s".formatted(iterations, pureTime, compProvTime, cpgSize));
        }
    }

    public long calculatePure(BigDecimal totalStepsBD) {
        long nano = System.nanoTime();

        final var mc = MathContext.DECIMAL128;
        final var half = new BigDecimal("0.5");
        final var minusG = G.negate();

        var y = INIT_Y;
        var v = INIT_V;

        final long steps = totalStepsBD.longValue();
        for (long i = 0; i < steps; i++) {
            // rho = rho_0 * exp(-y / H)
            final var exponent = y.divide(H, mc).negate();
            // Approximated exponential for BigDecimal using Taylor or double conversion for simplicity in pure logic
            final var rho = RHO_0.multiply(BigDecimal.valueOf(Math.exp(exponent.doubleValue())), mc);

            // F_drag = 0.5 * rho * v^2 * Cd * A
            final var vSquared = v.pow(2, mc);
            final var fDrag = half.multiply(rho, mc)
                    .multiply(vSquared, mc)
                    .multiply(C_D, mc)
                    .multiply(A, mc);

            // a = -g + F_drag / m
            final var a = minusG.add(fDrag.divide(MASS, mc), mc);

            // v_next = v + a * dt
            final var vNext = v.add(a.multiply(DT, mc), mc);

            // y_next = y + v * dt + 0.5 * a * dt^2
            final var dtSquared = DT.pow(2, mc);
            final var yNext = y.add(v.multiply(DT, mc), mc)
                    .add(half.multiply(a, mc).multiply(dtSquared, mc), mc);

            y = yNext;
            v = vNext;
        }

        return System.nanoTime() - nano;
    }

    public long calculateCompProv(BigDecimal totalStepsBD) throws IOException {
        long nano = System.nanoTime();

        final var mc128 = MathContext.DECIMAL128;

        // Subgraph: Defines the physics integration equations for a single time step
        final var subctx = new DefaultComputationContext(ENVIRONMENT,
                new DataContext(descriptor("Aerodynamic Integration Step Template")));
        {
            final var int0 = subctx.wrapInteger(0, descriptor("Constant 0"));
            final var int1 = subctx.wrapInteger(1, descriptor("Constant 1"));
            // Packed as an array for symmetry with y_v_next below (see class javadoc) --
            // execute() could just as well take y and v as two separate arguments.
            final var y_v_in = subctx.wrapBigDecimals(new BigDecimal[]{BigDecimal.ZERO, BigDecimal.ZERO}, descriptor("y_v_in"));
            final var yIn = y_v_in.extract(int0, descriptor("y_in"));
            final var vIn = y_v_in.extract(int1, descriptor("v_in"));
            final var mc = subctx.wrapMathContext(mc128, descriptor("precision"));

            // Subgraph constants
            final var rho0 = subctx.wrapBigDecimal(RHO_0, descriptor("rho_0"));
            final var scaleH = subctx.wrapBigDecimal(H, descriptor("scale_height"));
            final var half = subctx.wrapBigDecimal(new BigDecimal("0.5"), descriptor("0.5"));
            final var cd = subctx.wrapBigDecimal(C_D, descriptor("C_d"));
            final var area = subctx.wrapBigDecimal(A, descriptor("Area"));
            final var minusG = subctx.wrapBigDecimal(G.negate(), descriptor("-g"));
            final var mass = subctx.wrapBigDecimal(MASS, descriptor("Mass"));
            final var dt = subctx.wrapBigDecimal(DT, descriptor("dt"));
            final var twoInt = subctx.wrapInteger(2, descriptor("2"));

            integrationStep(yIn, vIn, rho0, scaleH, half, cd, area, minusG, mass, dt, twoInt, mc);
        }

        // Main context
        final var ctx = new DefaultComputationContext(ENVIRONMENT,
                new DataContext(descriptor("Atmospheric Descent Simulation with %s steps".formatted(totalStepsBD))));

        final var totalSteps = ctx.wrapBigDecimal(totalStepsBD, descriptor("Total steps"));

        // Global simulation variables
        var y_v = ctx.wrapBigDecimals(new BigDecimal[]{INIT_Y, INIT_V}, descriptor("y_v_0"));

        // Retrieve subgraph mapping input (y_in, v_in) -> output (y_next, v_next)
        final var stepSubgraph = ctx.wrapSubgraph(
                new Subgraph(
                        subctx,
                        List.of(subctx.findSingleVariable("y_v_in").getVariableTrack().getId()),
                        subctx.findSingleVariable("y_v_next").getVariableTrack().getId()
                ),
                subctx.descriptor()
        );

        final long steps = totalSteps.getValue().longValue();
        for (long i = 0; i < steps; i++) {
            y_v = (WrappedBigDecimals) stepSubgraph.execute(List.of(y_v), descriptor("integration_step_" + i));
        }

        nano = System.nanoTime() - nano;

        // Serialize execution metadata and graph
        final var fos = new FileOutputStream("physics_descent.json");
        final var snapshot = ctx.snapshot();
        ENVIRONMENT.toJson(snapshot, fos);
        fos.close();

        return nano;
    }

    /**
     * Executes the system of differential equations representing the physical kinematics
     * of aerodynamic drag in a single step.
     */
    private static WrappedBigDecimals integrationStep(WrappedBigDecimal y, WrappedBigDecimal v,
                                                      WrappedBigDecimal rho0, WrappedBigDecimal scaleH,
                                                      WrappedBigDecimal half, WrappedBigDecimal cd,
                                                      WrappedBigDecimal area, WrappedBigDecimal minusG,
                                                      WrappedBigDecimal mass, WrappedBigDecimal dt,
                                                      WrappedInteger twoInt, WrappedMathContext mc) {

        // 1. exponent = -y / H
        final var exponent = y.divide(scaleH, mc, descriptor("y/H")).negate(mc, descriptor("-y/H"));

        // 2. rho = rho0 * exp(exponent)
        final var expVal = exponent.expDouble(descriptor("exp(-y/H)"));
        final var rho = rho0.multiply(expVal, mc, descriptor("rho"));

        // 3. F_drag = 0.5 * rho * v^2 * Cd * A
        final var vSquared = v.pow(twoInt, mc, descriptor("v^2"));
        final var fDrag = half.multiply(rho, mc, descriptor("0.5*rho"))
                .multiply(vSquared, mc, descriptor("0.5*rho*v^2"))
                .multiply(cd, mc, descriptor("0.5*rho*v^2*Cd"))
                .multiply(area, mc, descriptor("F_drag"));

        // 4. a = -g + F_drag / m
        final var dragAcceleration = fDrag.divide(mass, mc, descriptor("F_drag/m"));
        final var a = minusG.add(dragAcceleration, mc, descriptor("a"));

        // 5. v_next = v + a * dt
        final var vNext = v.add(a.multiply(dt, mc, descriptor("a*dt")), mc, descriptor("v_next"));

        // 6. y_next = y + v * dt + 0.5 * a * dt^2
        final var dtSquared = dt.pow(twoInt, mc, descriptor("dt^2"));
        final var linearDisp = v.multiply(dt, mc, descriptor("v*dt"));
        final var accelDisp = half.multiply(a, mc, descriptor("0.5*a"))
                .multiply(dtSquared, mc, descriptor("0.5*a*dt^2"));

        final var yNext = y.add(linearDisp, mc, descriptor("y+v*dt"))
                .add(accelDisp, mc, descriptor("y_next"));

        // A subgraph has exactly one result slot, so the two outputs (y_next, v_next) MUST be
        // packed into a single WrappedBigDecimals array to be returned as one result variable.
        return yNext.array(List.of(vNext), descriptor("y_v_next"));
    }
}