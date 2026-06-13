package com.zzy.aurenteasebackend.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.Date;

@Component
public class JwtUtils {
    public static final String BLACKLIST_PREFIX = "rentease:jwt:blacklist:";


    @Value("${app.jwt.secret}")
    private String jwtSecret;

    /**
     * 解析 JWT 的 Claims
     * @param token
     * @return
     */
    public Claims getClaimsFromToken(String token) {
        return Jwts.parserBuilder()
                .setSigningKey(jwtSecret.getBytes())
                .build()
                .parseClaimsJws(token).getBody();
    }

    /**
     * 计算 Token 的剩余有效时间（毫秒）
     * @param token
     * @return
     */
    public long getRemainingLifetime(String token) {
        try{
            Claims claims=getClaimsFromToken(token);
            Date expiration=claims.getExpiration();
            long now=System.currentTimeMillis();
            long remain=expiration.getTime() - now;
            return Math.max(remain,0);
        }catch (Exception e){
            return 0;
        }
    }
}
