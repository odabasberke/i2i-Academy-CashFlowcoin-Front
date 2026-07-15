/**
 * Order execution and wallet management.
 *
 * <p>Owns: buy/sell order processing ({@code TradingService},
 * {@code TradingController} - {@code POST /api/trading/order}), portfolio
 * views ({@code PortfolioService}, {@code PortfolioController} -
 * {@code GET /api/portfolio}), balance debits/credits against the
 * {@code wallets} table, and immutable transaction logging in
 * {@code transaction_logs}. All balance-mutating operations here run
 * inside a single DB transaction ({@code @Transactional}), with
 * pessimistic row locks (fiat wallet locked before the asset wallet, in
 * that order for both buy and sell) to keep concurrent orders from the
 * same user from racing on a wallet balance or deadlocking each other.
 *
 * <p>Exposes a small {@code WalletService} API for other packages to call
 * into (e.g. {@code auth.AuthService} grants a signup bonus wallet right
 * after registration) rather than reaching into {@code WalletRepository}
 * directly - keeps wallet invariants owned entirely by this package.
 */
package com.cryptopal.trading;
