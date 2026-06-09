# ==========================================
# 阶段一：纯干净的云端打包和测试环境（Build Stage）
# ==========================================
FROM maven:3.9.6-eclipse-temurin-21 AS builder
WORKDIR /app

# 避坑：先复制pom.xml下载依赖，利用Docker的缓存机制
COPY pom.xml .
RUN mvn dependency:go-offline -B

# 复制真正的源码
COPY src ./src

# 在云端容器内执行编译打包，同时自动触发执行JUnit单元测试
RUN mvn clean package -DskipTests=false


# ==========================================
# 阶段二：极度精简，绝对安全的生产运行环境（Run stage）
# ==========================================
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app

# 💡 安全审计：大厂禁止用root权限运行java，创建一个名为rentease的普通系统用户
RUN addgroup -S rentease && adduser -S rentease -G rentease

# 🚀 🌟 终极修复：使用大厂白金级 --chown 语法
# 1. 在文件被“偷”过来的瞬间，它的所有者直接就是 rentease！彻底杜绝权限缝隙。
# 2. 用通配符 /app/target/au-rentease-backend-*.jar 模糊匹配，但强行改名为 app.jar 放入当前工作目录。
COPY --chown=rentease:rentease --from=builder /app/target/au-rentease-backend-*.jar ./app.jar

# 💡 额外防御保障：确保整个 /app 目录对于 rentease 用户是完全可读可写的
RUN chown -R rentease:rentease /app

# 🚀 切换到无特权的普通用户，此时它去读刚才属于它自己的 app.jar，畅通无阻！
USER rentease

# 暴漏springboot默认端口8080
EXPOSE 8080

# 生产级安全平滑启动命令（追加 urandom 防止 Linux 熵池不足导致秒挂闪退）
ENTRYPOINT ["java", "-jar", "-Djava.security.egd=file:/dev/./urandom", "app.jar"]