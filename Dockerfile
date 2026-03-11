# 构建阶段
FROM maven:3.9.6-eclipse-temurin-21-alpine AS builder

WORKDIR /app

# 配置 Maven 阿里云镜像（加速依赖下载）
COPY settings.xml /root/.m2/settings.xml

# 复制 pom.xml 先下载依赖（利用 Docker 缓存层）
COPY pom.xml .
RUN mvn dependency:go-offline -B

# 复制源代码
COPY src ./src

# 构建应用
RUN mvn clean package -DskipTests -B

# 运行阶段
FROM eclipse-temurin:21-jre-alpine

WORKDIR /app

# 安装必要的工具（curl 用于健康检查）
RUN apk add --no-cache curl

# 复制构建好的 JAR
COPY --from=builder /app/target/*.jar app.jar

# 健康检查
HEALTHCHECK --interval=30s --timeout=3s --start-period=60s --retries=3 \
  CMD curl -f http://localhost:8080/actuator/health || exit 1

EXPOSE 7001

# JVM 优化参数
ENV JAVA_OPTS="-XX:+UseContainerSupport -XX:MaxRAMPercentage=75.0 -XX:InitialRAMPercentage=50.0"

ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]
