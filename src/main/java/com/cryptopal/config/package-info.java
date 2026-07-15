/**
 * Application-wide configuration beans.
 *
 * <p>Owns: datasource/JPA, Redis client, OpenAPI, Spring's background task
 * execution ({@code SchedulingConfig} - {@code @EnableScheduling} for the
 * market data worker, {@code @EnableAsync} plus a bounded
 * {@code aiTaskExecutor} pool for {@code ai.AiInsightService}), and the
 * Spring Security filter chain ({@code SecurityConfig} - stateless JWT,
 * CSRF disabled since this API only ever authenticates via a bearer token
 * header, never cookies).
 */
package com.cryptopal.config;
