#阶段一.纯净的云端打包和测试环境（Build Stage)
FROM maven:3.9.6-eclipse-temurin-21 AS builder
WORKDIR /app

#避坑：先复制pom.xml下载依赖，利用Docker的缓存机制
# 这样只要pom.xml没有改，下次编译时就不用重复下载全网的jar包，速度飞起。
COPY pom.xml .
RUN mvn dependency:go-offline -B

#复制真正的源码
COPY src ./src

#在云端容器内执行编译打包，同时自动出发执行Junit单元测试
#如果测试未通过，Docker构建会自动崩溃熔断，坚决不让带病代码上线
RUN mvn clean package -DskipTests=false

#阶段二. 极度精简，绝对安全的生产运行环境（Run stage）
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app

#安全审计的死穴：大厂禁止用root权限运行java，我们创建一个名为rentease的普通系统用户
RUN addgroup -S rentease && adduser -S rentease -G rentease

#把编译好的jar包偷过来
COPY --from=builder /app/target/*.jar app.jar

#当前工作目录的所有权彻底移交给非root用户
RUN chown -R rentease:rentease /app
USER rentease

#暴漏springboot默认端口8080
EXPOSE 8080

#生产级安全平滑启动命令
#ENTRYPOINT ["java", "-jar", "-Djava.security.egd=file:/dev/./urandom", "app.jar"]
ENTRYPOINT ["java", "-jar", "app.jar"]