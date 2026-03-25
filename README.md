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
var env = new DefaultComputationEnvironment();

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

## Extending with custom type wrappers

Adding support for a type not built into the framework requires three things:

1. A `Wrapped<Type>` class that defines the tracked operations for your type.
2. A `VariableWrapper<Type>` factory that instantiates it.
3. Registering the factory with the environment.

### Step 1 — Create the wrapped class

```java
import io.compprov.core.ComputationContext;
import io.compprov.core.meta.Descriptor;
import io.compprov.core.variable.AbstractWrappedVariable;
import io.compprov.core.variable.VariableTrack;

import java.util.*;
import java.util.function.Function;

public final class WrappedLong extends AbstractWrappedVariable<Long> {

    // Define one Descriptor constant per operation.
    private static final Descriptor OP_ADD      = Descriptor.descriptor("add");
    private static final Descriptor OP_MULTIPLY = Descriptor.descriptor("multiply");

    // Map each Descriptor to a lambda that performs the actual computation.
    private static final Map<Descriptor, Function<List<Object>, Object>> FUNCTIONS;

    static {
        Map<Descriptor, Function<List<Object>, Object>> m = new HashMap<>();
        m.put(OP_ADD,      args -> (Long) args.get(0) + (Long) args.get(1));
        m.put(OP_MULTIPLY, args -> (Long) args.get(0) * (Long) args.get(1));
        FUNCTIONS = Collections.unmodifiableMap(m);
    }

    public WrappedLong(ComputationContext context, VariableTrack track, Long value) {
        super(context, track, value);
    }

    @Override
    public Function<List<Object>, Object> getFunction(Descriptor operationDescriptor) {
        return FUNCTIONS.get(operationDescriptor);
    }

    // --- Public API ---
    // Each operation comes in two overloads: with and without a result Descriptor.

    public WrappedLong add(WrappedLong augend, Descriptor resultDescriptor) {
        return (WrappedLong) execute(OP_ADD, "a", this, "b", augend, resultDescriptor);
    }

    public WrappedLong add(WrappedLong augend) {
        return add(augend, null);
    }

    public WrappedLong multiply(WrappedLong multiplicand, Descriptor resultDescriptor) {
        return (WrappedLong) execute(OP_MULTIPLY, "a", this, "b", multiplicand, resultDescriptor);
    }

    public WrappedLong multiply(WrappedLong multiplicand) {
        return multiply(multiplicand, null);
    }
}
```

### Step 2 — Create the factory

```java
import io.compprov.core.ComputationContext;
import io.compprov.core.variable.VariableTrack;
import io.compprov.core.variable.VariableWrapper;
import io.compprov.core.variable.WrappedVariable;

public final class LongWrapperFactory implements VariableWrapper<Long> {
    @Override
    public WrappedVariable wrap(ComputationContext context, VariableTrack track, Long value) {
        return new WrappedLong(context, track, value);
    }
}
```

### Step 3 — Register and use

```java
var env = new DefaultComputationEnvironment();
env.registerWrapper(Long.class, new LongWrapperFactory());

var ctx = new DefaultComputationContext(env,
        new DataContext(Descriptor.descriptor("my-computation")));

// Use the base wrap() method — DefaultComputationContext does not have a wrapLong() helper.
// Cast to your concrete type after wrapping.
var a = (WrappedLong) ctx.wrap(100L, Descriptor.descriptor("a"));
var b = (WrappedLong) ctx.wrap(42L,  Descriptor.descriptor("b"));
var sum = a.add(b, Descriptor.descriptor("sum"));
```

If you use a custom type frequently, extend `DefaultComputationContext` to add a typed
`wrapLong()` convenience method, the same way `DefaultComputationContext` does for
`wrapBigDecimal`, `wrapBigInteger`, etc.

### Type deserialization

If you need to serialize and deserialize snapshots containing your custom type, register a
Jackson deserializer with the `ObjectMapper` inside your custom `ComputationEnvironment`.
See `DefaultComputationEnvironment` for examples using `ZonedDateTimeSerializer`,
`MathContextDeserializer`, and `VariableDeserializer`.

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
