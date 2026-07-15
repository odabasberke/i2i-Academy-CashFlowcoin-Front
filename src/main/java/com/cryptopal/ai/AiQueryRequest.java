package com.cryptopal.ai;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record AiQueryRequest(
        @NotBlank @Size(max = 2000) String question
) {
}
