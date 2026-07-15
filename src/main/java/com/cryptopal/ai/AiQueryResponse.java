package com.cryptopal.ai;

import java.time.Instant;

public record AiQueryResponse(String answer, Instant answeredAt) {

    public AiQueryResponse(String answer) {
        this(answer, Instant.now());
    }
}
