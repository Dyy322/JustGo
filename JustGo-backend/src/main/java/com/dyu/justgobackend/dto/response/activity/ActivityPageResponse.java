package com.dyu.justgobackend.dto.response.activity;

import java.util.List;

public record ActivityPageResponse<T>(
        List<T> items,
        long total,
        int page,
        int size
) {}
