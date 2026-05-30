package com.dyu.justgobackend.oss;

import com.aliyun.oss.OSS;
import com.aliyun.oss.OSSClientBuilder;
import com.aliyun.oss.OSSException;
import com.aliyun.oss.model.CORSConfiguration;
import com.aliyun.oss.model.GenericRequest;
import com.aliyun.oss.model.GeneratePresignedUrlRequest;
import com.aliyun.oss.model.SetBucketCORSRequest;
import com.dyu.justgobackend.dto.response.oss.UploadTokenResponse;
import com.dyu.justgobackend.exception.BusinessException;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Date;
import java.util.List;
import java.util.UUID;

@Service
public class OssService {

    private static final Logger log = LoggerFactory.getLogger(OssService.class);
    private static final String URL_TEMPLATE = "https://%s.%s/%s";

    private final OssProperties properties;
    private OSS client;

    public OssService(OssProperties properties) {
        this.properties = properties;
    }

    @PostConstruct
    void init() {
        if (properties.endpoint().isBlank() || properties.bucketName().isBlank()) {
            log.warn("OSS 未配置 endpoint/bucketName，文件上传功能不可用");
            return;
        }
        if (properties.accessKeyId().isBlank() || properties.accessKeySecret().isBlank()) {
            log.warn("OSS 凭证为空 (OSS_ACCESS_KEY_ID / OSS_ACCESS_KEY_SECRET)，文件上传功能不可用");
            return;
        }
        this.client = new OSSClientBuilder()
                .build(properties.endpoint(), properties.accessKeyId(), properties.accessKeySecret());
        log.info("OSS client 初始化完成，endpoint={}, bucket={}", properties.endpoint(), properties.bucketName());
        configureBucketCors();
        runSelfTest();
    }

    private void configureBucketCors() {
        SetBucketCORSRequest request = new SetBucketCORSRequest(properties.bucketName());
        SetBucketCORSRequest.CORSRule rule = new SetBucketCORSRequest.CORSRule();
        rule.addAllowdOrigin("*");
        rule.addAllowedMethod("PUT");
        rule.addAllowedMethod("GET");
        rule.addAllowedMethod("HEAD");
        rule.addAllowedHeader("*");
        rule.addExposeHeader("ETag");
        rule.addExposeHeader("x-oss-request-id");
        rule.addExposeHeader("x-oss-version-id");
        rule.setMaxAgeSeconds(3600);
        request.addCorsRule(rule);

        try {
            client.setBucketCORS(request);
            log.info("OSS Bucket CORS 配置成功");
        } catch (OSSException e) {
            if ("AccessDenied".equals(e.getErrorCode())) {
                log.error(
                        "OSS CORS 配置失败 — 当前 RAM 用户缺少 PutBucketCORS 权限。"
                                + "请在 RAM 控制台为用户附加 AliyunOSSFullAccess 或自定义 oss:PutBucketCORS 权限。"
                                + "RequestId={}, Code={}, Message={}",
                        e.getRequestId(), e.getErrorCode(), e.getErrorMessage());
            } else {
                log.error(
                        "OSS CORS 配置失败 — {}: {}. RequestId={}",
                        e.getErrorCode(), e.getErrorMessage(), e.getRequestId());
            }
            return;
        } catch (Exception e) {
            log.error("OSS CORS 配置失败 — {}: {}", e.getClass().getSimpleName(), e.getMessage());
            return;
        }

        // 读回校验 CORS 规则是否生效
        try {
            CORSConfiguration config = client.getBucketCORS(new GenericRequest(properties.bucketName()));
            List<SetBucketCORSRequest.CORSRule> rules = config.getCorsRules();
            if (rules.isEmpty()) {
                log.warn("OSS CORS 已设置但读回列表为空，可能存在同步延迟，请稍后在控制台确认");
            } else {
                log.info("OSS CORS 读回校验通过，当前 {} 条规则", rules.size());
            }
        } catch (Exception e) {
            log.warn("OSS CORS 读回校验失败 — {}: {}。CORS 可能未生效，请登录控制台手动配置", e.getClass().getSimpleName(), e.getMessage());
        }
    }

    private void runSelfTest() {
        try {
            String testKey = properties.basePath() + ".selftest/" + UUID.randomUUID() + ".txt";
            GeneratePresignedUrlRequest req = new GeneratePresignedUrlRequest(
                    properties.bucketName(), testKey, com.aliyun.oss.HttpMethod.PUT);
            req.setExpiration(new Date(System.currentTimeMillis() + 60000));
            req.setContentType("text/plain");
            URL signedUrl = client.generatePresignedUrl(req);

            HttpURLConnection conn = (HttpURLConnection) signedUrl.openConnection();
            conn.setRequestMethod("PUT");
            conn.setDoOutput(true);
            conn.setRequestProperty("Content-Type", "text/plain");
            conn.setConnectTimeout(5000);
            conn.setReadTimeout(5000);
            byte[] body = "oss-self-test".getBytes();
            conn.setFixedLengthStreamingMode(body.length);
            try (OutputStream os = conn.getOutputStream()) {
                os.write(body);
            }
            int status = conn.getResponseCode();
            conn.disconnect();

            if (status == 200) {
                log.info("OSS 自检通过 — 预签名 PUT 上传成功");
                try { client.deleteObject(properties.bucketName(), testKey); } catch (Exception ignored) {}
            } else {
                log.error("OSS 自检失败 — 预签名 PUT 返回 HTTP {}，请检查 OSS 凭证和 Bucket 权限", status);
            }
        } catch (Exception e) {
            log.error("OSS 自检失败 — {}: {}", e.getClass().getSimpleName(), e.getMessage());
        }
    }

    @PreDestroy
    void destroy() {
        if (client != null) {
            client.shutdown();
        }
    }

    public CorsStatus getCorsStatus() {
        if (client == null) {
            return new CorsStatus(false, "OSS Client 未初始化", null);
        }
        try {
            CORSConfiguration config = client.getBucketCORS(new GenericRequest(properties.bucketName()));
            List<SetBucketCORSRequest.CORSRule> rules = config.getCorsRules();
            if (rules.isEmpty()) {
                return new CorsStatus(false, "Bucket 未配置 CORS 规则，浏览器直传将被拦截", null);
            }
            List<CorsStatus.RuleSummary> summaries = rules.stream()
                    .map(r -> new CorsStatus.RuleSummary(
                            r.getAllowedOrigins(), r.getAllowedMethods(), r.getAllowedHeaders()))
                    .toList();
            return new CorsStatus(true, "已配置 " + rules.size() + " 条 CORS 规则", summaries);
        } catch (OSSException e) {
            if ("AccessDenied".equals(e.getErrorCode())) {
                return new CorsStatus(false, "无权限读取 CORS 配置（缺少 GetBucketCORS 权限），请登录阿里云控制台手动确认", null);
            }
            return new CorsStatus(false, "查询 CORS 失败: " + e.getErrorMessage(), null);
        } catch (Exception e) {
            return new CorsStatus(false, "查询 CORS 异常: " + e.getMessage(), null);
        }
    }

    public record CorsStatus(boolean configured, String message, List<RuleSummary> rules) {
        public record RuleSummary(List<String> origins, List<String> methods, List<String> headers) {}
    }

    public String generatePresignedGetUrl(String objectKey) {
        ensureConfigured();
        Date expiration = new Date(System.currentTimeMillis() + properties.urlExpireSeconds() * 1000);
        GeneratePresignedUrlRequest request = new GeneratePresignedUrlRequest(
                properties.bucketName(), objectKey, com.aliyun.oss.HttpMethod.GET);
        request.setExpiration(expiration);
        URL signedUrl = client.generatePresignedUrl(request);
        return signedUrl.toString();
    }

    public UploadTokenResponse generateUploadToken(String prefix, String ext) {
        ensureConfigured();
        String objectKey = properties.basePath() + prefix + "/" + UUID.randomUUID() + "." + ext;
        String fileUrl = String.format(URL_TEMPLATE, properties.bucketName(), properties.endpoint(), objectKey);

        Date expiration = new Date(System.currentTimeMillis() + properties.urlExpireSeconds() * 1000);
        GeneratePresignedUrlRequest request = new GeneratePresignedUrlRequest(
                properties.bucketName(), objectKey, com.aliyun.oss.HttpMethod.PUT);
        request.setExpiration(expiration);
        request.setContentType(contentType(ext));

        URL signedUrl = client.generatePresignedUrl(request);
        log.debug("生成预签名 URL: {}", signedUrl);
        return new UploadTokenResponse(signedUrl.toString(), fileUrl, objectKey, properties.urlExpireSeconds());
    }

    private void ensureConfigured() {
        if (client == null) {
            throw new BusinessException(503, "OSS 未配置，请设置 OSS_ENDPOINT 等环境变量");
        }
        if (properties.accessKeyId().isBlank() || properties.accessKeySecret().isBlank()) {
            throw new BusinessException(503, "OSS 凭证未配置，请设置 OSS_ACCESS_KEY_ID 和 OSS_ACCESS_KEY_SECRET 环境变量");
        }
    }

    private static String contentType(String ext) {
        return switch (ext) {
            case "jpg", "jpeg" -> "image/jpeg";
            case "png" -> "image/png";
            case "gif" -> "image/gif";
            case "webp" -> "image/webp";
            default -> "application/octet-stream";
        };
    }

}
