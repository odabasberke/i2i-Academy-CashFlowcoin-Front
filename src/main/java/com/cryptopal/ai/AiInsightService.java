package com.cryptopal.ai;

import com.cryptopal.market.MarketDataProvider;
import com.cryptopal.market.MarketPriceCacheReader;
import com.cryptopal.trading.PortfolioResponse;
import com.cryptopal.trading.PortfolioService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

/**
 * Orchestrates AI-powered portfolio insights: gathers the user's current
 * context (portfolio + live Redis prices), builds a structured system
 * prompt around it, and delegates the actual model call to
 * {@link GeminiClient}.
 *
 * <p>Runs on a dedicated bounded thread pool ({@code aiTaskExecutor}, see
 * {@code config.SchedulingConfig}) via {@code @Async}. The controller
 * returns the resulting {@link CompletableFuture} directly, so Spring MVC
 * releases the servlet thread immediately and completes the HTTP response
 * once this future resolves - a slow or hanging Gemini call never ties up
 * a request-handling thread.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AiInsightService {

    private static final String SYSTEM_PROMPT_TEMPLATE = """
            You are CryptoPal's AI portfolio assistant. Answer the user's
            question using ONLY the account data provided below - never
            invent balances, prices, or transactions that aren't listed.

            Current wallet balances:
            %s

            Recent transactions (most recent first):
            %s

            Live market prices:
            %s

            Rules:
            - Be concise and use plain language, not financial jargon.
            - You may reference specific numbers from the data above.
            - This is informational only, not financial advice - if the
              user asks whether to buy or sell, say so explicitly and note
              that the decision is theirs.
            - If the data above doesn't answer the question, say so
              instead of guessing.
            """;

    private static final int MAX_TRANSACTIONS_IN_PROMPT = 20;

    private final PortfolioService portfolioService;
    private final MarketDataProvider marketDataProvider;
    private final MarketPriceCacheReader priceCacheReader;
    private final GeminiClient geminiClient;

    @Async("aiTaskExecutor")
    public CompletableFuture<AiQueryResponse> answerQuery(Long userId, AiQueryRequest request) {
        String systemPrompt = buildSystemPrompt(userId);
        String answer = geminiClient.generateContent(systemPrompt, request.question());
        log.info("AI query answered for user {}", userId);
        return CompletableFuture.completedFuture(new AiQueryResponse(answer));
    }

    private String buildSystemPrompt(Long userId) {
        PortfolioResponse portfolio = portfolioService.getPortfolio(userId);
        return SYSTEM_PROMPT_TEMPLATE.formatted(
                walletsBlock(portfolio), transactionsBlock(portfolio), pricesBlock());
    }

    private String walletsBlock(PortfolioResponse portfolio) {
        if (portfolio.wallets().isEmpty()) {
            return "(no wallets)";
        }
        return portfolio.wallets().stream()
                .map(w -> "- %s: %s (locked: %s)".formatted(w.currencyCode(), w.balance(), w.lockedBalance()))
                .collect(Collectors.joining("\n"));
    }

    private String transactionsBlock(PortfolioResponse portfolio) {
        if (portfolio.recentTransactions().isEmpty()) {
            return "(no transactions yet)";
        }
        return portfolio.recentTransactions().stream()
                .limit(MAX_TRANSACTIONS_IN_PROMPT)
                .map(t -> "- %s %s %s @ %s (balance after: %s, %s)"
                        .formatted(t.type(), t.amount(), t.currencyPair(), t.price(), t.balanceAfter(), t.executedAt()))
                .collect(Collectors.joining("\n"));
    }

    /** Reads Redis directly (never PostgreSQL) for every symbol the simulator supports. */
    private String pricesBlock() {
        List<String> symbols = List.copyOf(marketDataProvider.getSupportedSymbols());
        String block = symbols.stream()
                .map(priceCacheReader::getCachedPrice)
                .flatMap(Optional::stream)
                .map(p -> "- %s: %s".formatted(p.symbol(), p.price()))
                .collect(Collectors.joining("\n"));
        return block.isEmpty() ? "(no live prices available)" : block;
    }
}
