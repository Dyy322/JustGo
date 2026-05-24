package com.dyu.justgobackend.dto.response;

public record UploadTokenResponse(String uploadUrl, String fileUrl, String objectKey, long expireSeconds) {
}
