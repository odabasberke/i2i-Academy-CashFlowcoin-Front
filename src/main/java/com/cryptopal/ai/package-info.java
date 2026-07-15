/**
 * LLM-powered financial analysis.
 *
 * <p>Owns: {@code POST /api/ai/query}, context enrichment (pulls the
 * user's portfolio from {@code trading} and live prices from Redis via
 * {@code market.MarketPriceCacheReader}), system-prompt construction, and
 * the {@code GeminiClient} wrapper around Google Gemini's REST API.
 * Runs on the dedicated {@code aiTaskExecutor} pool ({@code @Async}, see
 * {@code config.SchedulingConfig}) so a slow or hanging model call never
 * ties up a servlet request thread.
 *
 * <p>Output produced here is informational only, not investment advice -
 * that constraint is enforced directly in the system prompt template, not
 * just left as a convention.
 */
package com.cryptopal.ai;
