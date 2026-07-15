package com.cryptopal.trading;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public record OrderRequest(
        @NotNull OrderType type,
        @NotBlank String symbol,
        @NotNull @DecimalMin(value = "0.00000001", message = "quantity must be positive") BigDecimal quantity
) {
}
