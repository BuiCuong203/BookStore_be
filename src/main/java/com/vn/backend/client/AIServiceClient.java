package com.vn.backend.client;

import com.vn.backend.dto.ai.*;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

@FeignClient(name = "ai-service", url = "${ai.service.url}")
public interface AIServiceClient {

    @PostMapping("/semantic-search")
    RecommendResponse searchBooks(@RequestBody SemanticSearchRequest request);

    @PostMapping("/similar")
    RecommendResponse getSimilarBooks(@RequestBody GetSimilarRequest request);
}