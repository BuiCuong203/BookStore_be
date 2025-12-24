package com.vn.backend.dto.response;

import lombok.*;
import lombok.experimental.FieldDefaults;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class RecentOrderDataResponse {
    String name; // "Hôm nay", "Hôm qua"...
    Long orders;
    Double revenue;
}
