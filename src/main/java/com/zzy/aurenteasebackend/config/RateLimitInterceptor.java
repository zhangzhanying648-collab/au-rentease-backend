package com.zzy.aurenteasebackend.config;

import com.zzy.aurenteasebackend.controller.PropertyController;
import io.opentelemetry.sdk.internal.RateLimiter;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.redisson.api.RateLimiterConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.web.servlet.HandlerInterceptor;

import java.util.concurrent.TimeUnit;

/**
 * 限制同一个ip，1秒内不能超过5次接口调用
 */
@Configuration
@RequiredArgsConstructor
public class RateLimitInterceptor implements HandlerInterceptor {
    private final RedisTemplate<String, Object> redisTemplate;
    private static final Logger log = LoggerFactory.getLogger(RateLimitInterceptor.class);

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        log.info("preHandle");
        String ip = request.getRemoteAddr();
        String uri = request.getRequestURI();

        // 2. 组装 Redis 计数器 Key
        String redisKey = "rentease:ratelimit:" + ip + ":" + uri;

        // 3. 🚀 执行 Redis 原子自增操作
        Long count = redisTemplate.opsForValue().increment(redisKey);

        if (count != null && count == 1) {
            // 如果是这 1 秒钟内的第一次访问，设置 1 秒后自动过期销毁
            redisTemplate.expire(redisKey, 1, TimeUnit.SECONDS);
        }

        if (count != null && count > 5) {
            log.info("Too many requests. Please slow down! (触发防爬虫安全机制)");
            // 4. 🛡️ 触发防御：1秒内超过5次，直接拦截并抛出 429 状态码
            response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
            response.setContentType("application/json;charset=UTF-8");
            response.getWriter().write("{\"success\":false,\"message\":\"Too many requests. Please slow down! (触发防爬虫安全机制)\"}");

            return false; // 拦截，请求不再往下传递到 Controller
        }
        return true; // 放行
    }
}
