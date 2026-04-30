package com.dyu.justgobackend.aspect;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.node.TextNode;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.util.Locale;
import java.util.Set;

/**
 * 将方法参数序列化为可审计字符串，并对敏感字段脱敏（递归处理嵌套对象 / 集合）。
 */
@Component
public class LogArgumentSanitizer {

    private static final Set<Class<?>> SKIP_DEEP_TO_STRING = Set.of(
            Class.class,
            MultipartFile.class
    );

    private final ObjectMapper objectMapper;

    public LogArgumentSanitizer(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public String toSafeJson(Object value, String maskPlaceholder) {
        if (value == null) {
            return "null";
        }
        if (value instanceof ServletRequest) {
            return "[%s]".formatted(value.getClass().getSimpleName());
        }
        if (value instanceof ServletResponse) {
            return "[%s]".formatted(value.getClass().getSimpleName());
        }
        for (Class<?> skip : SKIP_DEEP_TO_STRING) {
            if (skip.isInstance(value)) {
                return "[%s]".formatted(value.getClass().getSimpleName());
            }
        }
        try {
            JsonNode tree = objectMapper.valueToTree(value);
            mask(tree, maskPlaceholder);
            return objectMapper.writeValueAsString(tree);
        } catch (Exception ignored) {
            return "\"<%s: serialize skipped>\"".formatted(value.getClass().getSimpleName());
        }
    }

    private void mask(JsonNode node, String maskPlaceholder) {
        if (node instanceof ObjectNode objectNode) {
            var fieldNames = objectNode.fieldNames();
            while (fieldNames.hasNext()) {
                String name = fieldNames.next();
                JsonNode child = objectNode.get(name);
                if (isSensitiveKey(name)) {
                    objectNode.set(name, TextNode.valueOf(maskPlaceholder));
                } else {
                    mask(child, maskPlaceholder);
                }
            }
        } else if (node instanceof ArrayNode arrayNode) {
            for (JsonNode element : arrayNode) {
                mask(element, maskPlaceholder);
            }
        }
    }

    private boolean isSensitiveKey(String key) {
        String k = key.toLowerCase(Locale.ROOT);
        return k.contains("password")
                || k.contains("passwd")
                || k.contains("pwd")
                || k.contains("secret")
                || key.equalsIgnoreCase("authorization")
                || k.contains("token")
                || k.contains("credential")
                || k.contains("idcard")
                || k.contains("credit");
    }
}
