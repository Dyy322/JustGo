package com.dyu.justgobackend.dto.response.oss;

public record UploadTokenResponse(String uploadUrl, String fileUrl, String objectKey, long expireSeconds) {
}
