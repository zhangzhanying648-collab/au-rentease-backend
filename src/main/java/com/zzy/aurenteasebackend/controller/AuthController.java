package com.zzy.aurenteasebackend.controller;

import com.zzy.aurenteasebackend.domain.User;
import com.zzy.aurenteasebackend.service.AuthService;
import lombok.Data;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
public class AuthController {
    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/register")
    public ResponseEntity<String> register(@RequestBody User user){
        return  ResponseEntity.ok(authService.register(user));
    }

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@RequestBody LoginRequest request)
    {
        AuthService.LoginResult result =authService.login(request.getUsername(),request.getPassword());
        return ResponseEntity.ok(new AuthResponse(result.getToken(), result.getRole()));
    }

    //在 Controller 内部定义static class（静态内部类）
    //在写 Spring Boot 项目时，这是一个非常经典的架构设计与代码洁癖的问题。
    //为了高内聚性（Cohesion）和代码可读性。避免项目被无数个“只用一次”的微小 DTO 文件塞满。
    /*必须加上 static，否则 Spring Boot 的 Jackson 序列化组件会直接罢工报错。
在 Java 中，不带 static 的普通内部类（成员内部类）有一个致命特性：它必须依附于外部类的实例才能存在。
也就是说，想要 new LoginRequest()，必须先 new AuthController()。
如果不加 static：当一串 JSON 请求过来时，Spring 试图把 JSON 转成 LoginRequest 对象，
但 Jackson 根本不知道怎么去凭空实例化它（因为它拿不到 AuthController 的实例），程序直接抛出反序列化异常。*/
    @Data
    static class AuthResponse{
        private final String token;
        private final String tokenType="Bearer";// 💡 澳洲团队非常喜欢显式指定 Token 类型
        private final String role;
    }

    @Data
    static class LoginRequest{
        private String username;
        private String password;
    }

}
