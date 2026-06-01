package com.zzy.aurenteasebackend.security;

import com.zzy.aurenteasebackend.domain.UserRole;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Collections;
import java.util.List;

//extends OncePerRequestFilter：继承自 Spring Web 的一个经典基类。
//顾名思义，它能确保每一个 HTTP 请求在全生命周期里，只会经过这个过滤器“严格审查一次”，防止重复审查浪费性能。
public class JwtAuthenticationFilter extends OncePerRequestFilter {
    private JwtService jwtService;

    //通过构造函数把你刚才写好的 JwtService 注入进来，因为一会需要借用它的“钥匙”去解密和验证 Token。
    public JwtAuthenticationFilter(JwtService jwtService){
        this.jwtService = jwtService;
    }


    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest request,
                                    @NonNull HttpServletResponse response,
                                    @NonNull FilterChain filterChain)
            throws ServletException, IOException {
        //请求进来后，哨兵先去瞅一眼请求头（Header）里有没有一个叫 Authorization（授权）的包裹。
        final String header = request.getHeader("Authorization");
        final String jwt;
        final String username;

        // 💡 1. 检查请求头中是否包含 Bearer 令牌，没有则直接放行（交给后续的 Spring Security 拦截器处理）
        //如果根本没有这个头，或者有这个头但是它开头不是以 Bearer （持有者令牌的标准前缀，注意带个空格）开头的，
        // 说明这个请求要么是去“浏览房源”这种不需要登录的公开页面，要么就是没带通行证。
        if (header == null || !header.startsWith("Bearer ")) {
            //哨兵说：“行吧，我这里查不出什么，放你走，让后面的过滤器（比如 Spring Security 的匿名过滤器）来决定怎么处置你。”
            filterChain.doFilter(request, response);
            return;
        }

        //如果代码越过了上面的 if，说明请求带了通行证：
        //拆箱取单据（裁剪 Token）
        jwt= header.substring(7);//// 裁剪掉 "Bearer "
        username=jwtService.extractUsername(jwt);

        //双重确认（SecurityContext 状态对账）
        // 💡 2. 如果成功提取出用户名，且当前安全上下文（SecurityContext）中还没有登录记录
        //SecurityContextHolder 是 Spring Security 的全链路内存小账本，用来记录当前请求“到底是谁登录了”。
        //这里的判断是：如果小账本里目前还是 null（说明之前的过滤器还没判定他登录过）。
        //这样判断能防止重复对账，提高效率。
        if (username != null && SecurityContextHolder.getContext().getAuthentication() == null) {

            // 💡 3. 校验令牌合法性。如果通过，手动构建 Spring Security 能够识别的身份证明（Authentication）
            if(jwtService.validateToken(jwt,username)){
                // 💡 真正的大厂闭环：这里应该从你的 jwtService 里面去捞用户的真正 Role
                // 或者是为了快速通关，我们先给通过 JWT 校验的人发放一个 "ROLE_TENANT" 的大门门禁卡
                List<SimpleGrantedAuthority> authorities = List.of(new SimpleGrantedAuthority("ROLE_"+ UserRole.TENANT.name()));

                //只要 Token 合法，我们要给它换发一张 Spring Security 官方认可的“内部核心高级工作证”（即 Authentication 对象）。
                //参数一 username：告诉系统他是谁。
                //参数二 null：密码（既然 Token 已经合法，就不需要密码了，填 null）。
                //参数三 Collections.emptyList()：他的权限/角色列表。你目前传入了一个空列表，意味着他暂时是个普通登录用户，没有特殊角色（比如 ROLE_ADMIN）。
                UsernamePasswordAuthenticationToken authToken = new UsernamePasswordAuthenticationToken(
                        username,
                        null,
//                        Collections.emptyList()
                        authorities
                );

                //setAuthentication()：把刚刚做好的高级工作证，往 Spring Security 的全局小账本里一拍！
                //这一步完成之后，后续所有的 Controller 接口和 Spring 安全拦截器再来看账本时，就会惊呼：“哇！小账本里有他的身份证明，他是合法登录用户！” 从而绿灯全开。
                // 💡 4. 将这份合法的身份证明，牢牢锁进 Spring 的全局安全上下文中
                SecurityContextHolder.getContext().setAuthentication(authToken);
            }
        }

        //最后的 filterChain.doFilter(...)：至此，JWT 审查圆满结束，顺理成章地把请求送入后续的业务处理管道。
        filterChain.doFilter(request, response);
    }
}
