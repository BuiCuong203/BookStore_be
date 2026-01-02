package com.vn.backend.controller;

import com.vn.backend.dto.response.ApiResponse;
import com.vn.backend.dto.response.ImageUploadResponse;
import com.vn.backend.service.CloudinaryService;
import jakarta.validation.constraints.NotNull;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/v1/uploads")
public class ImageUploadController {

    private final CloudinaryService cloudinaryService;

    public ImageUploadController(CloudinaryService cloudinaryService) {
        this.cloudinaryService = cloudinaryService;
    }

    @PostMapping(consumes = {"multipart/form-data"})
    public ResponseEntity<ApiResponse<ImageUploadResponse>> upload(
            @RequestPart("file") @NotNull MultipartFile file
    ) throws Exception {
        var res = cloudinaryService.upload(file);
        return ResponseEntity.status(res.getStatusCode()).body(res);
    }

    @DeleteMapping("/{publicId}")
    public ApiResponse<?> delete(@PathVariable String publicId) throws Exception {
        boolean ok = cloudinaryService.deleteByPublicId(publicId);
        return ok ?
                ApiResponse.builder().statusCode(HttpStatus.NO_CONTENT.value()).build() :
                ApiResponse.builder().statusCode(HttpStatus.NOT_FOUND.value()).build();
    }
}
