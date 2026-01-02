package com.vn.backend.service;

import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;
import com.vn.backend.dto.response.ApiResponse;
import com.vn.backend.dto.response.ImageUploadResponse;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.util.HashMap;
import java.util.Map;

@Service
public class CloudinaryService {

    private final Cloudinary cloudinary;

    public CloudinaryService(Cloudinary cloudinary) {
        this.cloudinary = cloudinary;
    }

    public ApiResponse<ImageUploadResponse> upload(MultipartFile file) throws Exception {
        // kiểm tra content-type cơ bản
        String ct = file.getContentType();
        if (ct == null || !(ct.startsWith("image/") || ct.startsWith("video/") || ct.equals("application/pdf"))) {
            throw new IllegalArgumentException("Unsupported file type: " + ct);
        }

        Map<?, ?> uploadResult = cloudinary.uploader().upload(file.getBytes(), ObjectUtils.asMap(
                "resource_type", "auto",
                "overwrite", false,
                "use_filename", true,
                "unique_filename", true
        ));

        Map<String, String> result = new HashMap<>();
        result.put("url", uploadResult.get("secure_url").toString());
        result.put("id", uploadResult.get("public_id").toString());

        ImageUploadResponse response = new ImageUploadResponse(result.get("url"), result.get("id"));

        return ApiResponse.<ImageUploadResponse>builder()
                .message("Tải lên thành công")
                .data(response)
                .build();
    }

    public boolean deleteByPublicId(String publicId) throws Exception {
        Map<?, ?> res = cloudinary.uploader().destroy(publicId, ObjectUtils.emptyMap());
        return res.get("result").equals("ok");
    }
}
