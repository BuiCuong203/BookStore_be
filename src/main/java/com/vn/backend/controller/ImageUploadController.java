package com.vn.backend.controller;

import com.vn.backend.dto.response.ApiResponse;
import com.vn.backend.service.CloudinaryService;
import com.vn.backend.service.CloudinaryService.UploadResult;
import jakarta.validation.constraints.NotNull;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/uploads")
public class ImageUploadController {

    private final CloudinaryService cloudinaryService;

    public ImageUploadController(CloudinaryService cloudinaryService) {
        this.cloudinaryService = cloudinaryService;
    }

    @PostMapping(consumes = {"multipart/form-data"})
    public ApiResponse<UploadResult> upload(
            @RequestPart("file") @NotNull MultipartFile file,
            @RequestParam(value = "folder", defaultValue = "my-app/uploads") String folder
    ) throws Exception {
        var res = cloudinaryService.upload(file, folder);
        return ApiResponse.<UploadResult>builder()
                .message("Upload successful")
                .data(res)
                .build();
    }

    @DeleteMapping("/{publicId}")
    public ApiResponse<?> delete(@PathVariable String publicId) throws Exception {
        boolean ok = cloudinaryService.deleteByPublicId(publicId);
        return ok ?
                ApiResponse.builder().statusCode(HttpStatus.NO_CONTENT.value()).build() :
                ApiResponse.builder().statusCode(HttpStatus.NOT_FOUND.value()).build();
    }
}
