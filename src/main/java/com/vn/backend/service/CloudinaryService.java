package com.vn.backend.service;

import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.util.Map;
import java.util.UUID;

@Service
public class CloudinaryService {

    private final Cloudinary cloudinary;

    public CloudinaryService(Cloudinary cloudinary) {
        this.cloudinary = cloudinary;
    }

    public UploadResult upload(MultipartFile file, String folder) throws Exception {
        // kiểm tra content-type cơ bản
        String ct = file.getContentType();
        if (ct == null || !(ct.startsWith("image/") || ct.startsWith("video/") || ct.equals("application/pdf"))) {
            throw new IllegalArgumentException("Unsupported file type: " + ct);
        }

        String publicId = folder + "/" + UUID.randomUUID();
        try (InputStream is = file.getInputStream()) {
            Map<?, ?> res = cloudinary.uploader().upload(is, ObjectUtils.asMap(
                    "public_id", publicId,
                    "folder", folder,
                    "resource_type", "auto",
                    "overwrite", false,
                    "use_filename", true,
                    "unique_filename", true
            ));

            return new UploadResult(
                    (String) res.get("public_id"),
                    (String) res.get("secure_url"),
                    (String) res.get("format"),
                    ((Number) res.get("bytes")).longValue(),
                    (String) res.get("resource_type")
            );
        }
    }

    public boolean deleteByPublicId(String publicId) throws Exception {
        Map<?, ?> res = cloudinary.uploader().destroy(publicId, ObjectUtils.asMap(
                "resource_type", "image"
        ));
        return res.get("result").equals("ok");
    }

    public record UploadResult(String publicId, String url, String format, long bytes, String resourceType) {
    }
}
