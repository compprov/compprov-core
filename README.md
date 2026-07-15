# compprov-core

**compprov** (Computational Provenance) is a Java framework that automatically builds a
**Calculation Provenance Graph (CPG)** — a DAG that records every variable and every operation
in a computation as it runs. The result is a complete, machine-readable audit trail of how each
output was derived from its inputs.

---

## Contents

- [Core concepts](#core-concepts)
- [Getting started](#getting-started)
- [Usage example](#usage-example)
- [Snapshot: export, replay, and diff](#snapshot-export-replay-and-diff)
- [Subgraph folding: scaling cyclic computations](#subgraph-folding-scaling-cyclic-computations)
- [Extending with custom type wrappers](#extending-with-custom-type-wrappers)
- [Built-in types](#built-in-types)
- [Thread safety](#thread-safety)
- [Visualization](#visualization)
- [Examples](#examples)
- [License](#license)

---

## Core concepts

| Concept | Description |
|---|---|
| **CPG** | Directed Acyclic Graph where nodes are variables and edges are data-flow dependencies. Produced automatically during execution. |
| **Snapshot** | Immutable, point-in-time capture of all variables and operations recorded in a context. Can be serialized to JSON, replayed, or compared. |
| **Descriptor** | Name + optional metadata (`Meta`) attached to a variable or operation. Used in logs, diff reports, and audit trails. |
| **VariableWrapper** | Factory that converts a plain value into a provenance-tracked `WrappedVariable` and registers it in the active context. |
| **ComputationEnvironment** | Shared, thread-safe configuration: registered wrappers, clock, Jackson mapper, descriptor enforcement rules. |
| **ComputationContext** | Per-computation scope that accumulates the CPG. Not safe to snapshot while mutating. |

---

## Getting started

Add the dependency to your `pom.xml` (latest version: [![Maven Central](https://img.shields.io/maven-central/v/io.compprov/compprov-core?color=brightgreen)](https://central.sonatype.com/artifact/io.compprov/compprov-core)):

```xml
<dependency>
    <groupId>io.compprov</groupId>
    <artifactId>compprov-core</artifactId>
    <version>VERSION</version>
</dependency>
```

Requires Java 17+.

---

## Usage example

The entry point is `DefaultComputationEnvironment` (preconfigured with all built-in wrappers
and Jackson serializers) and `DefaultComputationContext` (typed convenience wrappers on top
of the base context).

```java
import io.compprov.core.*;
import io.compprov.core.meta.Descriptor;
import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;

// --- 1. Create the environment (thread-safe; reuse across computations) ---
var env = DefaultComputationEnvironment.create();

// --- 2. Create a context for this computation run ---
var ctx = new DefaultComputationContext(
        env,
        new DataContext(Descriptor.descriptor("invoice-calculation")));

// --- 3. Wrap all inputs ---
// Every wrapped value gets a unique ID and is recorded in the CPG as an INPUT node.
var mc      = ctx.wrapMathContext(new MathContext(10, RoundingMode.HALF_UP),
                                  Descriptor.descriptor("mc"));
var price   = ctx.wrapBigDecimal(new BigDecimal("100.00"),
                                 Descriptor.descriptor("price"));
var taxRate = ctx.wrapBigDecimal(new BigDecimal("0.08"),
                                 Descriptor.descriptor("tax-rate"));

// --- 4. Perform operations ---
// Each call records an operation node in the CPG and returns a wrapped result.
// Pass null as the last argument to let the framework auto-name the result.
var tax   = price.multiply(taxRate, mc, Descriptor.descriptor("tax"));
var total = price.add(tax, mc, Descriptor.descriptor("total"));

// --- 5. Read the result like any other value ---
System.out.println(total.getValue()); // 108.0000000

// --- 6. Export the full Calculation Provenance Graph ---
Snapshot snapshot = ctx.snapshot();
System.out.println(env.toJson(snapshot));
System.out.println(env.toHumanReadableLog(snapshot));
```

### JSON output (abbreviated)

```json
{
  "descriptor" : { "name" : "invoice-calculation", "meta" : { } },
  "variables" : [
    { "track" : { "id" : "i_1", "descriptor" : { "name" : "mc" }, ... }, "value" : ... },
    { "track" : { "id" : "i_2", "descriptor" : { "name" : "price" }, ... }, "value" : "100.00" },
    { "track" : { "id" : "i_3", "descriptor" : { "name" : "tax-rate" }, ... }, "value" : "0.08" },
    { "track" : { "id" : "o_4", "descriptor" : { "name" : "tax" }, ... }, "value" : "8.000000000" },
    { "track" : { "id" : "o_5", "descriptor" : { "name" : "total" }, ... }, "value" : "108.0000000" }
  ],
  "operations" : [
    { "track" : { "id" : "o_1", "descriptor" : { "name" : "multiply" }, ... },
      "arguments" : { "a" : "i_2", "b" : "i_3", "mc" : "i_1" },
      "resultId" : "o_4" },
    { "track" : { "id" : "o_2", "descriptor" : { "name" : "add" }, ... },
      "arguments" : { "a" : "i_2", "b" : "o_4", "mc" : "i_1" },
      "resultId" : "o_5" }
  ]
}
```

Variable IDs use the prefix `i_` for inputs and `o_` for outputs, followed by a sequential
numeric counter that is stable within a single context run.

---

## Snapshot: export, replay, and diff

### Serialize and deserialize

```java
String json = env.toJson(ctx.snapshot());

// Deserialize back to a Snapshot
Snapshot restored = env.fromJson(json);
```

### Replay a computation

`env.compute()` replays all recorded operations against the given snapshot,
producing a new context with freshly computed outputs:

```java
var replayed = env.compute(restored);
BigDecimal replayedTotal = (BigDecimal) replayed.getVariable("o_5").getValue();
```

### Change an input and propagate

Use `copyWith` to substitute one or more input values, then replay:

```java
Snapshot modified = env.copyWith(
        restored,
        Descriptor.descriptor("invoice-calculation-v2"),
        Map.of("i_3", new ValueWithDescriptor(
                Descriptor.descriptor("tax-rate"),
                new BigDecimal("0.10"))));  // 10% tax instead of 8%

var updated = env.compute(modified);
// updated.getVariable("o_5") now reflects the new total
```

---

## Subgraph folding: scaling cyclic computations

A CPG that faithfully records every operation is what makes compprov useful for auditing —
but for cyclic algorithms (Monte Carlo simulations, iterative solvers, time-series walks)
that repeat the *same* computation shape thousands or millions of times, that fidelity has
a cost: every iteration adds its own operations and intermediate variables to the graph, so
both live heap usage and exported snapshot size grow with iteration count **times** the
number of tracked steps per iteration.

**Subgraph folding** (implemented as `Subgraph` / `WrappedSubgraph`:
define the repeated step once as a reusable template, then replay it per cycle as a single
 operation instead of re-recording its internal steps every time.

### Why it matters

`io.compprov.examples.pi.PiCalculationStress` benchmarks a Monte Carlo Pi estimator (5
tracked operations per sampled point: `pow`, `pow`, `add`, `setScale`, `subtract`) with and
without folding. At 250,000 points, the exported JSON snapshot for the un-folded run was
**1.6 GB**; the folded run produced an identical calculation in **628 MB** — about **2.5x
smaller** — because each iteration contributes one `execute` operation and one result
variable to the graph instead of five of each. The saving scales with the number of steps
captured in the template: a repeated step with 20 internal operations would save roughly
20x per iteration instead of 5x.

The same reduction applies to memory while the computation is running: `ComputationContext`
keeps every recorded variable and operation in memory until `snapshot()` is called, so fewer
recorded nodes per cycle means a smaller live heap footprint, not just a smaller export.

### How it works

1. **Build the template once**, normally, in its own disposable `ComputationContext`. This
   produces a small, self-contained CPG for a single execution of the repeated step.
2. **Capture it as a `Subgraph`**: `new Subgraph(templateCtx, argumentIds, resultId)` records
   which of the template's variable ids are inputs and which one is the output.
3. **Wrap it once** in the context that drives the cycle: `ctx.wrapSubgraph(subgraph,
   descriptor)`. This adds exactly one variable to the outer CPG, no matter how many times
   it's later invoked.
4. **Execute it per cycle**: `subgraphVar.execute(List.of(arg1, arg2, ...), resultDescriptor)`.
   Internally this replays the template's function chain against the new argument values
   entirely in memory — no new tracking metadata is created for the internal steps. Only the
   outer `execute` call is recorded in `ctx`, as one operation producing one result variable.

```java
var env = DefaultComputationEnvironment.create();

// 1. Build the repeated step ONCE, in its own throwaway context.
var templateCtx = new DefaultComputationContext(env,
        new DataContext(Descriptor.descriptor("Pi calculation step with x,y points")));

var x   = templateCtx.wrapBigDecimal(BigDecimal.ZERO, Descriptor.descriptor("x"));
var y   = templateCtx.wrapBigDecimal(BigDecimal.ZERO, Descriptor.descriptor("y"));
var mc  = templateCtx.wrapMathContext(MathContext.DECIMAL128, Descriptor.descriptor("computation precision"));
var rmc = templateCtx.wrapMathContext(new MathContext(0, RoundingMode.DOWN), Descriptor.descriptor("0/1 math-context"));
var one = templateCtx.wrapBigDecimal(BigDecimal.ONE, Descriptor.descriptor("constant 1"));
var two = templateCtx.wrapInteger(2, Descriptor.descriptor("constant 2 integer"));

var xSquared  = x.pow(two, mc, Descriptor.descriptor("x^2"));
var ySquared  = y.pow(two, mc, Descriptor.descriptor("y^2"));
var dist      = xSquared.add(ySquared, mc, Descriptor.descriptor("x^2 + y^2"));
var distRound = dist.setScale(rmc, Descriptor.descriptor("0/1 distance"));
var inCircle  = one.subtract(distRound, mc, Descriptor.descriptor("inCircle"));

// 2. Capture it as a reusable Subgraph: declare which variables are inputs (by id)
//    and which one is the result.
var ctx = new DefaultComputationContext(env,
        new DataContext(Descriptor.descriptor("Pi calculation with 100000 points")));

var piStep = ctx.wrapSubgraph(
        new Subgraph(
                templateCtx,
                List.of(x.getVariableTrack().getId(), y.getVariableTrack().getId()),
                inCircle.getVariableTrack().getId()),
        Descriptor.descriptor("Pi calculation step"));

// 3. Drive the cycle. Each call replays the 5-step template in memory and records
//    exactly ONE "execute" operation + ONE result variable in `ctx` — not the five
//    operations and five intermediates the un-folded version would add per iteration.
var counter = ctx.wrapBigDecimal(BigDecimal.ZERO, Descriptor.descriptor("initial counter"));
for (long i = 0; i < totalPoints; i++) {
    var xi = ctx.wrapBigDecimal(BigDecimal.valueOf(random.nextDouble()), Descriptor.descriptor("x_" + i));
    var yi = ctx.wrapBigDecimal(BigDecimal.valueOf(random.nextDouble()), Descriptor.descriptor("y_" + i));
    var inCircleI = (WrappedBigDecimal) piStep.execute(List.of(xi, yi), Descriptor.descriptor("inCircle_" + i));
    counter = counter.add(inCircleI, mc, Descriptor.descriptor("counter_" + i));
}
```

See the full runnable version in `io.compprov.examples.pi.PiCalculator` (`calculate()`), and
the side-by-side benchmark in `io.compprov.examples.pi.PiCalculationStress`.

The same folding pattern also handles integrands where rejection sampling doesn't apply — see
`io.compprov.examples.pi.MonteCarloArcsineIntegrationStress`, which estimates `pi` via the
average-value method on `f(x) = 1/sqrt(1-x^2)` (unbounded as `x -> 1`, so no finite bounding box
exists for rejection sampling).

### Concurrency

`WrappedSubgraph` exposes two ways to invoke the folded template, trading memory for
parallelism:

- **`execute(args, resultDescriptor)`** replays the template against a single `MutableState`
  that's allocated once, when the `Subgraph` is built, and reused for every call. Concurrent
  calls are safe — they synchronize on that shared state — but fully serialized, so calling it
  from multiple threads gives no speedup. This is the cheapest option and the right default for
  a single-threaded driving loop, like the Pi example above.
- **`executeConcurrent(args, resultDescriptor)`** allocates a fresh `MutableState` — a full copy
  of the template's intermediate variables — for every call, so independent invocations never
  contend on shared state and can run truly in parallel. The tradeoff is one extra
  allocation-and-copy per call. See `io.compprov.examples.pi.MonteCarloArcsineIntegrationStressParallel`
  for a driving loop that submits samples to an `ExecutorService` and calls `executeConcurrent()`
  from multiple threads.

### Reproducing intermediate steps

Folding means `execute()` / `executeConcurrent()` deliberately don't record the template's
internal steps in the outer CPG — that's the point. When you need to inspect one specific call
in full detail (debugging, audit, a spot-check on a suspicious result), reconstruct it as its
own independent CPG using `extractSubgraph()`, `copyWith`, and `compute`:

```java
// 1. Export the template as a standalone snapshot: its full operation chain (xSquared,
//    ySquared, dist, distRound, inCircle) with the placeholder values it was built with.
Snapshot templateSnapshot = piStep.extractSubgraph();

// 2. Substitute the template's INPUT variables with the actual arguments from the call you
//    want to inspect — here, the x_42/y_42 point from the loop above.
Snapshot callSnapshot = env.copyWith(
        templateSnapshot,
        Descriptor.descriptor("Pi step replay for point 42"),
        Map.of(
                x.getVariableTrack().getId(), new ValueWithDescriptor(
                        Descriptor.descriptor("x"), xi.getValue()),
                y.getVariableTrack().getId(), new ValueWithDescriptor(
                        Descriptor.descriptor("y"), yi.getValue())));

// 3. Replay it. This produces a fresh ComputationContext with every intermediate variable
//    tracked, exactly as if folding had never happened for this one call.
ComputationContext replay = env.compute(callSnapshot);
System.out.println(env.toHumanReadableLog(replay.snapshot()));
```

`x` and `y` here are the same template-context variables used to build the `Subgraph`'s
`argumentIds` — `copyWith` only accepts substitutions for `INPUT`-kind variables, which is
exactly what those are.

```java
// Multiple threads calling the SAME WrappedSubgraph instance concurrently:
IntStream.range(0, totalPoints).parallel().forEach(i -> {
    var xi = ctx.wrapBigDecimal(BigDecimal.valueOf(random.nextDouble()), Descriptor.descriptor("x_" + i));
    var yi = ctx.wrapBigDecimal(BigDecimal.valueOf(random.nextDouble()), Descriptor.descriptor("y_" + i));
    piStep.executeConcurrent(List.of(xi, yi), Descriptor.descriptor("inCircle_" + i));
});
```

`ctx.wrapBigDecimal(...)` and `ctx.wrapSubgraph(...)` are themselves thread-safe (see
[Thread safety](#thread-safety)), so the outer driving context needs no extra synchronization
either way — the `execute` vs. `executeConcurrent` choice only affects the subgraph replay itself.

---

## Extending with custom type wrappers

Adding support for a type not built into the framework requires three things:

1. A `Wrapped<Type>` class that defines the tracked operations for your type.
2. A `VariableWrapper<Type>` factory that instantiates it.
3. Registering the factory with the environment.

### Step 1 — Create the wrapped class

Below, `Amount` is a currency-aware value type (a `Currency` plus a `BigDecimal`) from a
net-asset-value example: `add()` requires both amounts to share a currency, and `convert()`
applies an FX `Rate`. Only `add` and `convert` are shown here — a real wrapper can expose as
many operations as the underlying type needs (see `WrappedAmount` for the full version,
including a variadic `addBulk`).

```java
import io.compprov.core.ComputationContext;
import io.compprov.core.meta.Descriptor;
import io.compprov.core.variable.AbstractWrappedVariable;
import io.compprov.core.variable.VariableTrack;
import io.compprov.examples.nav.model.Amount;
import io.compprov.examples.nav.model.Rate;

import java.util.*;
import java.util.function.Function;

import static io.compprov.core.meta.Meta.formula;

public class WrappedAmount extends AbstractWrappedVariable<Amount> {

    // Define one Descriptor constant per operation; a formula makes the audit trail readable.
    private static final Descriptor OP_ADD     = Descriptor.descriptor("add", formula("a+b"));
    private static final Descriptor OP_CONVERT = Descriptor.descriptor("convert", formula("convert(a,r)"));

    // Map each Descriptor to a lambda that performs the actual computation.
    private static final Map<Descriptor, Function<List<Object>, Object>> FUNCTIONS;

    static {
        Map<Descriptor, Function<List<Object>, Object>> m = new HashMap<>();
        m.put(OP_ADD, args -> {
            Amount a = (Amount) args.get(0);
            Amount b = (Amount) args.get(1);
            return a.add(b);
        });
        m.put(OP_CONVERT, args -> {
            Amount a = (Amount) args.get(0);
            Rate r = (Rate) args.get(1);
            return a.convert(r);
        });
        FUNCTIONS = Collections.unmodifiableMap(m);
    }

    public WrappedAmount(ComputationContext context, VariableTrack track, Amount value) {
        super(context, track, value);
    }

    @Override
    public Function<List<Object>, Object> getFunction(Descriptor operationDescriptor) {
        return FUNCTIONS.get(operationDescriptor);
    }

    // --- Public API ---
    // Each operation comes in two overloads: with and without a result Descriptor.

    public WrappedAmount add(WrappedAmount augend, Descriptor resultDescriptor) {
        Objects.requireNonNull(augend, "augend");
        return (WrappedAmount) execute(OP_ADD, "a", this, "b", augend, resultDescriptor);
    }

    public WrappedAmount add(WrappedAmount augend) {
        return add(augend, null);
    }

    // Note the mixed argument type: an Amount converted by a Rate — the framework tracks
    // both as separate input variables, regardless of their concrete wrapped type.
    public WrappedAmount convert(WrappedRate rate, Descriptor resultDescriptor) {
        Objects.requireNonNull(rate, "rate");
        return (WrappedAmount) execute(OP_CONVERT, "a", this, "r", rate, resultDescriptor);
    }

    public WrappedAmount convert(WrappedRate rate) {
        return convert(rate, null);
    }
}
```

### Step 2 — Create the factory

```java
import io.compprov.core.ComputationContext;
import io.compprov.core.variable.VariableTrack;
import io.compprov.core.variable.VariableWrapper;
import io.compprov.core.variable.WrappedVariable;
import io.compprov.examples.nav.model.Amount;

public class AmountWrapper implements VariableWrapper<Amount> {
    @Override
    public WrappedVariable wrap(ComputationContext context, VariableTrack track, Amount value) {
        return new WrappedAmount(context, track, value);
    }
}
```

### Step 3 — Register and use

`Rate` is wrapped the same way (see `WrappedRate` / `RateWrapper`), so both sides of the
conversion end up as tracked variables in the CPG.

```java
var env = DefaultComputationEnvironment.create();
env.registerWrapper(Amount.class, new AmountWrapper());
env.registerWrapper(Rate.class, new RateWrapper());

var ctx = new DefaultComputationContext(env,
        new DataContext(Descriptor.descriptor("fx-conversion")));

// Use the base wrap() method — cast to your concrete type after wrapping.
var btcBalance = (WrappedAmount) ctx.wrap(
        new Amount(Currency.BTC, new BigDecimal("1.5")), Descriptor.descriptor("BTC balance"));
var btcUsdRate = (WrappedRate) ctx.wrap(
        new Rate(Currency.BTC, Currency.USD, new BigDecimal("65000.00")), Descriptor.descriptor("BTC/USD rate"));

var usdBalance = btcBalance.convert(btcUsdRate, Descriptor.descriptor("BTC->USD"));
```

If you use a custom type frequently, extend `DefaultComputationContext` to add a typed
`wrap(Amount, Descriptor)` overload, the same way `NavComputationContext` does — or the way
`DefaultComputationContext` does for `wrapBigDecimal`, `wrapBigInteger`, etc.

### Type serialization/deserialization

If custom type requires custom JSON serialization or deserialization, register a
Jackson serializer/deserializer with the `ObjectMapper` inside your custom `ComputationEnvironment`.
compprov-core runs on **Jackson 3.x** (`tools.jackson.*`), so custom deserializers implement
`tools.jackson.databind.deser.std.StdDeserializer` / `ValueDeserializer`, not the Jackson 2.x
`com.fasterxml.jackson.databind.JsonDeserializer`.
See `DefaultComputationEnvironment` for examples using `ZonedDateTimeSerializer` and
`MathContextDeserializer`, and `AmountDeserializer` for a custom-type example registered via
`environment.registerWrapper(Amount.class, new AmountWrapper(), new AmountDeserializer())`.

---

## Built-in types

`DefaultComputationEnvironment` registers the following wrappers out of the box:

| Java type | Wrapped class | Notes |
|---|---|---|
| `BigDecimal` | `WrappedBigDecimal` | Full arithmetic: add, subtract, multiply, divide, pow, sqrt, abs, negate, remainder, max, min, and more |
| `BigInteger` | `WrappedBigInteger` | Full arithmetic including modPow (ternary) |
| `Integer` | `WrappedInteger` | Parameter-only type; used as an argument to `pow`, `scaleByPowerOfTen`, etc. |
| `Long` | `WrappedLong` | Parameter-only type |
| `MathContext` | `WrappedMathContext` | Carries precision and rounding mode; passed to most `BigDecimal` / `BigInteger` ops |

---

## Thread safety

`ComputationEnvironment` and its wrappers map are fully thread-safe — a single instance can
be shared across threads and computations.

`ComputationContext` is thread-safe for all wrap and executeOperation calls. The `snapshot()`
method is **not** safe to call while other threads are still recording operations into the same
context.

`WrappedSubgraph.execute()` and `executeConcurrent()` are both safe to call concurrently on the
same instance, but with different tradeoffs — see [Concurrency](#concurrency) under Subgraph
folding.

---

## Visualization

To visualize your CPG data use [compprov-render](https://github.com/compprov/compprov-render) —
a set of HTML pages that run locally in your web browser, no server required.

Simply export a snapshot to JSON and open the page:

```java
String json = env.toJson(ctx.snapshot());
// save to a file, then open graph.html or plot.html in your browser
```

### Graph view — provenance graph

Renders the full CPG as an interactive node-edge graph.
Variables are shown as typed nodes (input / output), operations as diamond nodes with labeled argument edges.

![Provenance Graph](https://raw.githubusercontent.com/compprov/compprov-render/master/screenshots/graph.png)

### Plot view — multi-dataset comparison

Plots numeric variable values across one or more datasets side-by-side.
Supports points, line, and table views with configurable X-axis labels.

![Plot View](https://raw.githubusercontent.com/compprov/compprov-render/master/screenshots/plot.png)

---

## Examples

The `io.compprov.examples` package contains three self-contained examples that each demonstrate
a different aspect of the framework.

| Example | Package | Domain | Key technique |
|---|---|---|---|
| [Net Asset Value (NAV)](#net-asset-value-nav) | `io.compprov.examples.nav` | Crypto-portfolio accounting | Custom domain type wrappers |
| [Gauge Block Calibration](#gauge-block-calibration) | `io.compprov.examples.gaugeblock` | Precision length metrology | Pure `BigDecimal` scalar formula chain |
| [Hydrological Model Evaluation](#hydrological-model-evaluation) | `io.compprov.examples.hydrology` | River discharge modelling | List-based tracked operations |

---

### Net Asset Value (NAV)

**`io.compprov.examples.nav`** · `NetAssetValueCalculator.calculate()`

Computes the total USD value of a multi-asset crypto portfolio (BTC, ETH, USDC positions held
across Binance, staking, and Morpho DeFi) by converting each position to USD at a spot rate
and summing the results.

The primary focus is showing **how to wrap custom domain types**. The domain model uses `Amount`
and `Rate` objects rather than raw `BigDecimal`, and the example integrates them with the
framework without modifying them — using the three-step pattern:

1. **`WrappedAmount` / `WrappedRate`** extend `AbstractWrappedVariable<T>` and declare their
   operations (`add`, `convert`, `addBulk`) as `Descriptor` constants mapped to computation lambdas.
2. **`AmountWrapper` / `RateWrapper`** implement `VariableWrapper<T>` — the one-method factory
   the framework calls to instantiate tracked variables.
3. **`NavComputationContext`** extends `DefaultComputationContext`, registers both wrappers with
   the shared `ComputationEnvironment`, and exposes typed `wrap(Amount, ...)` / `wrap(Rate, ...)`
   convenience overloads.

After the calculation the snapshot is serialized to JSON, then deserialized and replayed via
`NavComputationContext.environment.compute()` — verifying that the CPG is round-trip stable and the
replayed output matches the original result.

---

### Gauge Block Calibration

**`io.compprov.examples.gaugeblock`** · `GaugeBlockCalibration.calibrate()`

Reproduces the interferometric calibration of a 7 mm tungsten carbide gauge block (NRC 91A)
from the following paper, which uses this measurement as a demonstration of metrological
provenance management:

> Ryan M. White, *Provenance in the Context of Metrological Traceability*, Metrology 2025, 5(3), 52.
> DOI: [10.3390/metrology5030052](https://doi.org/10.3390/metrology5030052)

The computation chain has three stages, all tracked in the CPG:

1. **Refractive index** — the Birch–Downs modified Ciddor equation (8 tracked steps) converts
   air temperature, pressure, relative humidity, CO₂ concentration, and saturation vapor
   pressure into the refractive index *n* of the measurement medium.
2. **Interferometric length** — the HeNe laser vacuum wavelength (632.99 nm) divided by *n*
   gives the air wavelength; the observed fringe order `m + f` gives the raw length
   `L_raw = (m + f) × λ_air / 2`.
3. **Thermal correction** — the raw length is corrected to the ISO 1 reference temperature
   (20 °C) using the tungsten carbide expansion coefficient α = 4.23 × 10⁻⁶ K⁻¹ from
   the paper: `L_cal = L_raw / (1 + α × ΔT)`.

The deviation from the 7 mm nominal length is asserted to round to **+2 nm**, matching the
paper's reported result (expanded uncertainty U = 31 nm, k = 2).

This example uses only built-in `WrappedBigDecimal` arithmetic — no custom wrappers needed —
showing that the framework handles complex pure-scalar formula chains out of the box.

---

### Hydrological Model Evaluation

**`io.compprov.examples.hydrology`** · `MhmDischargeEvaluation.evaluateParameterSetP1()`

Evaluates the mesoscale Hydrologic Model (mHM) output against observed river discharge at the
Moselle River basin upstream of Perl (~11 500 km², Luxembourg/Germany), as described in:

> Villamar et al., *Archivist: a metadata management tool for facilitating FAIR research*,
> Scientific Data, 2025.
> DOI: [10.1038/s41597-025-04521-6](https://doi.org/10.1038/s41597-025-04521-6)

The metric is the **Kling-Gupta Efficiency (KGE)** (Gupta et al. 2009):

```
KGE = 1 − √[ (r−1)² + (α−1)² + (β−1)² ]

  r = Pearson correlation = Σ(devObs · devSim) / √(Σ devObs² · Σ devSim²)
  α = variability ratio  = σ_sim / σ_obs
  β = bias ratio         = μ_sim / μ_obs
```

KGE = 1 is perfect; values below 0 indicate the model is worse than the observed mean as a
predictor. The paper reports that parameter set P₁ outperforms P₂ with scores mostly above 0.5.

The computation uses `ArrayList<WrappedBigDecimal>` with loops and `addBulk`, demonstrating the
pattern for **list-based tracked operations** where the number of time steps is dynamic.
The 8-step chain (means → deviations → squared deviations and cross products → sums → r → α
→ β → KGE) is fully recorded in the CPG, with every intermediate quantity named and traceable.
The synthetic dataset is engineered so that r = 1, β = 1, α = 0.9, giving **KGE = 0.9 exactly**,
verified by exact `BigDecimal` equality.

---

## License

Apache License 2.0 — see [LICENSE](LICENSE).
