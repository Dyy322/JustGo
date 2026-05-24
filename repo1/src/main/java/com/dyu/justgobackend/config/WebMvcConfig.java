package com.dyu.justgobackend.config;

import com.dyu.justgobackend.security.JwtAuthenticationInterceptor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Spring Web MVC 核心配置类
 * 负责处理跨域资源共享（CORS）及请求拦截器注册
 */
@Configuration
public class WebMvcConfig implements WebMvcConfigurer {
    private final CorsProperties corsProperties;
    private final JwtAuthenticationInterceptor jwtAuthenticationInterceptor;

    public WebMvcConfig(CorsProperties corsProperties, JwtAuthenticationInterceptor jwtAuthenticationInterceptor) {
        this.corsProperties = corsProperties;
        this.jwtAuthenticationInterceptor = jwtAuthenticationInterceptor;
    }

    /**
     * 配置全局跨域映射规则，允许指定域名和方法访问接口
     *
     * @param registry CORS 注册表
     */
    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/**")
                .allowedOrigins(corsProperties.allowedOrigins().toArray(String[]::new))
                .allowedMethods(corsProperties.allowedMethods().toArray(String[]::new))
                .allowedHeaders(corsProperties.allowedHeaders().toArray(String[]::new))
                .allowCredentials(corsProperties.allowCredentials())
                .maxAge(corsProperties.maxAge());
    }

    /**
     * 注册 JWT 认证拦截器，对 API 请求进行身份校验并排除公开接口
     *
     * @param registry 拦截器注册表
     */
    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(jwtAuthenticationInterceptor)
                .addPathPatterns("/api/**")
                .excludePathPatterns(
                        "/api/auth/login",
                        "/api/auth/register",
                        "/api/auth/refresh",
                        "/api/health"
                );
    }
}