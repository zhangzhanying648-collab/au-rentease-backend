package com.zzy.aurenteasebackend;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;

@SpringBootApplication
@EnableAsync // 核心关键：引爆整个 Spring 容器的异步线程池分配能力！
public class AuRenteaseBackendApplication {

    public static void main(String[] args) {
        SpringApplication.run(AuRenteaseBackendApplication.class, args);
    }

}
