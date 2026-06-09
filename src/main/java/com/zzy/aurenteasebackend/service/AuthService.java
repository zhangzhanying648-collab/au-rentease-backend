package com.zzy.aurenteasebackend.service;

import com.zzy.aurenteasebackend.domain.User;
import com.zzy.aurenteasebackend.repository.UserRepository;
import com.zzy.aurenteasebackend.security.JwtService;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AuthService {
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final AuthenticationManager authenticationManager;

//    public AuthService(UserRepository userRepository, PasswordEncoder passwordEncoder, JwtService jwtService, AuthenticationManager authenticationManager) {
//        this.userRepository = userRepository;
//        this.passwordEncoder = passwordEncoder;
//        this.jwtService = jwtService;
//        this.authenticationManager = authenticationManager;
//    }


    //用户注册
    public String register(User user) {
        if(userRepository.findByUsername(user.getUsername()).isPresent()){
            throw new IllegalArgumentException("Username is already in use");
        }

        // 💡 核心步骤：将用户的明文密码经过 BCrypt 强力揉碎加密
        user.setPassword(passwordEncoder.encode(user.getPassword()));
        userRepository.save(user);
        return "User registered successfully";
    }

    //用户登录
    //轻量做法：自己去验证密码匹配。
//    public String login(String username, String password){
//        //userRepository.findByUsername返回的是Optional, orElseThrows是Optional的方法，如果返回值为空，则执行
//        //orElseThrow后面的抛出异常
//        User user=userRepository.findByUsername(username)
//                .orElseThrow(()->new UsernameNotFoundException("Username not found"));
//
//        if(!passwordEncoder.matches(password,user.getPassword())){
//            throw new IllegalArgumentException("Password does not match");
//        }
//
//        return jwtService.generateToken(username);
//    }

    //用户登录
    //正统的大厂全托管写法
    public LoginResult login(String username, String password){

        // 💡 让大经理去调 Provider，Provider 再去调 UserDetailsService 和 PasswordEncoder 进行肉搏比对
        // 如果密码错或者用户不存在，这一行会自动抛出 BadCredentialsException 异常
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(username, password)
        );
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new org.springframework.security.core.userdetails.UsernameNotFoundException("User not found after auth"));

        String token = jwtService.generateToken(username);
        String role = user.getRole(); // 或者 user.getRole().name() 如果你的 role 是枚类型

        // 返回包装对象
        return new LoginResult(token, role);
    }

    // @Value 会自动生成只读的全参构造、getter 方法
    @lombok.Value
    public static class LoginResult {
        String token;
        String role;
    }
}
