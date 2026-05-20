


Based on the code map provided, I can see this is a campus seat reservation system built with Spring Boot. Let me generate the README based on this information.

# Seat Reservation

图书馆座位预约系统

## 项目简介

Seat Reservation 是一个基于 Spring Boot 3.x 开发的图书馆座位预约系统，采用 MyBatis-Plus 作为持久层框架，Redis 作为缓存，JWT 进行身份认证。该系统支持用户注册登录、座位预约、签到、取消预约等功能，并提供管理员界面进行自习室管理。

## 技术栈

- **后端框架**: Spring Boot 3.x
- **持久层**: MyBatis-Plus
- **数据库**: MySQL
- **缓存**: Redis
- **安全**: Spring Security + JWT
- **其他**: Lombok, Validation

## 功能特性

### 用户功能
- 用户注册与登录
- 座位预约
- 预约签到
- 取消预约
- 查看我的预约记录

### 管理员功能
- 自习室增删改查
- 时间段管理

### 系统特性
- 预约超时自动取消（每5分钟检查）
- 缓存支持
- 并发预约控制

## 快速开始

### 环境要求

- JDK 17+
- Maven 3.8+
- MySQL 8.0+
- Redis 6.0+

### 配置

在 `application.yml` 中配置数据库和 Redis 连接信息：

```yaml
spring:
  datasource:
    url: jdbc:mysql://localhost:3306/seat_reservation
    username: your_username
    password: your_password
  data:
    redis:
      host: localhost
      port: 6379

jwt:
  secret: your-secret-key
  expiration: 86400000
```

### 构建运行

```bash
mvn clean install
java -jar target/seat-reservation-0.0.1-SNAPSHOT.jar
```

## API 接口

### 认证接口

| 方法 | 路径 | 描述 |
|------|------|------|
| POST | /api/auth/register | 用户注册 |
| POST | /api/auth/login | 用户登录 |

### 预约接口

| 方法 | 路径 | 描述 |
|------|------|------|
| POST | /api/reservations | 创建预约 |
| GET | /api/reservations | 我的预约列表 |
| GET | /api/reservations/{id} | 预约详情 |
| POST | /api/reservations/{id}/sign | 签到 |
| POST | /api/reservations/{id}/cancel | 取消预约 |

### 自习室接口

| 方法 | 路径 | 描述 |
|------|------|------|
| GET | /api/rooms | 自习室列表 |
| GET | /api/rooms/{id} | 自习室详情 |
| POST | /api/rooms | 创建自习室（管理员） |
| PUT | /api/rooms/{id} | 更新自习室（管理员） |
| DELETE | /api/rooms/{id} | 删除自习室（管理员） |

## 数据库表

- `user` - 用户表
- `study_room` - 自习室表
- `time_slot` - 时间段表
- `reservation` - 预约表

## 许可证

MIT License