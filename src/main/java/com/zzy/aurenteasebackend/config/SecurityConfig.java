package com.zzy.aurenteasebackend.config;

import com.zzy.aurenteasebackend.repository.UserRepository;
import com.zzy.aurenteasebackend.security.JwtAuthenticationFilter;
import com.zzy.aurenteasebackend.security.JwtService;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import static org.springframework.security.config.Customizer.withDefaults; // 🌟 必须有这个静态导入

@Configuration
@EnableWebSecurity
@EnableMethodSecurity // 极其重要：激活这个注解，@PreAuthorize 权限哨兵才会全面上岗执勤！
public class SecurityConfig {
    private final JwtService jwtService;
    private final UserRepository userRepository; // 💡 注入我们的用户数据库操作层

    public SecurityConfig(JwtService jwtService, UserRepository userRepository) {
        this.jwtService = jwtService;
        this.userRepository = userRepository;
    }

    // 💡 核心：向 Spring Security 郑重宣告我们的用户数据来源！
    //告诉 Spring ：“请把我这个方法返回的对象上报给安全框架。这就是我们全项目独一无二的‘查户口专员’！”
    @Bean
    public UserDetailsService userDetailsService() {
        return username -> userRepository.findByUsername(username)
                //重磅核心！因为 Spring Security 官方是个“洋买办”，它根本不认你自定义的 domain.User。
                // 你必须用官方自带的 User 建造者模式，把你的数据组装成官方认得的 UserDetails 格式。
                .map(user -> org.springframework.security.core.userdetails.User.builder()//
                        .username(user.getUsername())
                        .password(user.getPassword()) // 数据库里的密文密码
                        // 💡 避坑指南：.roles("TENANT") 底层会自动变为 "ROLE_TENANT"
                        .roles(user.getRole())        // 赋予其 TENANT 或 LANDLORD 角色
                        .build())
                .orElseThrow(() -> new UsernameNotFoundException("User not found in RentEase DB: " + username));
    }

    @Bean
    public AuthenticationProvider authenticationProvider() {
        //DaoAuthenticationProvider：这是 Spring Security 官方提供的一辆全自动密码比对坦克（基于数据访问层的认证处理器）。
        DaoAuthenticationProvider authProvider = new DaoAuthenticationProvider();
        authProvider.setUserDetailsService(userDetailsService()); // 绑定抓人逻辑
        authProvider.setPasswordEncoder(passwordEncoder());        // 绑定加密器
        //🛡️ 幕后黑科技（大厂面试高频）：当前端发来明文密码 123456 时，
        // 这个 authProvider 会在幕后自动调用加密器，把明文跟数据库捞出来的密文进行强哈希比对。
        // 如果对不上，它会替你抛出 BadCredentialsException（凭证错误）。
        // 你完全不需要自己写 if(password.equals(...)) 这种业余代码了。
        return authProvider;
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
        //AuthenticationManager：这是整个安全框架的最高行政总裁/总监。
        //运作逻辑：你在 AuthService.login() 里面需要调用一个核心方法：
        // authenticationManager.authenticate(new UsernamePasswordAuthenticationToken(username, password))。
        //
        //大厂内幕：这个经理自己其实不干活，但他手底下管着很多“打工人”（比如上面那个 AuthenticationProvider）。
        // 当你把请求提交给经理时，经理会转手把它甩给下面的坦克去对账。有了这个 @Bean，
        // 你的 AuthService 才能顺利 @Autowired 或构造函数注入它！
        return config.getAuthenticationManager();
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
//                .csrf(AbstractHttpConfigurer::disable)

                // 1. 彻底禁用 CSRF（前后端分离项目的标准做法）
                .csrf(csrf -> csrf.disable())

                // 2. 启用跨域资源共享（CORS），配合你的 CorsConfigurationSource 豆子生效
//                .cors(cors -> cors.withDefaults())
                .cors(withDefaults())
                .exceptionHandling(exception -> exception
                        // 🚀 动态认证入口：根据具体的 authException 吐回真实的错误提示
                        .authenticationEntryPoint((request, response, authException) -> {
                            // 1. 锁死状态码 401
                            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                            response.setContentType("application/json;charset=UTF-8");

                            // 2. 🌟 智能判定：根据异常类型动态组织语言
                            String errorMessage = "Full authentication is required to access this resource.";

                            if (authException != null) {
                                // 如果是账户被锁、凭证错误等导致的（比如登录接口真的输错账密）
                                errorMessage = authException.getMessage();
                            }

                            // 如果你在 JwtAuthenticationFilter 里把错误存到了 request 域中，这里也能拿到：
                            if (request.getAttribute("jwt_error") != null) {
                                errorMessage = request.getAttribute("jwt_error").toString();
                            }

                            // 3. 严格对齐前端要求的 JSON 结构
                            String jsonPayload = String.format("{\"message\": \"%s\"}", errorMessage);

                            response.getWriter().write(jsonPayload);
                            response.getWriter().flush();
                        })
                )

                .authorizeHttpRequests(auth->auth
                        //精准放行 /ws/notifications 及其所有子路由，严防 Security 误伤
                        .requestMatchers("/ws/notifications", "/ws/notifications/**").permitAll()
                        .requestMatchers("/api/properties/search").permitAll()
                        .requestMatchers("/api/files/**").permitAll()
                        .requestMatchers("/api/auth/**").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/properties/**").permitAll()
                        .anyRequest().authenticated()
                )

                .sessionManagement(session->{
                    session.sessionCreationPolicy(SessionCreationPolicy.STATELESS);
                })
                .addFilterBefore(new JwtAuthenticationFilter(jwtService, userRepository),
                        UsernamePasswordAuthenticationFilter.class)


;
        return http.build();
    }

    // 💡 注入大厂标配的密码加密器
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
