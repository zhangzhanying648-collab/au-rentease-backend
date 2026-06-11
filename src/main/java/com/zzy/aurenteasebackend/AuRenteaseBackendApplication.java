package com.zzy.aurenteasebackend;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableAsync // 核心关键：引爆整个 Spring 容器的异步线程池分配能力！
@EnableScheduling // 🚀 核心大闸：开启 Spring 内部的定时任务调度引擎
public class AuRenteaseBackendApplication {

    public static void main(String[] args) {
        SpringApplication.run(AuRenteaseBackendApplication.class, args);
    }

}
