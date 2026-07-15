package com.cryptopal.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

/**
 * Enables Spring's background task execution: {@code @Scheduled} (used by
 * the market data cache-sync worker) and, as of Module 5, {@code @Async}
 * (used by {@code ai.AiInsightService} so a slow or hanging Gemini call
 * never ties up a servlet request thread). Kept separate from
 * {@code CryptoPalApplication} so the entry point stays a pure bootstrap
 * class.
 */
@Configuration
@EnableScheduling
@EnableAsync
public class SchedulingConfig {

    /**
     * Bounded pool dedicated to AI calls, so a burst of slow/hanging Gemini
     * requests can't grow threads unboundedly or starve other async work.
     * Deliberately left on the default rejection policy (AbortPolicy, not
     * CallerRunsPolicy): a rejection under overload throws immediately -
     * see GlobalExceptionHandler's TaskRejectedException handler - rather
     * than falling back to running the task on the caller's own thread,
     * which would silently reintroduce the exact blocking this executor
     * exists to avoid.
     */
    @Bean("aiTaskExecutor")
    public ThreadPoolTaskExecutor aiTaskExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(2);
        executor.setMaxPoolSize(10);
        executor.setQueueCapacity(50);
        executor.setThreadNamePrefix("ai-task-");
        executor.initialize();
        return executor;
    }
}
