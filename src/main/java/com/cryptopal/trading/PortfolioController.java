package com.cryptopal.trading;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/portfolio")
@RequiredArgsConstructor
@Tag(name = "Portfolio", description = "Current wallet balances and trade history")
public class PortfolioController {

    private final PortfolioService portfolioService;

    @Operation(summary = "Get the authenticated user's wallet balances and recent transaction history")
    @GetMapping
    public ResponseEntity<PortfolioResponse> getPortfolio(Authentication authentication) {
        Long userId = AuthenticatedUserId.from(authentication);
        return ResponseEntity.ok(portfolioService.getPortfolio(userId));
    }
}
