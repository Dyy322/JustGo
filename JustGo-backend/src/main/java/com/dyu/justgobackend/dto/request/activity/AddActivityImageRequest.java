package com.dyu.justgobackend.dto.request.activity;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record AddActivityImageRequest(
        @NotBlank @Size(max = 500) String objectKey
) {}
