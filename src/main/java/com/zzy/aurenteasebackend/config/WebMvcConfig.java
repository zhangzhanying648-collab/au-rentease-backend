package com.zzy.aurenteasebackend.config;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
@RequiredArgsConstructor
public class WebMvcConfig implements WebMvcConfigurer {

    private final RateLimitInterceptor rateLimitInterceptor;

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        // 🌟 核心：让所有经过 /api/ 的业务接口全部接受 Redis 限流过滤器的审查
        registry.addInterceptor(rateLimitInterceptor)
                .addPathPatterns("/api/**");
    }
}