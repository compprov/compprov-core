/**
 * Plain business-domain model for the NAV calculation example.
 *
 * <p>These classes are intentionally framework-agnostic: they carry no compprov
 * imports and can be used as-is in any Java application. compprov wraps them
 * externally (see {@code io.compprov.examples.nav.wrapped}) without modifying
 * their source.
 *
 * <h2>Classes</h2>
 * <ul>
 *   <li>{@link io.compprov.examples.nav.model.Currency} — enum of supported
 *       currencies ({@code BTC}, {@code ETH}, {@code USDC}, {@code USD}), each
 *       carrying its standard decimal precision.</li>
 *   <li>{@link io.compprov.examples.nav.model.Amount} — an immutable
 *       (currency, quantity) pair. Supports {@code add} (same-currency) and
 *       {@code convert} (cross-currency via a {@code Rate}), both returning a
 *       new {@code Amount}.</li>
 *   <li>{@link io.compprov.examples.nav.model.Rate} — an immutable directional
 *       exchange rate between two currencies. {@code Amount.convert} uses the
 *       rate direction to decide whether to multiply or divide.</li>
 *   <li>{@link io.compprov.examples.nav.model.DataProvider} — stub data source
 *       that returns hard-coded market prices and portfolio balances, simulating
 *       what would be live API calls in a production system.</li>
 * </ul>
 */
package io.compprov.examples.nav.model;