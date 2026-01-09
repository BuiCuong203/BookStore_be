package com.vn.backend.dto.ai;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.util.List;

@Data
public class RecommendResponse {
    @JsonProperty("book_ids")
    private List<Long> bookIds;

    private List<Double> scores;
}
