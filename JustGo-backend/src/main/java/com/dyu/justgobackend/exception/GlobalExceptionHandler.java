package com.dyu.justgobackend.exception;

import com.dyu.justgobackend.common.ApiResponse;
import jakarta.validation.ConstraintViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.stream.Collectors;

@RestControllerAdvice
public class GlobalExceptionHandler {
    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ApiResponse<Void>> handleBusinessException(BusinessException exception) {
        return ResponseEntity
                .status(resolveStatus(exception.getCode()))
                .body(ApiResponse.fail(exception.getCode(), exception.getMessage()));
    }

    /**
     * 处理 @Valid 参数校验失败异常，提取所有字段错误信息并拼接返回
     *
     * @param exception 方法参数校验异常对象
     * @return 包含校验错误信息的 400 响应
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<Void>> handleValidationException(MethodArgumentNotValidException exception) {
        String message = exception.getBindingResult()
                .getFieldErrors()
                .stream()
                .map(this::formatFieldError)
                .collect(Collectors.joining("; "));
        return ResponseEntity.badRequest().body(ApiResponse.fail(400, message));
    }

    /**
     * 处理约束违规异常（通常用于 GET 请求参数校验）
     *
     * @param exception 约束违规异常对象
     * @return 包含错误信息的 400 响应
     */
    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ApiResponse<Void>> handleConstraintViolationException(ConstraintViolationException exception) {
        return ResponseEntity.badRequest().body(ApiResponse.fail(400, exception.getMessage()));
    }

    /**
     * 处理全局未捕获的通用异常，防止敏感信息泄露并返回统一错误提示
     *
     * @param exception 异常对象
     * @return 服务器内部错误 500 响应
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Void>> handleException(Exception exception) {
        return ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponse.fail(500, "服务器内部错误"));
    }

    private String formatFieldError(FieldError error) {
        return error.getField() + ": " + error.getDefaultMessage();
    }

    private HttpStatus resolveStatus(int code) {
        return HttpStatus.resolve(code) == null ? HttpStatus.BAD_REQUEST : HttpStatus.valueOf(code);
    }
}
