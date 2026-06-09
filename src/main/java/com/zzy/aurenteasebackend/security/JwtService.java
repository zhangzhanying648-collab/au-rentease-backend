package com.zzy.aurenteasebackend.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.security.Key;
import java.util.Date;
import java.util.function.Function;

@Service
public class JwtService {
    // 💡 澳洲大厂生产铁律：密钥绝对不能硬编码在代码里！这里仅作演示。生产环境应通过环境变量注入
    //加密的“盐”。HS256 算法要求这个字符串至少包含 256 位（32 字节）以上的字符。

    @Value("${app.jwt.secret}")
    private  String SECRET_KEY;

    // 💡 Token 有效期设为 24 小时
    @Value("${app.jwt.expiration}")
    private  long JWT_EXPIRATION;

    //将你的字符串密钥转成字节数组，通过 Keys.hmacShaKeyFor 生成一个真正的安全加密密钥对象（Key），供后面签名使用。
    private Key getSigningKey(){
        return Keys.hmacShaKeyFor(SECRET_KEY.getBytes());
    }

    //根据用户名生成签名
    //当用户登录成功，我们要发给他们一张通行证（Token）：
    //用 Date 而不用 LocalDateTime原因：jwt诞生的时候，LocalDateTime还没出来
    //这里的new Date都是函数中的局部变量，不存在跨多线程共享问题，没有线程安全问题。
    public String generateToken(String username){
        return Jwts.builder()                   // 开启 JWT 组装流水线
                .setSubject(username)           // 把“用户名”塞进箱子（作为核心主体）
                .setIssuedAt(new Date(System.currentTimeMillis()))      // 记下“发证时间”为当前时间
                .setExpiration(new Date(System.currentTimeMillis()+JWT_EXPIRATION)) // 记下“过期时间”（24小时后）
                .signWith(getSigningKey(), SignatureAlgorithm.HS256) //🌟 核心：用我们的钥匙和 HS256 算法在箱子外贴上防伪防篡改签名
                .compact();  // 压缩、打包装箱，变成一串用“.”隔开的经典 JWT 三段式字符串
    }

    //从token中提取用户名
    public String extractUsername(String token){
        return extractClaim(token, Claims::getSubject);

    }

    //extractClaim 用来刷钥匙开箱拿里面的单据
    //Function<Claims, T> 怎么理解？
    //它是 Java 的函数式接口。意思是：“我把解密出来的整张单据（Claims）交给你，你告诉我你要拿什么字段（返回 T 类型）。
    //Claims::getSubject 就是那个提取指令。
    //相当于告诉 extractClaim：“开箱后，帮我调用 claims.getSubject() 把用户名拿出来返回给我
    private <T> T extractClaim(String token, Function<Claims, T> claimsResolver) {
        final Claims claims=Jwts.parserBuilder()  // 开启 JWT 解密流
                .setSigningKey(getSigningKey()) // 传入同一把钥匙（必须是同一把，否则报错）
                .build() // 构建解密器
                .parseClaimsJws(token) // 验签并解密 Token（如果 Token 被人篡改过，这一步直接抛异常）
                .getBody();  // 成功后，拿到里面所有的“单据（Claims）”
        return claimsResolver.apply(claims);// 🌟 核心：执行传进来的提取指令（如拿用户名或拿过期时间）
    }

    public boolean validateToken(String token, String username){
        String extractUsername = extractUsername(token);
        return extractUsername.equals(username)&&!isTokenExpired(token);
    }

    private boolean isTokenExpired(String token) {
        return extractExpiration(token).before(new Date());
    }

    private Date extractExpiration(String token) {
        return extractClaim(token, Claims::getExpiration);
    }

    public String extractRole(String token) {
        return extractClaim(token, claims -> claims.get("role", String.class));
    }
}
