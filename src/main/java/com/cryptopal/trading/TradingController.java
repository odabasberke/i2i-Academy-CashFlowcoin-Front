package com.cryptopal.trading;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/trading")
@RequiredArgsConstructor
@Tag(name = "Trading", description = "Market order execution")
public class TradingController {

    private final TradingService tradingService;

    @Operation(summary = "Execute a market BUY or SELL order at the current Redis-cached price")
    @PostMapping("/order")
    public ResponseEntity<OrderResponse> placeOrder(
            Authentication authentication, @Valid @RequestBody OrderRequest request) {
        Long userId = AuthenticatedUserId.from(authentication);
        return ResponseEntity.ok(tradingService.executeOrder(userId, request));
    }
}
