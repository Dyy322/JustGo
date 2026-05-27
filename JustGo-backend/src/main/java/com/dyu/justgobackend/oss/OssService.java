package com.dyu.justgobackend.oss;

import com.aliyun.oss.OSS;
import com.aliyun.oss.OSSClientBuilder;
import com.aliyun.oss.model.GeneratePresignedUrlRequest;
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
        runSelfTest();
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
