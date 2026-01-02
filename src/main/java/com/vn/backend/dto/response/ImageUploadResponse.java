package com.vn.backend.dto.response;

import lombok.*;
import lombok.experimental.FieldDefaults;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class ImageUploadResponse {
    String url;
    String id;
    String error;

    public ImageUploadResponse(String url, String id) {
        this.url = url;
        this.id = id;
    }
}
