package com.dyu.justgobackend.oss;

import com.aliyun.oss.OSS;
import com.aliyun.oss.OSSClientBuilder;
import com.aliyun.oss.common.utils.DateUtil;
import com.aliyun.oss.model.GeneratePresignedUrlRequest;
import com.dyu.justgobackend.dto.response.UploadTokenResponse;
import com.dyu.justgobackend.exception.BusinessException;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import org.springframework.stereotype.Service;

import java.net.URL;
import java.util.Date;
import java.util.UUID;

@Service
public class OssService {

    private static final String URL_TEMPLATE = "https://%s.%s/%s";
    private static final String[] ALLOWED_EXTENSIONS = {"jpg", "jpeg", "png", "gif", "webp"};

    private final OssProperties properties;
    private OSS client;

    public OssService(OssProperties properties) {
        this.properties = properties;
    }

    @PostConstruct
    void init() {
        if (properties.endpoint().isBlank() || properties.bucketName().isBlank()) {
            return;
        }
        this.client = new OSSClientBuilder()
                .build(properties.endpoint(), properties.accessKeyId(), properties.accessKeySecret());
    }

    @PreDestroy
    void destroy() {
        if (client != null) {
            client.shutdown();
        }
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
        return new UploadTokenResponse(signedUrl.toString(), fileUrl, objectKey, properties.urlExpireSeconds());
    }

    private void ensureConfigured() {
        if (client == null) {
            throw new BusinessException(503, "OSS 未配置，请设置 OSS_ENDPOINT 等环境变量");
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
