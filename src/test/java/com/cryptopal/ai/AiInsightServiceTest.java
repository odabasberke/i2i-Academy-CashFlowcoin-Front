package com.cryptopal.ai;

import com.cryptopal.exception.AiServiceUnavailableException;
import com.cryptopal.market.MarketDataProvider;
import com.cryptopal.market.MarketPriceCacheReader;
import com.cryptopal.market.PriceTick;
import com.cryptopal.trading.PortfolioResponse;
import com.cryptopal.trading.PortfolioService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AiInsightServiceTest {

    @Mock private PortfolioService portfolioService;
    @Mock private MarketDataProvider marketDataProvider;
    @Mock private MarketPriceCacheReader priceCacheReader;
    @Mock private GeminiClient geminiClient;

    private AiInsightService aiInsightService;

    @BeforeEach
    void setUp() {
        aiInsightService = new AiInsightService(portfolioService, marketDataProvider, priceCacheReader, geminiClient);
    }

    @Test
    void answerQuery_buildsPromptFromPortfolioAndPrices_andReturnsGeminiAnswer() throws Exception {
        PortfolioResponse.WalletBalance wallet =
                new PortfolioResponse.WalletBalance("BTC", new BigDecimal("0.05"), BigDecimal.ZERO);
        PortfolioResponse.TransactionSummary tx = new PortfolioResponse.TransactionSummary(
                UUID.randomUUID(), "BUY", "BTC/USD", new BigDecimal("0.01"),
                new BigDecimal("50000"), new BigDecimal("0.05"), Instant.now());
        when(portfolioService.getPortfolio(10L)).thenReturn(new PortfolioResponse(List.of(wallet), List.of(tx)));

        when(marketDataProvider.getSupportedSymbols()).thenReturn(Set.of("BTC"));
        when(priceCacheReader.getCachedPrice("BTC"))
                .thenReturn(Optional.of(new PriceTick("BTC", new BigDecimal("51000.00"), Instant.now())));

        // The prompt sent to Gemini must actually contain the wallet balance -
        // this is the whole point of "context enrichment".
        when(geminiClient.generateContent(contains("0.05"), anyString())).thenReturn("You hold 0.05 BTC.");

        CompletableFuture<AiQueryResponse> future =
                aiInsightService.answerQuery(10L, new AiQueryRequest("How much BTC do I have?"));

        assertThat(future.get().answer()).isEqualTo("You hold 0.05 BTC.");
    }

    @Test
    void answerQuery_withEmptyPortfolioAndNoPrices_stillBuildsAPromptWithoutThrowing() throws Exception {
        when(portfolioService.getPortfolio(10L)).thenReturn(new PortfolioResponse(List.of(), List.of()));
        when(marketDataProvider.getSupportedSymbols()).thenReturn(Set.of());
        when(geminiClient.generateContent(anyString(), anyString())).thenReturn("You have no holdings yet.");

        AiQueryResponse response = aiInsightService.answerQuery(10L, new AiQueryRequest("What do I own?")).get();

        assertThat(response.answer()).isEqualTo("You have no holdings yet.");
    }

    @Test
    void answerQuery_whenGeminiFails_propagatesForGlobalExceptionHandlerToCatch() {
        when(portfolioService.getPortfolio(10L)).thenReturn(new PortfolioResponse(List.of(), List.of()));
        when(marketDataProvider.getSupportedSymbols()).thenReturn(Set.of());
        when(geminiClient.generateContent(anyString(), anyString()))
                .thenThrow(new AiServiceUnavailableException("Gemini API call failed"));

        // No Spring proxy in a plain unit test, so @Async has no effect here -
        // the method body runs synchronously and the exception propagates
        // directly. In production, Spring's @Async interceptor instead makes
        // the *returned* CompletableFuture complete exceptionally, which
        // Spring MVC's async dispatch routes to GlobalExceptionHandler the
        // same way a synchronous throw would.
        assertThatThrownBy(() -> aiInsightService.answerQuery(10L, new AiQueryRequest("test")))
                .isInstanceOf(AiServiceUnavailableException.class);
    }
}
