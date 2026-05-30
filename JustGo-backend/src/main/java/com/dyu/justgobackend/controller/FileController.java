package com.dyu.justgobackend.controller;

import com.dyu.justgobackend.common.ApiResponse;
import com.dyu.justgobackend.dto.request.oss.AccessUrlRequest;
import com.dyu.justgobackend.dto.request.user.UploadTokenRequest;
import com.dyu.justgobackend.dto.response.oss.AccessUrlResponse;
import com.dyu.justgobackend.dto.response.oss.UploadTokenResponse;
import com.dyu.justgobackend.oss.OssService;
import com.dyu.justgobackend.oss.OssService.CorsStatus;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/files")
public class FileController {

    private final OssService ossService;

    public FileController(OssService ossService) {
        this.ossService = ossService;
    }

    @GetMapping("/cors-status")
    public ApiResponse<CorsStatus> corsStatus() {
        return ApiResponse.success(ossService.getCorsStatus());
    }

    @PostMapping("/upload-token")
    public ApiResponse<UploadTokenResponse> uploadToken(@Valid @RequestBody UploadTokenRequest request) {
        return ApiResponse.success(ossService.generateUploadToken(request.prefix(), request.ext()));
    }

    @PostMapping("/access-url")
    public ApiResponse<AccessUrlResponse> accessUrl(@Valid @RequestBody AccessUrlRequest request) {
        String presignedUrl = ossService.generatePresignedGetUrl(request.objectKey());
        return ApiResponse.success(new AccessUrlResponse(presignedUrl));
    }
}
