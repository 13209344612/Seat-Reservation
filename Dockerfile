# ---- 构建阶段：Maven 在容器内打包，服务器无需安装 JDK/Maven ----
FROM maven:3.9-eclipse-temurin-17 AS build
WORKDIR /build

# 先拷贝 pom.xml 预下载依赖（best-effort，利用层缓存；失败不阻断构建）
COPY pom.xml .
RUN mvn -B dependency:go-offline || true

# 拷贝源码并打包（跳过测试）
COPY src ./src
RUN mvn -B clean package -DskipTests

# ---- 运行阶段：仅 JRE，镜像更小 ----
FROM eclipse-temurin:17-jre-alpine

LABEL maintainer="SeatReservation Team"
WORKDIR /app

# 从构建阶段拷贝打好的 JAR
COPY --from=build /build/target/*.jar app.jar

EXPOSE 8080

# JVM 参数优化（可根据服务器内存调整）
ENV JAVA_OPTS="-Xms256m -Xmx512m -XX:+UseG1GC -XX:MaxGCPauseMillis=200 -Dspring.profiles.active=docker"

ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar /app/app.jar"]
