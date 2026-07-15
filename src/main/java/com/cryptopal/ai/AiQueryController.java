package com.cryptopal.ai;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.concurrent.CompletableFuture;

@RestController
@CrossOrigin(origins = "http://localhost:5173")
@RequestMapping("/api/ai")
@RequiredArgsConstructor
@Tag(name = "AI Insights", description = "Gemini-powered portfolio Q&A")
public class AiQueryController {

    private final AiInsightService aiInsightService;

    @Operation(summary = "Ask a question about your portfolio - answered by Gemini using your live account data")
    @PostMapping("/query")
    public CompletableFuture<ResponseEntity<AiQueryResponse>> query(
            Authentication authentication, @Valid @RequestBody AiQueryRequest request) {
        Long userId = AuthenticatedUserId.from(authentication);
        return aiInsightService.answerQuery(userId, request).thenApply(ResponseEntity::ok);
    }
}
