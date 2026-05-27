package com.dyu.justgobackend.dto.request.oss;

import jakarta.validation.constraints.NotBlank;

public record AccessUrlRequest(
        @NotBlank(message = "objectKey 不能为空") String objectKey) {
}
