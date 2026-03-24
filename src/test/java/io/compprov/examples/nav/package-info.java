/**
 * End-to-end example demonstrating compprov-core usage through a realistic
 * crypto-portfolio Net Asset Value (NAV) calculation, including how to wrap
 * custom business-domain types into the framework.
 *
 * <p>The example computes the total USD value of a multi-asset crypto portfolio
 * (BTC, ETH, USDC positions held across Binance, staking, and Morpho) by
 * converting each position to USD at a spot rate and summing the results.
 * Every arithmetic step is automatically recorded in a Calculation Provenance
 * Graph (CPG), producing a machine-readable audit trail that can be serialised
 * and stored alongside the final NAV figure.
 *
 * <h2>Package layout</h2>
 * <ul>
 *   <li>{@code io.compprov.examples.nav} — this package; contains
 *       {@link io.compprov.examples.nav.NetAssetValueCalculator}, the JUnit test
 *       that drives the full calculation and asserts the result.</li>
 *   <li>{@code io.compprov.examples.nav.model} — plain business-domain classes:
 *       {@code Amount}, {@code Rate}, {@code Currency} enum, and a stub
 *       {@code DataProvider} that simulates fetching market data.</li>
 *   <li>{@code io.compprov.examples.nav.wrapped} — compprov wrapping layer:
 *       {@code WrappedAmount}, {@code WrappedRate}, their {@code VariableWrapper}
 *       factories, and {@code NavComputationContext} which wires everything
 *       together.</li>
 * </ul>
 *
 * <h2>Wrapping custom business models</h2>
 * <p>A key goal of this example is to show the three-step pattern for integrating
 * any existing domain type with compprov — without modifying the type itself:
 * <ol>
 *   <li><b>Extend {@code AbstractWrappedVariable&lt;T&gt;}</b> — create a tracked
 *       wrapper class (e.g. {@code WrappedAmount}) that holds a value of your type
 *       and declares its operations as {@code Descriptor} constants. Implement
 *       {@code getFunction(Descriptor)} to map each descriptor to the actual
 *       computation lambda.</li>
 *   <li><b>Implement {@code VariableWrapper&lt;T&gt;}</b> — create a lightweight factory
 *       (e.g. {@code AmountWrapper}) whose sole job is to instantiate the wrapped
 *       class from a raw value. This is the registration contract the framework
 *       uses to create tracked variables.</li>
 *   <li><b>Register with a {@code ComputationEnvironment}</b> — call
 *       {@code environment.registerWrapper(MyType.class, new MyTypeWrapper())} once,
 *       then pass the environment to a {@code DefaultComputationContext}. After that,
 *       every call to {@code context.wrap(myValue, descriptor)} automatically
 *       produces a tracked variable and records it in the CPG.</li>
 * </ol>
 * <p>{@code NavComputationContext} bundles steps 2–3 into a single, reusable context
 * class, which is the recommended pattern for real applications.
 *
 * <h2>How to read the example</h2>
 * <ol>
 *   <li>Start with {@link io.compprov.examples.nav.NetAssetValueCalculator#calculate()}
 *       to see the high-level calculation flow and how the context is used.</li>
 *   <li>Look at {@code NavComputationContext} to understand how domain types are
 *       registered with the framework and how typed {@code wrap()} overloads are
 *       exposed to callers.</li>
 *   <li>Examine {@code WrappedAmount} to see how multi-argument operations
 *       ({@code add}, {@code convert}, {@code addBulk}) are declared and recorded
 *       in the CPG.</li>
 * </ol>
 */
package io.compprov.examples.nav;