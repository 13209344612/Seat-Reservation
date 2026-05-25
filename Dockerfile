# 使用 OpenJDK 17 作为基础镜像
FROM openjdk:17-jdk-slim

# 设置维护者信息
LABEL maintainer="SeatReservation Team"

# 设置工作目录
WORKDIR /app

# 复制 Maven 构建的 JAR 文件到容器中
COPY target/*.jar app.jar

# 暴露应用端口
EXPOSE 8080

# JVM 参数优化（可根据实际情况调整）
ENV JAVA_OPTS="-Xms256m -Xmx512m -XX:+UseG1GC -XX:MaxGCPauseMillis=200"

# 启动应用
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar /app.jar"]
