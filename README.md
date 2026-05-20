# Seat Reservation

自习室座位预约系统 —— Spring Boot 后端项目

## 技术栈

Spring Boot 3.5 · MyBatis-Plus 3.5 · MySQL 8.0 · Redis · Spring Security · JWT (jjwt 0.12) · Lombok · JUnit 5

## 功能特性

**学生端**
- 注册/登录（BCrypt 加密）
- 查看自习室列表（Redis 缓存）
- 预约座位（乐观锁防超卖）
- 签到 / 取消预约
- 查看我的预约记录

**管理员端**
- 自习室 CRUD + 时段管理
- 角色权限隔离（`@PreAuthorize`）

**系统特性**
- 超时 30 分钟未签到自动取消（`@Scheduled` 每 5 分钟扫描）
- 并发预约控制（`@Version` 乐观锁 + 原子 SQL 扣库存）
- 全局异常处理（`@RestControllerAdvice`）

## 技术亮点

| 亮点 | 实现 |
|------|------|
| 乐观锁防超卖 | `LambdaUpdateWrapper.setSql("available_capacity = available_capacity - 1")` + `@Version`，CyclicBarrier 10 线程并发验证零超卖 |
| RBAC 角色权限 | JWT claim 携带 role → `SimpleGrantedAuthority("ROLE_" + role.toUpperCase())` → `@PreAuthorize("hasRole('ADMIN')")` |
| Redis 缓存 | `CachingConfigurer` + `JavaTimeModule` 解决 LocalDateTime 序列化，自习室列表 SQL 从 6 次降到 0 |
| 定时回收 | `@Scheduled(fixedRate = 300000)` 扫描超时预约 → 取消 + 恢复库存 |
| 异常处理 | `@RestControllerAdvice` 拦截 AccessDeniedException(403) 和 RuntimeException(400)，防止被 Spring Security 吞 |

## 快速开始

### 环境

- JDK 17
- Maven 3.8+
- MySQL 8.0+
- Redis

### 数据库

```sql
CREATE DATABASE seat_reservation DEFAULT CHARSET utf8mb4;

CREATE TABLE user (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    username VARCHAR(32) NOT NULL UNIQUE,
    password VARCHAR(128) NOT NULL,
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

-- 插入测试数据
INSERT INTO study_room (id, name, total_capacity, available_capacity) VALUES
(1, '自习室A（1号馆）', 50, 50),
(2, '自习室B（2号馆）', 30, 30),
(3, '自习室C（图书馆）', 80, 80);

INSERT INTO time_slot (room_id, start_time, end_time) VALUES
(1, '08:00', '12:00'), (1, '13:00', '17:00'), (1, '18:00', '22:00'),
(2, '08:00', '12:00'), (2, '13:00', '17:00'), (2, '18:00', '22:00'),
(3, '08:00', '12:00'), (3, '13:00', '17:00'), (3, '18:00', '22:00');
```

### 配置

修改 `application.yml` 中的数据库密码和 Redis 地址。

### 启动

```bash
# 确保 MySQL 和 Redis 已启动
mvn spring-boot:run
```

### 注册测试账号

```bash
# 注册
curl -X POST http://localhost:8080/api/auth/register \
  -H "Content-Type: application/json" \
  -d '{"username":"test","password":"123456"}'

# 登录获取 token
curl -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username":"test","password":"123456"}'
```

管理员账号需手动提权：

```sql
UPDATE user SET role = 'admin' WHERE username = 'test';
```

## API 接口

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
| POST | `/api/reservations` | 预约座位 |
| GET | `/api/reservations` | 我的预约列表 |
| GET | `/api/reservations/{id}` | 预约详情 |
| POST | `/api/reservations/{id}/sign` | 签到 |
| POST | `/api/reservations/{id}/cancel` | 取消预约 |

所有接口（除注册/登录外）需在 Header 带 `Authorization: Bearer <token>`。

## 项目结构

```
src/main/java/com/campus/seatreservation/
├── common/          Result、GlobalExceptionHandler
├── config/          SecurityConfig、RedisConfig、MybatisPlusConfig
├── controller/      AuthController、StudyRoomController、ReservationController
├── dto/             请求/响应对象
├── entity/          User、StudyRoom、TimeSlot、Reservation
├── mapper/          MyBatis-Plus BaseMapper
├── security/        JwtAuthenticationFilter
├── service/         业务接口 + impl
├── task/            ReservationTimeoutTask（定时回收）
└── util/            JwtUtils
```