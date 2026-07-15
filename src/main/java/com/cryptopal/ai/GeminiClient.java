package com.cryptopal.ai;

import com.cryptopal.exception.AiServiceUnavailableException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.net.http.HttpClient;
import java.time.Duration;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * Thin, low-level wrapper around the Gemini REST API's {@code generateContent}
 * endpoint. Every failure mode - connect timeout, read timeout, a non-2xx
 * response, an empty/malformed body - is normalized into a single
 * {@link AiServiceUnavailableException}, so {@link AiInsightService} never
 * needs to know anything about HTTP or Gemini's wire format.
 *
 * <p>Built with a plain {@code RestClient.builder()} rather than an
 * injected, Boot-autoconfigured {@code RestClient.Builder} bean - Spring
 * Boot 4 split RestClient's autoconfiguration into its own optional
 * starter, and this project doesn't otherwise need it. The static builder
 * needs nothing beyond {@code spring-boot-starter-web}, which is already a
 * dependency, and Spring's internal Jackson builder (used for the default
 * JSON message converter either way) auto-registers discovered modules
 * the same way Boot's own autoconfigured ObjectMapper does.
 */
@Slf4j
@Component
public class GeminiClient {

    private static final String API_BASE_URL = "https://generativelanguage.googleapis.com/v1beta/models";
    private static final Duration CONNECT_TIMEOUT = Duration.ofSeconds(3);
    private static final Duration READ_TIMEOUT = Duration.ofSeconds(12);
    private static final double TEMPERATURE = 0.4;
    private static final int MAX_OUTPUT_TOKENS = 1024;

    private final RestClient restClient;
    private final String model;

    public GeminiClient(
            @Value("${cryptopal.ai.gemini.api-key}") String apiKey,
            @Value("${cryptopal.ai.gemini.model:gemini-3.1-flash-lite}") String model) {
        this.model = "gemini-3.1-flash-lite";

        HttpClient httpClient = HttpClient.newBuilder()
                .connectTimeout(CONNECT_TIMEOUT)
                .build();
        JdkClientHttpRequestFactory requestFactory = new JdkClientHttpRequestFactory(httpClient);
        requestFactory.setReadTimeout(READ_TIMEOUT);

        this.restClient = RestClient.builder()
                .baseUrl(API_BASE_URL)
                .requestFactory(requestFactory)
                // Current Gemini auth convention (google's docs, mid-2026):
                // an x-goog-api-key header, not a query-string ?key= param -
                // keeps the key out of URL/access logs too.
                .defaultHeader("x-goog-api-key", apiKey)
                .build();
    }

    /**
     * Calls Gemini with a system prompt (the enriched account context) and
     * the user's question, and returns the plain-text answer.
     *
     * @throws AiServiceUnavailableException on any failure - timeout,
     *         connection error, non-2xx response, or an empty/malformed body
     */
    public String generateContent(String systemPrompt, String userQuestion) {
        GeminiRequest request = new GeminiRequest(
                new GeminiRequest.SystemInstruction(List.of(new GeminiRequest.Part(systemPrompt))),
                List.of(new GeminiRequest.Content("user", List.of(new GeminiRequest.Part(userQuestion)))),
                new GeminiRequest.GenerationConfig(TEMPERATURE, MAX_OUTPUT_TOKENS));

        try {
            GeminiResponse response = restClient.post()
                    .uri("/{model}:generateContent", model)
                    .body(request)
                    .retrieve()
                    .body(GeminiResponse.class);
            return extractText(response);
        } catch (RestClientException e) {
            // RestClientException's subtypes cover connect/read timeouts,
            // connection failures, and non-2xx responses (RestClient throws
            // for 4xx/5xx by default) - one catch block, one clean fallback.
            log.error("Gemini API call failed", e);
            throw new AiServiceUnavailableException("Gemini API call failed", e);
        }
    }

    private String extractText(GeminiResponse response) {
        if (response == null || response.candidates() == null || response.candidates().isEmpty()) {
            throw new AiServiceUnavailableException("Gemini returned no candidates");
        }
        GeminiResponse.Content content = response.candidates().get(0).content();
        if (content == null || content.parts() == null || content.parts().isEmpty()) {
            throw new AiServiceUnavailableException("Gemini returned an empty response");
        }
        return content.parts().stream()
                .map(GeminiResponse.Part::text)
                .filter(Objects::nonNull)
                .collect(Collectors.joining());
    }

    // --- Gemini wire format - private and nested on purpose: nobody outside
    // this class should ever need to know Gemini's specific JSON shape. ---

    private record GeminiRequest(
            SystemInstruction systemInstruction, List<Content> contents, GenerationConfig generationConfig) {
        private record SystemInstruction(List<Part> parts) {
        }

        private record Content(String role, List<Part> parts) {
        }

        private record Part(String text) {
        }

        private record GenerationConfig(double temperature, int maxOutputTokens) {
        }
    }

    private record GeminiResponse(List<Candidate> candidates) {
        private record Candidate(Content content) {
        }

        private record Content(List<Part> parts) {
        }

        private record Part(String text) {
        }
    }
}
