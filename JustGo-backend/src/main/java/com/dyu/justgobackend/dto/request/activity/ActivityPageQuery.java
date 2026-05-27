package com.dyu.justgobackend.dto.request.activity;

import jakarta.validation.constraints.*;

public record ActivityPageQuery(
        @Positive Long categoryId,
        @Positive Integer status,
        @Size(max = 100) String keyword,
        @Min(1) @Max(50) int size,
        @Min(1) int page
) {
    public ActivityPageQuery {
        if (size == 0) size = 20;
        if (page == 0) page = 1;
    }
}
