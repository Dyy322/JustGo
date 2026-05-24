package com.dyu.justgobackend.util;

import java.util.Optional;

public final class AuthorizationHeaderUtils {

    public static final String BEARER_PREFIX = "Bearer ";

    private AuthorizationHeaderUtils() {
    }

    /**
     * Parses {@code Authorization: Bearer <token>}, trimming the token segment.
     *
     * @return empty if the header is missing or not a non-empty Bearer value
     */
    public static Optional<String> bearerToken(String authorizationHeader) {
        if (authorizationHeader == null || !authorizationHeader.startsWith(BEARER_PREFIX)) {
            return Optional.empty();
        }
        String raw = authorizationHeader.substring(BEARER_PREFIX.length()).trim();
        return raw.isEmpty() ? Optional.empty() : Optional.of(raw);
    }
}
