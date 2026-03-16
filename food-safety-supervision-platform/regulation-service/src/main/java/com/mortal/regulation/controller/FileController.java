package com.mortal.regulation.controller;

import com.mortal.platform.common.ApiResponse;
import com.mortal.regulation.dto.FilePresignRequest;
import com.mortal.regulation.service.MinioFileService;
import com.mortal.regulation.util.JwtUserResolver;
import com.mortal.regulation.vo.FilePresignVO;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 文件控制器
 */
@RestController
@RequestMapping("/api/files")
public class FileController {

    private final MinioFileService minioFileService;
    private final JwtUserResolver jwtUserResolver;

    public FileController(MinioFileService minioFileService, JwtUserResolver jwtUserResolver) {
        this.minioFileService = minioFileService;
        this.jwtUserResolver = jwtUserResolver;
    }

    /**
     * 生成上传预签名地址（需要登录）。
     */
    @PostMapping("/presign")
    public ApiResponse<FilePresignVO> presign(@RequestHeader("Authorization") String token,
                                              @Valid @RequestBody FilePresignRequest request) {
        Long userId = jwtUserResolver.resolveUserId(token);
        if (userId == null) {
            return ApiResponse.failure(401, "unauthorized");
        }
        return ApiResponse.success(minioFileService.presignUpload(userId, request));
    }
}

