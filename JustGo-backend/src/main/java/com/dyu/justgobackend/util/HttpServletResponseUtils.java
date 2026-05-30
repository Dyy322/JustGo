package com.dyu.justgobackend.util;

import com.dyu.justgobackend.common.ApiResponse;
import com.dyu.justgobackend.config.ApplicationJsonMapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

public final class HttpServletResponseUtils {

    private static final ObjectMapper OBJECT_MAPPER = ApplicationJsonMapper.create();

    private HttpServletResponseUtils() {
    }

    public static void writeJson(HttpServletResponse response, int httpStatus, ApiResponse<?> body) throws IOException {
        response.setStatus(httpStatus);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        try {
            OBJECT_MAPPER.writeValue(response.getOutputStream(), body);
        } catch (Exception e) {
            response.reset();
            throw e;
        }
    }

    public static void writeUnauthorized(HttpServletResponse response, String message) throws IOException {
        writeJson(response, HttpStatus.UNAUTHORIZED.value(), ApiResponse.fail(401, message));
    }

    public static void writeForbidden(HttpServletResponse response, String message) throws IOException {
        writeJson(response, HttpStatus.FORBIDDEN.value(), ApiResponse.fail(403, message));
    }

    public static void writeBadRequest(HttpServletResponse response, String message) throws IOException {
        writeJson(response, HttpStatus.BAD_REQUEST.value(), ApiResponse.fail(400, message));
    }
}
