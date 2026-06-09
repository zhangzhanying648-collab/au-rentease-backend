package com.zzy.aurenteasebackend.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.util.List;

/**
 * 企业级全栈跨域安全配置
 * 拒绝直接使用狂野的 "*" 允许所有跨域，而是通过环境变量动态精准注入，兼顾本地开发与生产安全。
 */
@Configuration
public class CorsConfig {
    //从配置文件/环境变量中读取允许的跨域来源，默认允许本地 React 开发端口
    @Value("${app.cors.allowed-origin:http://localhost:3000,http://localhost:5173}")
    private List<String> allowedOrigins;

    @Bean
    public WebMvcConfigurer corsConfigurer() {
        return new WebMvcConfigurer() {
            @Override
            public void addCorsMappings(CorsRegistry registry) {
                registry.addMapping("/**")//拦截所有后端接口路由
                        .allowedOrigins(allowedOrigins.toArray(new String[0]))  //准确放行信任的前端
                        .allowedMethods("GET", "POST", "PUT", "DELETE","OPTIONS","PATCH")
                        .allowedHeaders("*")    //允许携带任何自定义请求头
                        .exposedHeaders("Authorization")    //暴漏特殊的响应头,方便前端React提取Jwt Token
                        .allowCredentials(true)        //允许前端携带cookie或认证凭证
                        .maxAge(3600);  //预检请求（OPTIONS）的缓存时间（秒），避免前端频繁发送无意义的预检请求

            }
        };
    }

}
