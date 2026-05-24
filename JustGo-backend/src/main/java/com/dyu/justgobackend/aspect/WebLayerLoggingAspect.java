package com.dyu.justgobackend.aspect;

import com.dyu.justgobackend.common.ApiResponse;
import com.dyu.justgobackend.util.HttpServletRequestUtils;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.util.AntPathMatcher;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.util.ArrayList;
import java.util.List;

/**
 * REST 入口统一审计日志：HTTP 语义、脱敏入参、耗时、结果摘要与异常。
 */
@Aspect
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
@Slf4j
@ConditionalOnProperty(prefix = "app.observability.web-log", name = "enabled", havingValue = "true", matchIfMissing = true)
public class WebLayerLoggingAspect {

    private final WebLayerLoggingProperties properties;
    private final LogArgumentSanitizer sanitizer;
    private final AntPathMatcher pathMatcher = new AntPathMatcher();

    public WebLayerLoggingAspect(WebLayerLoggingProperties properties, LogArgumentSanitizer sanitizer) {
        this.properties = properties;
        this.sanitizer = sanitizer;
    }

    @Pointcut("@within(org.springframework.web.bind.annotation.RestController)")
    public void restController() {
    }

    @Around("restController()")
    public Object logAround(ProceedingJoinPoint joinPoint) throws Throwable {
        HttpServletRequest request = currentRequest();
        String uri = request != null ? request.getRequestURI() : "";
        if (request != null && isExcluded(uri)) {
            return joinPoint.proceed();
        }

        long startNs = System.nanoTime();
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        String handler = signature.getDeclaringType().getSimpleName() + "." + signature.getName();

        try {
            if (log.isInfoEnabled()) {
                log.info(
                        "[begin-{}] | method={} uri={} query={} clientIp={} args={}",
                        handler,
                        request != null ? request.getMethod() : "-",
                        uri,
                        request != null && request.getQueryString() != null ? request.getQueryString() : "",
                        request != null && properties.includeClientIp() ? HttpServletRequestUtils.clientIp(request) : "-",
                        formatArgs(joinPoint.getArgs()));
            }

            Object result = joinPoint.proceed();

            long durationMs = (System.nanoTime() - startNs) / 1_000_000L;
            if (durationMs >= properties.slowThresholdMs() && log.isWarnEnabled()) {
                log.warn("[slow-{}] | durationMs={} thresholdMs={} handler={} outcome=success",
                        handler, durationMs, properties.slowThresholdMs(), handler);
            } else if (log.isInfoEnabled()) {
                log.info("[end-{}] | durationMs={} handler={} outcome=success result={}",
                        handler, durationMs, handler, summarizeResult(result));
            }
            return result;

        } catch (Throwable ex) {
            long durationMs = (System.nanoTime() - startNs) / 1_000_000L;
            log.error("[end-{}] | durationMs={} handler={} outcome=error errorType={} message={}",
                    handler, durationMs, handler, ex.getClass().getName(), ex.getMessage(), ex);
            throw ex;
        }
    }

    private List<String> formatArgs(Object[] args) {
        List<String> list = new ArrayList<>(args.length);
        for (Object arg : args) {
            list.add(sanitizer.toSafeJson(arg, properties.maskPlaceholder()));
        }
        return list;
    }

    private static String summarizeResult(Object result) {
        if (result instanceof ApiResponse<?> apiResponse) {
            String dataPart = apiResponse.data() == null
                    ? "null"
                    : apiResponse.data().getClass().getSimpleName();
            return "ApiResponse[code=%d,message=%s,data=%s]"
                    .formatted(apiResponse.code(), apiResponse.message(), dataPart);
        }
        if (result == null) {
            return "null";
        }
        return result.getClass().getSimpleName();
    }

    private boolean isExcluded(String uri) {
        for (String pattern : properties.excludePathPatterns()) {
            if (pathMatcher.match(pattern, uri)) {
                return true;
            }
        }
        return false;
    }

    private static HttpServletRequest currentRequest() {
        RequestAttributes attrs = RequestContextHolder.getRequestAttributes();
        if (attrs instanceof ServletRequestAttributes servletAttrs) {
            return servletAttrs.getRequest();
        }
        return null;
    }
}
