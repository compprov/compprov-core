/**
 * compprov wrapping layer for the NAV calculation example.
 *
 * <p>This package adapts the plain domain types in
 * {@code io.compprov.examples.nav.model} to the compprov tracking framework.
 * Each class here has a single, well-defined responsibility in the wrapping
 * pipeline:
 *
 * <h2>Classes</h2>
 * <ul>
 *   <li>{@link io.compprov.examples.nav.wrapped.WrappedAmount} — tracked wrapper
 *       for {@code Amount}. Declares three provenance-recording operations:
 *       {@code add} (binary), {@code convert} (binary, using a {@code WrappedRate}),
 *       and {@code addBulk} (variadic). Each operation is keyed by a
 *       {@code Descriptor} constant and resolved via {@code getFunction}, making
 *       the operation-to-implementation mapping explicit and extensible.</li>
 *   <li>{@link io.compprov.examples.nav.wrapped.WrappedRate} — tracked wrapper
 *       for {@code Rate}. Rates are inputs only; no operations are defined on
 *       them directly.</li>
 *   <li>{@link io.compprov.examples.nav.wrapped.AmountWrapper} — {@code VariableWrapper}
 *       factory that tells the framework how to instantiate a {@code WrappedAmount}
 *       from a raw {@code Amount} value.</li>
 *   <li>{@link io.compprov.examples.nav.wrapped.RateWrapper} — {@code VariableWrapper}
 *       factory for {@code Rate}.</li>
 *   <li>{@link io.compprov.examples.nav.wrapped.NavComputationContext} — pre-configured
 *       {@code DefaultComputationContext} for NAV calculations. Registers both
 *       wrappers in a shared {@code ComputationEnvironment} and exposes
 *       strongly-typed {@code wrap(Amount, Descriptor)} and
 *       {@code wrap(Rate, Descriptor)} overloads so callers avoid unchecked casts.</li>
 * </ul>
 *
 * <h2>Design note</h2>
 * <p>The static {@code ComputationEnvironment} in {@code NavComputationContext} is
 * shared across all context instances. Wrapper registration is idempotent and
 * thread-safe, so creating multiple {@code NavComputationContext} instances (e.g.
 * one per calculation run) is safe and is the intended usage pattern.
 */
package io.compprov.examples.nav.wrapped;