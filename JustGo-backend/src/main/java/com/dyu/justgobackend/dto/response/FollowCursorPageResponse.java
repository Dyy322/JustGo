package com.dyu.justgobackend.dto.response;

import java.util.List;

public record FollowCursorPageResponse<T>(
        List<T> items,
        String nextCursor,
        boolean hasMore
) {
}
