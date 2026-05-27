package com.dyu.justgobackend.dto.response.user;

import java.util.List;

public record FollowCursorPageResponse<T>(
        List<T> items,
        String nextCursor,
        boolean hasMore
) {
}
