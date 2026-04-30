package com.dyu.justgobackend.util;

import com.dyu.justgobackend.common.ApiResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

@Component
public class HttpServletResponseUtils {

    private final ObjectMapper objectMapper;

    public HttpServletResponseUtils(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public void writeJson(HttpServletResponse response, int httpStatus, ApiResponse<?> body) throws IOException {
        response.setStatus(httpStatus);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        objectMapper.writeValue(response.getWriter(), body);
    }

    public void writeUnauthorized(HttpServletResponse response, String message) throws IOException {
        writeJson(response, HttpStatus.UNAUTHORIZED.value(), ApiResponse.fail(401, message));
    }

    public void writeForbidden(HttpServletResponse response, String message) throws IOException {
        writeJson(response, HttpStatus.FORBIDDEN.value(), ApiResponse.fail(403, message));
    }

    public void writeBadRequest(HttpServletResponse response, String message) throws IOException {
        writeJson(response, HttpStatus.BAD_REQUEST.value(), ApiResponse.fail(400, message));
    }
}
