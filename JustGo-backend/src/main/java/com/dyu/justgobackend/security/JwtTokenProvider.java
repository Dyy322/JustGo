package com.dyu.justgobackend.security;

import com.dyu.justgobackend.entity.user.User;
import com.dyu.justgobackend.enums.TokenKind;
import com.dyu.justgobackend.exception.BusinessException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Duration;
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

    public String createAccessToken(User user) {
        return createSignedToken(user, TokenKind.ACCESS, properties.expiration());
    }

    public String createRefreshToken(User user) {
        return createSignedToken(user, TokenKind.REFRESH, properties.refreshExpiration());
    }

    private String createSignedToken(User user, TokenKind kind, Duration ttl) {
        Instant now = Instant.now();
        Instant expiresAt = now.plus(ttl);

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
        payload.put("token_use", kind.claimValue());

        String unsignedToken = base64Json(header) + "." + base64Json(payload);
        return unsignedToken + "." + sign(unsignedToken);
    }

    public ParsedToken parseAccessToken(String token) {
        return parseToken(token, TokenKind.ACCESS);
    }

    /** 校验 refresh token；不检查黑名单（由业务在轮换前调用 {@link JwtDenylistService#isDenied}）。 */
    public ParsedToken parseRefreshToken(String token) {
        return parseToken(token, TokenKind.REFRESH);
    }

    private ParsedToken parseToken(String token, TokenKind expectedKind) {
        Map<String, Object> payload = parseAndVerifyPayload(token);
        assertTokenUse(payload, expectedKind);

        long expiresAtEpoch = numberValue(payload.get("exp"), "exp").longValue();
        if (Instant.now().getEpochSecond() >= expiresAtEpoch) {
            throw new BusinessException(401, expectedKind.expiredMessage());
        }

        String jti = requireJti(payload);
        LoginUser loginUser = readLoginUser(payload);
        return new ParsedToken(loginUser, jti, expiresAtEpoch);
    }

    public long expiresInSeconds() {
        return properties.expiration().toSeconds();
    }

    public long refreshExpiresInSeconds() {
        return properties.refreshExpiration().toSeconds();
    }

    private Map<String, Object> parseAndVerifyPayload(String token) {
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
        return payload;
    }

    private static void assertTokenUse(Map<String, Object> payload, TokenKind expected) {
        Object use = payload.get("token_use");
        String actual = use == null ? "" : String.valueOf(use);
        if (expected == TokenKind.ACCESS) {
            if (TokenKind.REFRESH.claimValue().equals(actual)) {
                throw new BusinessException(401, "请使用访问令牌调用该接口");
            }
            return;
        }
        if (!expected.claimValue().equals(actual)) {
            throw new BusinessException(401, "无效的刷新令牌");
        }
    }

    private String requireJti(Map<String, Object> payload) {
        Object jtiObj = payload.get("jti");
        if (jtiObj == null) {
            throw new BusinessException(401, "令牌缺少 jti，请重新登录");
        }
        String jti = String.valueOf(jtiObj).trim();
        validateJti(jti);
        return jti;
    }

    private LoginUser readLoginUser(Map<String, Object> payload) {
        Long userId = Long.valueOf(String.valueOf(payload.get("sub")));
        String username = String.valueOf(payload.get("username"));
        Integer accountType = numberValue(payload.get("accountType"), "accountType").intValue();
        return new LoginUser(userId, username, accountType);
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
