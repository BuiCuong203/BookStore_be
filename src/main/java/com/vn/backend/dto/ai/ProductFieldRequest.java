package com.vn.backend.dto.ai;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class ProductFieldRequest {
    private String name;
    private String category;
    private String description;
}
