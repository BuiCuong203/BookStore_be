package com.vn.backend.dto.ai;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class GetSimilarRequest {
    @JsonProperty("book_id")
    private Long bookId;

    private int limit;
}
