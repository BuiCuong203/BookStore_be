package com.vn.backend.dto.response;

import lombok.*;
import lombok.experimental.FieldDefaults;

import java.util.List;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class PagedResponse<T> {
    List<T> data;
    Long totalElements;
    int totalPages;
    int currentPage;
    int pageSize;
    boolean hasNext;
    boolean hasPrevious;
}
