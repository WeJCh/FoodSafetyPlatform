package com.mortal.regulation.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.mortal.platform.common.ApiResponse;
import com.mortal.regulation.common.enums.FileBizType;
import com.mortal.regulation.dto.FilePresignRequest;
import com.mortal.regulation.service.MinioFileService;
import com.mortal.regulation.util.JwtUserResolver;
import com.mortal.regulation.vo.FilePresignVO;
import org.junit.jupiter.api.Test;

class FileControllerTest {

    @Test
    void presign_shouldAllowPublicComplaintUpload() {
        MinioFileService fileService = mock(MinioFileService.class);
        JwtUserResolver jwtUserResolver = mock(JwtUserResolver.class);
        FileController controller = new FileController(fileService, jwtUserResolver);
        FilePresignRequest request = request("COMPLAINT");
        FilePresignVO responseBody = new FilePresignVO();
        responseBody.setUploadUrl("http://upload");

        when(jwtUserResolver.resolveUserId("Bearer public-token")).thenReturn(1001L);
        when(fileService.presignUpload(eq(1001L), eq(request), eq(FileBizType.COMPLAINT))).thenReturn(responseBody);

        ApiResponse<FilePresignVO> response = controller.presign("Bearer public-token", "PUBLIC", request);

        assertEquals(0, response.getCode());
        assertNotNull(response.getData());
        assertEquals("http://upload", response.getData().getUploadUrl());
        verify(fileService).presignUpload(1001L, request, FileBizType.COMPLAINT);
    }

    @Test
    void presign_shouldRejectPublicRectificationUpload() {
        MinioFileService fileService = mock(MinioFileService.class);
        JwtUserResolver jwtUserResolver = mock(JwtUserResolver.class);
        FileController controller = new FileController(fileService, jwtUserResolver);
        FilePresignRequest request = request("RECTIFICATION");

        when(jwtUserResolver.resolveUserId("Bearer public-token")).thenReturn(1001L);

        ApiResponse<FilePresignVO> response = controller.presign("Bearer public-token", "PUBLIC", request);

        assertEquals(403, response.getCode());
        assertEquals("forbidden biz type", response.getMessage());
        verifyNoInteractions(fileService);
    }

    @Test
    void presign_shouldAllowEnterpriseRectificationUploadWhenUserTypeFallsBackToJwt() {
        MinioFileService fileService = mock(MinioFileService.class);
        JwtUserResolver jwtUserResolver = mock(JwtUserResolver.class);
        FileController controller = new FileController(fileService, jwtUserResolver);
        FilePresignRequest request = request("RECTIFICATION");
        FilePresignVO responseBody = new FilePresignVO();
        responseBody.setFileUrl("http://file");

        when(jwtUserResolver.resolveUserId("Bearer enterprise-token")).thenReturn(2002L);
        when(jwtUserResolver.resolveUserType("Bearer enterprise-token")).thenReturn("ENTERPRISE");
        when(fileService.presignUpload(eq(2002L), eq(request), eq(FileBizType.RECTIFICATION))).thenReturn(responseBody);

        ApiResponse<FilePresignVO> response = controller.presign("Bearer enterprise-token", null, request);

        assertEquals(0, response.getCode());
        assertNotNull(response.getData());
        assertEquals("http://file", response.getData().getFileUrl());
        verify(fileService).presignUpload(2002L, request, FileBizType.RECTIFICATION);
    }

    @Test
    void presign_shouldAllowEnterpriseProfileUploadForEnterprise() {
        MinioFileService fileService = mock(MinioFileService.class);
        JwtUserResolver jwtUserResolver = mock(JwtUserResolver.class);
        FileController controller = new FileController(fileService, jwtUserResolver);
        FilePresignRequest request = request("ENTERPRISE_PROFILE");
        FilePresignVO responseBody = new FilePresignVO();
        responseBody.setFileUrl("http://enterprise-profile-file");

        when(jwtUserResolver.resolveUserId("Bearer enterprise-token")).thenReturn(2002L);
        when(fileService.presignUpload(eq(2002L), eq(request), eq(FileBizType.ENTERPRISE_PROFILE))).thenReturn(responseBody);

        ApiResponse<FilePresignVO> response = controller.presign("Bearer enterprise-token", "ENTERPRISE", request);

        assertEquals(0, response.getCode());
        assertNotNull(response.getData());
        assertEquals("http://enterprise-profile-file", response.getData().getFileUrl());
        verify(fileService).presignUpload(2002L, request, FileBizType.ENTERPRISE_PROFILE);
    }

    @Test
    void presign_shouldRejectInspectionUploadForEnterprise() {
        MinioFileService fileService = mock(MinioFileService.class);
        JwtUserResolver jwtUserResolver = mock(JwtUserResolver.class);
        FileController controller = new FileController(fileService, jwtUserResolver);
        FilePresignRequest request = request("INSPECTION");

        when(jwtUserResolver.resolveUserId("Bearer enterprise-token")).thenReturn(2002L);

        ApiResponse<FilePresignVO> response = controller.presign("Bearer enterprise-token", "ENTERPRISE", request);

        assertEquals(403, response.getCode());
        assertEquals("forbidden biz type", response.getMessage());
        verifyNoInteractions(fileService);
    }

    private FilePresignRequest request(String bizType) {
        FilePresignRequest request = new FilePresignRequest();
        request.setFilename("proof.png");
        request.setContentType("image/png");
        request.setSize(1024L);
        request.setBizType(bizType);
        return request;
    }
}
