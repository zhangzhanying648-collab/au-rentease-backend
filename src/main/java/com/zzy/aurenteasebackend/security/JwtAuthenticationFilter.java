package com.zzy.aurenteasebackend.security;

import com.zzy.aurenteasebackend.repository.UserRepository;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;

import java.io.IOException;
import java.util.List;

public class JwtAuthenticationFilter extends OncePerRequestFilter {
    private final JwtService jwtService;
    private final UserRepository userRepository; // 🌟 1. 注入用户仓库，用于现查真实角色

    // 🌟 2. 构造函数同步更新
    public JwtAuthenticationFilter(JwtService jwtService, UserRepository userRepository) {
        this.jwtService = jwtService;
        this.userRepository = userRepository;
    }

    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest request,
                                    @NonNull HttpServletResponse response,
                                    @NonNull FilterChain filterChain)
            throws ServletException, IOException {

        final String header = request.getHeader("Authorization");
        final String jwt;
        final String username;

        // 💡 1. 检查请求头
        if (header == null || !header.startsWith("Bearer ")) {
            filterChain.doFilter(request, response);
            return;
        }

        jwt = header.substring(7); // 裁剪 "Bearer "
        username = jwtService.extractUsername(jwt);

        // 💡 2. 安全上下文对账
        if (username != null && SecurityContextHolder.getContext().getAuthentication() == null) {

            // 💡 3. 校验令牌合法性
            if (jwtService.validateToken(jwt, username)) {

                // 🌟🌟🌟【重磅核心修正：动态发放门禁卡】🌟🌟🌟
                // 从数据库里现捞这个用户，看看他到底是什么 Role（"ADMIN"、"LANDLORD"、"TENANT"）
                String dbRole = userRepository.findByUsername(username)
                        .map(user -> user.getRole())
                        .orElse("TENANT"); // 兜底：万一查不到，降级为普通租客

                // 🌟 大厂核心规范：hasRole 匹配必须带 "ROLE_" 前缀
                List<SimpleGrantedAuthority> authorities = List.of(
                        new SimpleGrantedAuthority("ROLE_" + dbRole)
                );

                // 💡 4. 构建官方认可的高级工作证，并把真正的特权权限塞进去
                UsernamePasswordAuthenticationToken authToken = new UsernamePasswordAuthenticationToken(
                        username,
                        null,
                        authorities // 🟢 带着真实的权限通关！
                );

                authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));

                // 💡 5. 拍入小账本，绿灯全开
                SecurityContextHolder.getContext().setAuthentication(authToken);
            }
        }

        filterChain.doFilter(request, response);
    }
}