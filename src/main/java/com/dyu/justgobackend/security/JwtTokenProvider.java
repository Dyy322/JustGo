package com.dyu.justgobackend.security;

import com.dyu.justgobackend.entity.User;
import com.dyu.justgobackend.exception.BusinessException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

@Component
public class JwtTokenProvider {
    private static final String HMAC_SHA256 = "HmacSHA256";
    private static final TypeReference<Map<String, Object>> MAP_TYPE = new TypeReference<>() {
    };

    private final JwtProperties properties;
    // 在 Java 对象和 JSON 格式之间进行双向转换
    private final ObjectMapper objectMapper;
    private final Base64.Encoder urlEncoder = Base64.getUrlEncoder().withoutPadding();
    private final Base64.Decoder urlDecoder = Base64.getUrlDecoder();

    public JwtTokenProvider(JwtProperties properties, ObjectMapper objectMapper) {
        this.properties = properties;
        this.objectMapper = objectMapper;
    }

    public String createToken(User user) {
        Instant now = Instant.now();
        Instant expiresAt = now.plus(properties.expiration());

        Map<String, Object> header = new LinkedHashMap<>();
        header.put("alg", "HS256");
        header.put("typ", "JWT");

        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("iss", properties.issuer());
        // Subject
        payload.put("sub", user.getId().toString());
        payload.put("username", user.getUsername());
        payload.put("accountType", user.getAccountType());
        payload.put("jti", UUID.randomUUID().toString());
        // Issue At 签发时间
        payload.put("iat", now.getEpochSecond());
        // Expiration Time 过期时间
        payload.put("exp", expiresAt.getEpochSecond());

        String unsignedToken = base64Json(header) + "." + base64Json(payload);
        return unsignedToken + "." + sign(unsignedToken);
    }

    public ParsedAccessToken parseAccessToken(String token) {
        String[] parts = token.split("\\.");
        if (parts.length != 3) {
            throw new BusinessException(401, "无效的登录凭证，令牌格式错误");
        }

        String unsignedToken = parts[0] + "." + parts[1];
        if (!MessageDigest.isEqual(sign(unsignedToken).getBytes(StandardCharsets.UTF_8),
                parts[2].getBytes(StandardCharsets.UTF_8))) {
            throw new BusinessException(401, "无效的登录凭证，签名验证失败");
        }

        Map<String, Object> payload = readJson(parts[1]);
        if (!properties.issuer().equals(payload.get("iss"))) {
            throw new BusinessException(401, "无效的登录凭证，发者验证失败");
        }

        long expiresAt = numberValue(payload.get("exp"), "exp").longValue();
        if (Instant.now().getEpochSecond() >= expiresAt) {
            throw new BusinessException(401, "登录凭证已过期");
        }

        Object jtiObj = payload.get("jti");
        if (jtiObj == null) {
            throw new BusinessException(401, "令牌缺少 jti，请重新登录");
        }
        String jti = String.valueOf(jtiObj).trim();
        validateJti(jti);

        Long userId = Long.valueOf(String.valueOf(payload.get("sub")));
        String username = String.valueOf(payload.get("username"));
        Integer accountType = numberValue(payload.get("accountType"), "accountType").intValue();
        LoginUser loginUser = new LoginUser(userId, username, accountType);
        return new ParsedAccessToken(loginUser, jti, expiresAt);
    }

    public long expiresInSeconds() {
        return properties.expiration().toSeconds();
    }

    private String base64Json(Map<String, Object> value) {
        try {
            return urlEncoder.encodeToString(objectMapper.writeValueAsBytes(value));
        } catch (Exception exception) {
            throw new IllegalStateException("JWT 序列化失败", exception);
        }
    }

    private Map<String, Object> readJson(String base64Value) {
        try {
            byte[] json = urlDecoder.decode(base64Value);
            return objectMapper.readValue(json, MAP_TYPE);
        } catch (Exception exception) {
            throw new BusinessException(401, "无效的登录凭证");
        }
    }

    private String sign(String value) {
        try {
            Mac mac = Mac.getInstance(HMAC_SHA256);
            mac.init(new SecretKeySpec(properties.secret().getBytes(StandardCharsets.UTF_8), HMAC_SHA256));
            return urlEncoder.encodeToString(mac.doFinal(value.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception exception) {
            throw new IllegalStateException("JWT 签名失败", exception);
        }
    }

    private Number numberValue(Object value, String fieldName) {
        if (value instanceof Number number) {
            return number;
        }
        throw new BusinessException(401, "无效的登录凭证: " + fieldName);
    }

    /**
     * 校验 JTI 字段，确保其格式正确
     */
    private void validateJti(String jti) {
        try {
            UUID.fromString(jti);
        } catch (IllegalArgumentException ignored) {
            throw new BusinessException(401, "无效的登录凭证，jti 格式错误");
        }
    }
}
