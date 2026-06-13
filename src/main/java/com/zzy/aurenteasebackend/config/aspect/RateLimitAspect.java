package com.zzy.aurenteasebackend.config.aspect;

import org.apache.catalina.util.RateLimiter;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Aspect
@Component
public class RateLimitAspect {

    // 用一个并发 Map 在内存中维护各个方法的限流器
    private final Map<String, RateLimiter> limiters = new ConcurrentHashMap<>();

    @Around("@annotation(rateLimited)")
    public Object doAround(ProceedingJoinPoint joinPoint, RateLimited rateLimited) throws Throwable {
        // 拿到目标方法的完全限定名作为 Map 的 Key
        String methodName = joinPoint.getSignature().toLongString();

        // 如果不存在，则根据注解传入的 QPS 创建一个新的限流器
        RateLimiter limiter = limiters.computeIfAbsent(methodName,
                k -> RateLimiter.create(rateLimited.qps()));

        // 尝试拿令牌
        if (!limiter.tryAcquire()) {
            throw new ResponseStatusException(HttpStatus.TOO_MANY_REQUESTS, "Rate limit exceeded.");
        }

        return joinPoint.proceed(); // 拿到令牌，放行执行原方法
    }
}