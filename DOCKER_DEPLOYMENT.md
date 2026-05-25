# Docker 部署指南

## 前置条件

确保已安装以下软件：
- Docker Desktop（Windows/Mac）或 Docker Engine（Linux）
- Docker Compose（通常随 Docker Desktop 一起安装）

## 快速开始

### 1. 构建并启动所有服务

```bash
# 在项目根目录执行
mvn clean package -DskipTests
docker-compose up -d
```

### 2. 查看服务状态

```bash
# 查看所有容器状态
docker-compose ps

# 查看日志
docker-compose logs -f app
```

### 3. 访问服务

- **应用服务**: http://localhost:8080
- **RabbitMQ 管理界面**: http://localhost:15672 (guest/guest)
- **MySQL**: localhost:3306 (root/622824)
- **Redis**: localhost:6379

### 4. 停止服务

```bash
# 停止所有服务
docker-compose down

# 停止并删除数据卷（谨慎使用！）
docker-compose down -v
```

## 常用命令

### 容器管理

```bash
# 重启某个服务
docker-compose restart app

# 查看实时日志
docker-compose logs -f [service_name]

# 进入容器内部
docker-compose exec app bash
docker-compose exec mysql mysql -uroot -p622824
docker-compose exec redis redis-cli
```

### 镜像管理

```bash
# 重新构建镜像
docker-compose build --no-cache

# 只构建应用镜像
docker-compose build app

# 查看镜像列表
docker images | grep seat-reservation
```

### 数据备份

```bash
# 备份 MySQL 数据
docker-compose exec mysql mysqldump -uroot -p622824 seat_reservation > backup.sql

# 恢复 MySQL 数据
docker-compose exec -T mysql mysql -uroot -p622824 seat_reservation < backup.sql
```

## 配置说明

### 环境变量

在 `docker-compose.yml` 中可以修改以下配置：

```yaml
environment:
  # 数据库配置
  SPRING_DATASOURCE_URL: jdbc:mysql://mysql:3306/seat_reservation?...
  SPRING_DATASOURCE_USERNAME: root
  SPRING_DATASOURCE_PASSWORD: 622824
  
  # Redis 配置
  SPRING_DATA_REDIS_HOST: redis
  SPRING_DATA_REDIS_PORT: 6379
  
  # RabbitMQ 配置
  SPRING_RABBITMQ_HOST: rabbitmq
  SPRING_RABBITMQ_PORT: 5672
  SPRING_RABBITMQ_USERNAME: guest
  SPRING_RABBITMQ_PASSWORD: guest
```

### 端口映射

| 服务 | 容器端口 | 宿主机端口 | 说明 |
|------|---------|-----------|------|
| App | 8080 | 8080 | Spring Boot 应用 |
| MySQL | 3306 | 3306 | 数据库 |
| Redis | 6379 | 6379 | 缓存 |
| RabbitMQ | 5672 | 5672 | AMQP 协议 |
| RabbitMQ | 15672 | 15672 | 管理界面 |

### 数据持久化

使用 Docker Volume 实现数据持久化：

- `mysql_data`: MySQL 数据文件
- `redis_data`: Redis 持久化数据
- `rabbitmq_data`: RabbitMQ 队列数据
- `app_logs`: 应用日志

## 故障排查

### 1. 应用启动失败

```bash
# 查看应用日志
docker-compose logs app

# 检查依赖服务是否健康
docker-compose ps

# 检查网络连接
docker-compose exec app ping mysql
docker-compose exec app ping redis
docker-compose exec app ping rabbitmq
```

### 2. 数据库连接失败

```bash
# 检查 MySQL 是否启动
docker-compose ps mysql

# 查看 MySQL 日志
docker-compose logs mysql

# 手动连接测试
docker-compose exec mysql mysql -uroot -p622824
```

### 3. Redis 连接失败

```bash
# 检查 Redis 状态
docker-compose ps redis

# 测试 Redis 连接
docker-compose exec redis redis-cli ping
```

### 4. RabbitMQ 连接失败

```bash
# 检查 RabbitMQ 状态
docker-compose ps rabbitmq

# 查看 RabbitMQ 日志
docker-compose logs rabbitmq
```

### 5. 端口冲突

如果端口被占用，修改 `docker-compose.yml` 中的端口映射：

```yaml
ports:
  - "8081:8080"  # 将宿主机的 8081 映射到容器的 8080
```

## 生产环境建议

### 1. 安全配置

- 修改默认密码
- 使用 Docker Secret 管理敏感信息
- 限制容器资源使用

```yaml
services:
  app:
    deploy:
      resources:
        limits:
          cpus: '2'
          memory: 1G
        reservations:
          cpus: '0.5'
          memory: 512M
```

### 2. 日志管理

```yaml
services:
  app:
    logging:
      driver: "json-file"
      options:
        max-size: "10m"
        max-file: "3"
```

### 3. 健康检查

已在 `docker-compose.yml` 中配置健康检查，确保服务依赖关系正确。

### 4. 自动重启

```yaml
services:
  app:
    restart: always  # 或 unless-stopped
```

## 性能优化

### JVM 参数调优

在 `Dockerfile` 中调整：

```dockerfile
ENV JAVA_OPTS="-Xms512m -Xmx1g -XX:+UseG1GC -XX:MaxGCPauseMillis=200"
```

### MySQL 优化

在 `docker-compose.yml` 中添加：

```yaml
command:
  - --innodb-buffer-pool-size=256M
  - --max-connections=200
```

## 监控和运维

### 查看资源使用情况

```bash
# 查看容器资源使用
docker stats

# 查看特定容器
docker stats seat-reservation-app
```

### 清理资源

```bash
# 清理未使用的镜像
docker image prune -a

# 清理未使用的卷
docker volume prune

# 清理所有未使用的资源
docker system prune -a --volumes
```

## 常见问题

### Q: 如何更新应用版本？

```bash
# 1. 重新打包
mvn clean package -DskipTests

# 2. 重新构建并启动
docker-compose up -d --build app
```

### Q: 如何查看数据库初始化是否成功？

```bash
# 进入 MySQL 容器
docker-compose exec mysql mysql -uroot -p622824

# 查询数据
USE seat_reservation;
SHOW TABLES;
SELECT COUNT(*) FROM user;
SELECT COUNT(*) FROM study_room;
```

### Q: 如何重置整个环境？

```bash
# 警告：这将删除所有数据！
docker-compose down -v
docker-compose up -d
```

## 开发模式

如果需要热重载，可以挂载本地目录：

```yaml
services:
  app:
    volumes:
      - ./target/app.jar:/app/app.jar
```

然后使用 Spring DevTools 实现热更新。

## 更多信息

- [Docker 官方文档](https://docs.docker.com/)
- [Docker Compose 文档](https://docs.docker.com/compose/)
- [Spring Boot Docker 指南](https://spring.io/guides/topicals/spring-boot-docker/)
