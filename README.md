# Seat Reservation

自习室座位预约系统 —— Spring Boot 后端项目

## 技术栈

Spring Boot 3.5 · MyBatis-Plus 3.5 · MySQL 8.0 · Redis · Redisson · RabbitMQ · Spring Security · JWT · Docker · Lombok · JUnit 5

## 功能特性

**学生端**
- 注册/登录（BCrypt 加密）
- 查看自习室列表（Redis 缓存）
- 预约座位（分布式锁 + 乐观锁双层防护）
- 预约成功异步短信通知（RabbitMQ）
- 签到 / 取消预约
- 查看我的预约记录

**管理员端**
- 自习室 CRUD + 时段管理
- 角色权限隔离（`@PreAuthorize`）

**系统特性**
- 超时 30 分钟未签到自动取消（`@Scheduled` 每 5 分钟扫描）
- 双层并发控制（Redisson 分布式锁 + `@Version` 乐观锁）
- RabbitMQ 异步解耦，预约接口不受通知模块影响
- 全局异常处理（`@RestControllerAdvice`）
- Docker Compose 一键部署（MySQL + Redis + RabbitMQ + 应用）

## 技术亮点

| 亮点 | 实现 |
|------|------|
| 分布式锁 + 乐观锁 | Redisson 前置加锁 + `setSql` 原子扣库存，双层防护防超卖 |
| RBAC 角色权限 | JWT claim 携带 role → `hasRole('ADMIN')` 保护管理接口 |
| RabbitMQ 异步通知 | 预约成功扔消息到队列即返回，消费者异步模拟短信发送 |
| Redis 缓存 | `CachingConfigurer` + `JavaTimeModule` 解决 LocalDateTime 序列化 |
| 定时回收 | `@Scheduled` 每5分钟扫描超时预约 → 自动取消并恢复库存 |
| Docker 部署 | `docker compose up -d` 一键启动全部环境 |

## 快速开始

### 方式一：Docker Compose（推荐）

```bash
docker compose up -d
```

自动启动 MySQL、Redis、RabbitMQ、应用，无需手动装环境。

### 方式二：本地运行

**环境**

- JDK 17
- Maven 3.8+
- MySQL 8.0+
- Redis
- RabbitMQ（可选，未装时跳过短信通知）

**数据库**

执行 `sql/init.sql` 或手动建表：

```sql
CREATE DATABASE seat_reservation DEFAULT CHARSET utf8mb4;

CREATE TABLE user (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    username VARCHAR(32) NOT NULL UNIQUE,
    password VARCHAR(128) NOT NULL,
    phone VARCHAR(16) DEFAULT '',
    role VARCHAR(16) NOT NULL DEFAULT 'student',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE study_room (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(64) NOT NULL,
    total_capacity INT NOT NULL,
    available_capacity INT NOT NULL,
    version INT DEFAULT 0,
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE time_slot (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    room_id BIGINT NOT NULL,
    start_time TIME NOT NULL,
    end_time TIME NOT NULL,
    FOREIGN KEY (room_id) REFERENCES study_room(id)
);

CREATE TABLE reservation (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    room_id BIGINT NOT NULL,
    time_slot_id BIGINT NOT NULL,
    reservation_date DATE NOT NULL,
    status VARCHAR(16) DEFAULT 'booked',
    sign_time DATETIME,
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (user_id) REFERENCES user(id),
    FOREIGN KEY (room_id) REFERENCES study_room(id),
    FOREIGN KEY (time_slot_id) REFERENCES time_slot(id),
    UNIQUE KEY uk_user_room_slot_date (user_id, room_id, time_slot_id, reservation_date)
);

-- 测试数据
INSERT INTO study_room (id, name, total_capacity, available_capacity) VALUES
(1, '自习室A（1号馆）', 50, 50),
(2, '自习室B（2号馆）', 30, 30),
(3, '自习室C（图书馆）', 80, 80);

INSERT INTO time_slot (room_id, start_time, end_time) VALUES
(1, '08:00', '12:00'), (1, '13:00', '17:00'), (1, '18:00', '22:00'),
(2, '08:00', '12:00'), (2, '13:00', '17:00'), (2, '18:00', '22:00'),
(3, '08:00', '12:00'), (3, '13:00', '17:00'), (3, '18:00', '22:00');
```

**启动**

```bash
mvn spring-boot:run
```

**测试**

```bash
# 注册
curl -X POST http://localhost:8080/api/auth/register \
  -H "Content-Type: application/json" \
  -d '{"username":"test","password":"123456"}'

# 登录获取 token
curl -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username":"test","password":"123456"}'

# 提权为管理员
# UPDATE user SET role = 'admin' WHERE username = 'test';
```

## API 接口

所有接口（除注册/登录外）需在 Header 带 `Authorization: Bearer <token>`。

### 认证

| 方法 | 路径 | 说明 |
|------|------|------|
| POST | `/api/auth/register` | 注册（默认 student） |
| POST | `/api/auth/login` | 登录，返回 JWT |

### 自习室

| 方法 | 路径 | 权限 | 说明 |
|------|------|------|------|
| GET | `/api/rooms` | 登录即可 | 列表（Redis 缓存） |
| GET | `/api/rooms/{id}` | 登录即可 | 详情 |
| POST | `/api/rooms` | ADMIN | 新增 |
| PUT | `/api/rooms/{id}` | ADMIN | 修改 |
| DELETE | `/api/rooms/{id}` | ADMIN | 删除 |

### 预约

| 方法 | 路径 | 说明 |
|------|------|------|
| POST | `/api/reservations` | 预约座位（分布式锁 + 乐观锁） |
| GET | `/api/reservations` | 我的预约列表 |
| GET | `/api/reservations/{id}` | 预约详情 |
| POST | `/api/reservations/{id}/sign` | 签到 |
| POST | `/api/reservations/{id}/cancel` | 取消预约 |

## 项目结构

```
├── Dockerfile                    应用镜像
├── docker-compose.yml            容器编排
├── sql/init.sql                  数据库初始化脚本
└── src/main/java/com/campus/seatreservation/
    ├── common/                   Result、GlobalExceptionHandler
    ├── config/                   SecurityConfig、RedisConfig、MybatisPlusConfig、
    │                             RabbitMQConfig、RedissonConfig
    ├── controller/               AuthController、StudyRoomController、
    │                             ReservationController
    ├── dto/                      请求/响应对象、SmsMessage
    ├── entity/                   User、StudyRoom、TimeSlot、Reservation
    ├── mapper/                   MyBatis-Plus BaseMapper
    ├── security/                 JwtAuthenticationFilter
    ├── service/                  业务接口 + impl（含 SmsConsumer）
    ├── task/                     ReservationTimeoutTask
    └── util/                     JwtUtils
```

## 数据库表

| 表名 | 说明 | 关键字段 |
|------|------|----------|
| `user` | 用户表 | username, password(BCrypt), role(student/admin), phone |
| `study_room` | 自习室表 | name, total_capacity, available_capacity, version(乐观锁) |
| `time_slot` | 时段表 | room_id, start_time, end_time |
| `reservation` | 预约表 | user_id, room_id, status(booked/signed/cancelled), 唯一索引防重复 |
