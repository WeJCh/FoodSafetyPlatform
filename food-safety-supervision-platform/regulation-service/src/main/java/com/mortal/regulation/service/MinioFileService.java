package com.mortal.regulation.service;

import com.mortal.regulation.config.MinioProperties;
import com.mortal.regulation.dto.FilePresignRequest;
import com.mortal.regulation.vo.FilePresignVO;
import io.minio.BucketExistsArgs;
import io.minio.MakeBucketArgs;
import io.minio.MinioClient;
import io.minio.GetPresignedObjectUrlArgs;
import io.minio.http.Method;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

/**
 * Minio文件服务
 */
@Service
public class MinioFileService {

    private static final long MAX_FILE_SIZE = 5 * 1024 * 1024L;
    private static final int PRESIGN_EXPIRE_SECONDS = 600;
    private static final List<String> ALLOWED_CONTENT_TYPES = List.of(
        "image/jpeg",
        "image/png",
        "image/webp"
    );

    private final MinioClient minioClient;
    private final MinioProperties properties;

    public MinioFileService(MinioClient minioClient, MinioProperties properties) {
        this.minioClient = minioClient;
        this.properties = properties;
    }

    /**
     * 生成上传预签名 URL，上传由前端直连 MinIO 完成，服务端只返回地址。
     */
    public FilePresignVO presignUpload(Long userId, FilePresignRequest request) {
        validateRequest(request);
        ensureBucket();
        String objectKey = buildObjectKey(userId, request.getFilename());
        String uploadUrl = buildUploadUrl(objectKey);
        String fileUrl = buildFileUrl(objectKey);

        FilePresignVO vo = new FilePresignVO();
        vo.setUploadUrl(uploadUrl);
        vo.setFileUrl(fileUrl);
        vo.setObjectKey(objectKey);
        return vo;
    }

    private void validateRequest(FilePresignRequest request) {
        if (request.getSize() == null || request.getSize() <= 0) {
            throw new IllegalArgumentException("file size required");
        }
        if (request.getSize() > MAX_FILE_SIZE) {
            throw new IllegalArgumentException("file too large");
        }
        String contentType = StringUtils.hasText(request.getContentType())
            ? request.getContentType().toLowerCase(Locale.ROOT).trim()
            : "";
        if (!ALLOWED_CONTENT_TYPES.contains(contentType)) {
            throw new IllegalArgumentException("invalid content type");
        }
    }

    /**
     * 确保桶存在
     */
    private void ensureBucket() {
        try {
            String bucket = properties.getBucket();
            boolean exists = minioClient.bucketExists(BucketExistsArgs.builder().bucket(bucket).build());
            if (!exists) {
                // 中文注释：若桶不存在则自动创建，便于本地原型快速跑通。
                minioClient.makeBucket(MakeBucketArgs.builder().bucket(bucket).build());
            }
        } catch (Exception ex) {
            throw new IllegalStateException("minio bucket error", ex);
        }
    }

    /**
     * 构建对象键
     */
    private String buildObjectKey(Long userId, String filename) {
        String datePath = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy/MM/dd"));
        String safeName = sanitizeFilename(filename);
        String userSegment = userId == null ? "public" : String.valueOf(userId);
        String uuid = UUID.randomUUID().toString().replace("-", "");
        return String.format("complaints/%s/%s/%s_%s", datePath, userSegment, uuid, safeName);
    }

    /**
     * 构建上传URL
     */
    private String buildUploadUrl(String objectKey) {
        try {
            return minioClient.getPresignedObjectUrl(GetPresignedObjectUrlArgs.builder()
                .method(Method.PUT)
                .bucket(properties.getBucket())
                .object(objectKey)
                .expiry(PRESIGN_EXPIRE_SECONDS)
                .build());
        } catch (Exception ex) {
            throw new IllegalStateException("minio presign error", ex);
        }
    }

    /**
     * 构建文件URL
     */
    private String buildFileUrl(String objectKey) {
        String base = StringUtils.hasText(properties.getPublicEndpoint())
            ? properties.getPublicEndpoint()
            : properties.getEndpoint();
        String trimmed = base == null ? "" : base.replaceAll("/+$", "");
        return String.format("%s/%s/%s", trimmed, properties.getBucket(), objectKey);
    }

    /**
     *  文件名安全处理
     */
    private String sanitizeFilename(String filename) {
        if (!StringUtils.hasText(filename)) {
            return "file";
        }
        return filename.replace("\\", "_").replace("/", "_").trim();
    }
}
