package com.dyu.justgobackend.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record UploadTokenRequest(
        @NotBlank @Pattern(regexp = "^(jpg|jpeg|png|gif|webp)$", message = "不支持的文件类型") String ext,
        @NotBlank @Pattern(regexp = "^[a-z]+$", message = "prefix 只能是小写字母") String prefix) {
}
