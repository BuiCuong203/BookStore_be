package com.vn.backend.dto.response;

import lombok.*;
import lombok.experimental.FieldDefaults;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class MonthlyDataResponse {
    String name; // T1, T2, T3...
    Long users;
    Long orders;
    Double revenue;
    Long books;
}
