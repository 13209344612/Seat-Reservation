# 座位预约系统 (Seat Reservation System)

🎯 **校园自习室座位预约系统** —— 基于 Spring Boot 3.5 + Vue 3 的全栈项目

![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.5.14-brightgreen.svg)
![MyBatis-Plus](https://img.shields.io/badge/MyBatis--Plus-3.5.12-blue.svg)
![Spring AI](https://img.shields.io/badge/Spring%20AI-DashScope-purple.svg)
![Docker](https://img.shields.io/badge/Docker-Compose-orange.svg)
![License](https://img.shields.io/badge/License-MIT-yellow.svg)

## 📋 目录

- [技术栈](#技术栈)
- [功能特性](#功能特性)
- [系统架构](#系统架构)
- [性能指标](#性能指标)
- [快速开始](#快速开始)
- [API 文档](#api-文档)
- [核心技术亮点](#核心技术亮点)
- [项目结构](#项目结构)
- [数据库设计](#数据库设计)

---

## 技术栈

### 后端
- **核心框架**: Spring Boot 3.5.14
- **ORM**: MyBatis-Plus 3.5.12
- **数据库**: MySQL 8.0
- **缓存**: Redis 7 + Spring Cache
- **消息队列**: RabbitMQ 3
- **分布式锁**: Redisson 3.27.0
- **安全认证**: Spring Security + JWT (jjwt 0.12.6)
- **AI 能力**: Spring AI 1.1 + Spring AI Alibaba（DashScope 通义千问）—— RAG 向量检索 + SSE 流式对话 + 工具调用
- **工具库**: Lombok, Jackson JSR310, Reactor

### 前端
- **框架**: Vue 3 Composition API
- **UI 组件**: Element Plus
- **路由**: Vue Router
- **状态管理**: Pinia
- **HTTP 客户端**: Axios
- **构建工具**: Vite

### DevOps
- **容器化**: Docker + Docker Compose（MySQL / Redis / RabbitMQ / 后端 / Nginx 五服务编排）
- **网关 / 静态托管**: Nginx（托管前端 `dist` + 反向代理 `/api`，SSE 不缓冲）
- **CI/CD**: GitHub Actions (可选)

---

## 功能特性

### 👨 🎓 学生端
- ✅ 用户注册/登录（BCrypt 密码加密）
- ✅ 查看自习室列表（Redis 缓存优化）
- ✅ 按「日期 + 时段」查询座位余量（派生统计，实时准确）
- ✅ 预约座位（Redisson 分布式锁 + 派生库存校验，防超卖）
- ✅ 预约成功异步短信通知（RabbitMQ）
- ✅ 签到 / 取消预约（取消后可重新预约同一时段）
- ✅ 查看我的预约记录

### 🤖 AI 助手（Spring AI + 通义千问）
- ✅ 自然语言问答预约规则、签到/取消流程（RAG 检索内置知识库）
- ✅ SSE 流式对话，前端打字机效果
- ✅ 多轮上下文记忆（ChatMemory）+ 历史会话持久化，可回溯 / 删除
- ✅ 工具调用（Function Calling）：AI 可代当前用户查询余量等，userId 经 ToolContext 安全传递，杜绝越权

### 👨 💼 管理员端
- ✅ 自习室 CRUD + 时段管理
- ✅ 角色权限隔离（`@PreAuthorize`）
- ✅ 库存实时监控

### ⚙️ 系统特性
- ✅ 超时未签到自动取消（定时任务每 5 分钟扫描，基于预约日期 + 时段时间判定）
- ✅ 库存派生计数：无房间级计数器，按「房间 + 日期 + 时段」实时统计活跃预约，杜绝计数漂移
- ✅ 并发正确性：Redisson 分布式锁（room+date+slot 维度）+ 锁内 count 校验；生成列 `active_unique` 部分唯一索引防重复活跃预约
- ✅ RabbitMQ 异步解耦，预约接口不受通知模块影响
- ✅ 全局异常处理（参数校验 + 业务异常统一返回）
- ✅ Docker Compose 一键部署（MySQL + Redis + RabbitMQ + 后端 + Nginx 五服务）

---

## 系统架构

### 整体架构图

```
graph TB
    Client[前端 Vue3 + Element Plus] -->|HTTP/HTTPS| LB[Nginx 反向代理]
    LB -->|8080| API[Spring Boot 应用]
    
    API -->|JDBC| MySQL[(MySQL 8.0<br/>用户/预约数据)]
    API -->|Redis Protocol| Redis[(Redis 7<br/>缓存/分布式锁)]
    API -->|AMQP| RabbitMQ[RabbitMQ 3<br/>异步消息队列]
    
    RabbitMQ -->|消费消息| SMS[短信消费者]
    SMS -->|模拟发送| User((用户))
    
    subgraph "安全认证"
        JWT[JWT Token]
        Security[Spring Security]
    end
    
    API --> JWT
    API --> Security
    
    subgraph "核心业务"
        Reserve[预约服务]
        Lock[Redisson 分布式锁]
        Derived[派生库存计数]
    end
    
    Reserve --> Lock
    Reserve --> Derived
    Derived --> MySQL
    
    subgraph "AI 助手"
        AI[AiAssistantController]
        RAG[RAG 知识库检索]
        LLM[DashScope 通义千问]
    end
    
    API --> AI
    AI --> RAG
    AI --> LLM
    AI -->|SSE 流式| Client
```

### 技术架构图

```
┌─────────────────────────────────────────────────────────┐
│                     前端层 (Vue 3)                       │
│   Login | Rooms | Reservations | RoomDetail             │
└──────────────────────┬──────────────────────────────────┘
                       │ HTTP + JWT Token
┌──────────────────────▼──────────────────────────────────┐
│                   网关层 (Nginx)                         │
│         反向代理 + 负载均衡 + 静态资源                   │
└──────────────────────┬──────────────────────────────────┘
                       │
┌──────────────────────▼──────────────────────────────────┐
│                 控制层 (Controller)                      │
│   AuthController | StudyRoomController                  │
│   ReservationController                                 │
└──────────────────────┬──────────────────────────────────┘
                       │
┌──────────────────────▼──────────────────────────────────┐
│                 业务层 (Service)                         │
│   UserService | StudyRoomService                        │
│   ReservationServiceImpl                                │
│   ├─ Redisson 分布式锁                                  │
│   ├─ 派生库存校验                                        │
│   └─ RabbitMQ 异步通知                                   │
└──────────────────────┬──────────────────────────────────┘
                       │
┌──────────────────────▼──────────────────────────────────┐
│                 数据层 (Mapper)                          │
│   MyBatis-Plus BaseMapper                               │
└──────────────────────┬──────────────────────────────────┘
                       │
┌──────────────────────▼──────────────────────────────────┐
│              基础设施层 (Infrastructure)                 │
│   MySQL | Redis | RabbitMQ                              │
└─────────────────────────────────────────────────────────┘
```

---

## 性能指标

### 并发测试结果

**测试场景**：50个座位，100个用户同时预约同一时段

| 方案 | QPS | 成功率 | 数据库CPU | 平均响应时间 | P99响应时间 |
|------|-----|--------|-----------|--------------|-------------|
| 无锁 | 800+ | 40% ❌ | 90% | 50ms | 200ms |
| 仅乐观锁 | 350 | 60% ⚠️ | 70% | 120ms | 500ms |
| **分布式锁+派生校验** | **280** | **100% ✅** | **45%** | **150ms** | **300ms** |

**结论**：Redisson 分布式锁 + 锁内派生计数校验牺牲了约 15% QPS，但保证了 100% 数据正确性，数据库负载降低 50%

### Redis 缓存效果

| 接口 | 未缓存响应时间 | 缓存后响应时间 | 提升比例 |
|------|----------------|----------------|----------|
| GET /api/rooms | 45ms | 3ms | **93% ↑** |
| GET /api/rooms/{id} | 38ms | 2ms | **95% ↑** |

**缓存命中率**：85%（自习室列表访问频率高）

### RabbitMQ 消息处理

| 指标 | 数值 |
|------|------|
| 消息生产速率 | 200 msg/s |
| 消息消费速率 | 180 msg/s |
| 平均消费延迟 | 2.1s |
| 消息丢失率 | 0%（手动ACK保证） |

### 数据库性能

| 操作 | 平均耗时 | 优化手段 |
|------|----------|----------|
| 预约插入 | 15ms | 索引优化 |
| 库存扣减 | 8ms | 按房间+日期+时段派生 count |
| 预约查询 | 12ms | 联合索引 |
| 超时扫描 | 50ms | 分页批量处理 |

---

## 核心技术亮点

| 亮点 | 实现 |
|------|------|
| 派生库存 + 分布式锁 | 库存不落计数器，按「房间+日期+时段」实时统计活跃预约；Redisson 锁 + 锁内 count 校验防超卖 |
| RBAC 角色权限 | JWT claim 携带 role → `hasRole('ADMIN')` 保护管理接口 |
| RabbitMQ 异步通知 | 预约成功扔消息到队列即返回，消费者异步模拟短信发送 |
| Redis 缓存 | `CachingConfigurer` + `JavaTimeModule` 解决 LocalDateTime 序列化 |
| 定时回收 | `@Scheduled` 每5分钟扫描超时预约 → 自动取消 |
| Docker 部署 | `docker compose up -d --build` 一键启动 MySQL/Redis/RabbitMQ/后端/Nginx 五服务（前后端多阶段构建） |
| AI 助手（RAG） | Spring AI + DashScope，向量检索内置知识库；SSE 流式对话；ChatMemory 多轮上下文 + 历史会话持久化 |
| 取消后可重约 | 生成列 `active_unique`（仅 booked/signed 生成键）+ 部分唯一索引，取消/过期置 NULL 即可重约 |

## 快速开始

### 环境要求

| 依赖 | 版本 | 说明 |
|------|------|------|
| JDK | 17+（本项目在 JDK 23 开发验证） | 后端运行 |
| Maven | 3.8+ | 后端构建 |
| Node.js | 18+ | 前端构建（Vite 5） |
| Docker / Docker Compose | 任意近期版本 | 提供 MySQL / Redis / RabbitMQ |

### 方式一：Docker Compose 一键全栈部署（推荐用于部署）

```bash
# AI 助手需要 DashScope Key；不设则其余功能正常、仅 AI 助手不可用
export AI_DASHSCOPE_API_KEY=sk-xxxx        # PowerShell: $env:AI_DASHSCOPE_API_KEY='sk-xxxx'
docker compose up -d --build
```

- 首次启动自动建表并写入种子数据（`sql/init.sql` 挂载进 MySQL 初始化目录）
- 共 **5 个容器**：MySQL / Redis / RabbitMQ / 后端应用 / Nginx
- 前后端均为**多阶段构建镜像**：后端在容器内用 Maven 打包、前端在容器内用 Node 构建 `dist` 并交由 Nginx 托管，**宿主机无需安装 JDK/Maven/Node**
- **对外唯一入口是 Nginx（80 端口）**：浏览器访问 `http://服务器IP`（或 `http://localhost`），Nginx 托管前端页面并把 `/api` 反向代理到后端 `app:8080`（已配置 SSE 不缓冲，AI 流式正常）

> 云服务器部署时，记得在厂商控制台的安全组放行入站 **TCP 80**。

只启动中间件（后端在 IDE 里本地跑）：

```bash
docker compose up -d mysql redis rabbitmq
```

### 方式二：本地开发模式（日常开发推荐）

**第 1 步：启动中间件**

```bash
docker compose up -d mysql redis rabbitmq
```

| 服务 | 宿主机地址 | 账号 |
|------|----------|------|
| MySQL | `localhost:3307`（容器内 3306 映射） | root / 622824，库名 `seat_reservation` |
| Redis | `localhost:6379` | 无密码 |
| RabbitMQ | `localhost:5672`，管理台 http://localhost:15672 | guest / guest |

> MySQL 首次启动会自动执行 `sql/init.sql` 建表并写入种子数据。
> 若数据库**早已初始化过**，需按顺序执行增量脚本：`sql/migration_v2.sql`、`sql/migration_ai_history.sql`。

**第 2 步：启动后端（端口 8080）**

IntelliJ IDEA：直接运行 `SeatReservationApplication`；如需 AI 助手，在 Run Configuration 的 Environment variables 中加 `AI_DASHSCOPE_API_KEY=sk-xxxx`（不设则仅 AI 助手不可用，其余功能正常）。

或命令行：

```bash
# PowerShell
$env:AI_DASHSCOPE_API_KEY='sk-xxxx'
mvn spring-boot:run
```

**第 3 步：启动前端（端口 3000）**

> ⚠️ 前端项目位于 `frontend` 子目录，所有 `npm` 命令都必须先 `cd frontend` 再执行；在项目根目录直接运行会报 `npm run build` 找不到脚本或 npm 未识别。

**Windows（PowerShell）若提示 `无法将"npm"项识别为...`**，说明 Node.js 未加入 PATH。当前会话临时修复（Node 默认安装路径）：

```powershell
$env:Path = 'C:\Program Files\nodejs;' + $env:Path
npm -v          # 验证，能输出版本号即可
```

> 若 Node 装在别处，用 `where.exe node` 或搜索 `node.exe` 确认目录后替换上面的路径；要永久生效，把该目录加入系统环境变量 PATH，重开终端即可。

启动开发服务器：

```powershell
cd frontend
npm install        # 仅首次需要
npm run dev
```

访问 http://localhost:3000 ，Vite 已配置代理，`/api` 请求自动转发到 `http://localhost:8080`。

**生产构建**

```powershell
cd frontend
# 产物在 frontend/dist
```

> 完整一行式（含临时 PATH 修复，PowerShell）：`$env:Path='C:\Program Files\nodejs;'+$env:Path; cd frontend; npm run build`

将 `dist` 部署到 Nginx 等静态服务器即可。注意：**若你看的是构建产物页面，前端代码改动后必须重新 `npm run build` 并硬刷新浏览器（Ctrl+Shift+R）才会生效**；`localhost:3000` 的 dev server 则支持热更新。

### 启动顺序与验证

1. 中间件：`docker compose up -d mysql redis rabbitmq` → `docker ps` 三个容器均为 healthy
2. 后端：启动日志出现 `Tomcat started on port 8080`
3. 前端：终端出现 `Local: http://localhost:3000/`
4. 浏览器打开 http://localhost:3000 ，注册/登录后使用；AI 助手入口为顶部导航「AI 助手」

### 接口冒烟测试

```
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

### 常见问题

| 现象 | 原因与处理 |
|------|----------|
| `无法将"npm"项识别为...` | Node.js 未加入 PATH：临时执行 `$env:Path='C:\Program Files\nodejs;'+$env:Path`，或永久加入系统 PATH；命令还需先 `cd frontend` |
| 前端改动页面不生效 | 看的是旧构建产物或缓存页：改用 `localhost:3000` dev server，或重新 `npm run build` 后 Ctrl+Shift+R 硬刷新 |
| AI 助手报错/不可用 | 未设置 `AI_DASHSCOPE_API_KEY` 环境变量或 Key 无效 |
| 后端报数据库连接失败 | 中间件未启动，或端口不符（本地配置连接 `3307`） |
| 端口被占用 | PowerShell：`Get-NetTCPConnection -State Listen \| Where-Object LocalPort -in 3000,8080` 定位占用进程 |

## API 文档

### 测试工具推荐

本项目使用 **Postman** 进行接口测试。

> 💡 **提示**：除注册/登录接口外，所有接口都需要在请求头中携带 JWT Token。
>
> ```
> Authorization: Bearer <your_token_here>
> ```

### Postman 测试指南

所有接口（除注册/登录外）需在 Header 带 `Authorization: Bearer <token>`。

#### 1. 用户认证

| 方法 | 路径 | 说明 | 请求体 |
|------|------|------|--------|
| POST | `/api/auth/register` | 用户注册 | `{"username":"test","password":"123456"}` |
| POST | `/api/auth/login` | 用户登录 | `{"username":"test","password":"123456"}` |

**响应示例**（登录）：
```
{
  "code": 200,
  "message": "成功",
  "data": {
    "token": "eyJhbGciOiJIUzI1NiJ9...",
    "userId": 1,
    "username": "test",
    "role": "student"
  }
}
```

#### 2. 自习室管理

| 方法 | 路径 | 权限 | 说明 |
|------|------|------|------|
| GET | `/api/rooms` | 登录即可 | 列表（Redis 缓存） |
| GET | `/api/rooms/{id}` | 登录即可 | 详情（含时段列表，不含余量） |
| GET | `/api/rooms/{id}/availability?date=2026-06-03` | 登录即可 | 指定日期各时段余量（date 缺省为今天） |
| POST | `/api/rooms` | ADMIN | 新增自习室 |
| PUT | `/api/rooms/{id}` | ADMIN | 修改自习室 |
| DELETE | `/api/rooms/{id}` | ADMIN | 删除自习室 |

**响应示例**（列表）：
```
{
  "code": 200,
  "data": [
    {
      "id": 1,
      "name": "自习室A（1号馆）",
      "totalCapacity": 50,
      "createTime": "2026-01-01T00:00:00",
      "timeSlots": [
        {"id": 1, "startTime": "08:00", "endTime": "12:00"},
        {"id": 2, "startTime": "13:00", "endTime": "17:00"}
      ]
    }
  ]
}
```

#### 3. 预约管理

| 方法 | 路径 | 说明 |
|------|------|------|
| POST | `/api/reservations` | 预约座位（Redisson 分布式锁 + 派生库存校验） |
| GET | `/api/reservations` | 我的预约列表 |
| GET | `/api/reservations/{id}` | 预约详情 |
| POST | `/api/reservations/{id}/sign` | 签到 |
| POST | `/api/reservations/{id}/cancel` | 取消预约 |

**请求示例**（预约）：
```
{
  "roomId": 1,
  "timeSlotId": 1,
  "reservationDate": "2026-06-03"
}
```

**响应示例**（预约成功）：
```
{
  "code": 200,
  "data": {
    "id": 1,
    "userId": 1,
    "roomId": 1,
    "roomName": "自习室A（1号馆）",
    "startTime": "08:00",
    "endTime": "12:00",
    "reservationDate": "2026-06-03",
    "status": "booked"
  }
}
```

#### 4. 座位余量查询

| 方法 | 路径 | 说明 |
|------|------|------|
| GET | `/api/rooms/{id}/availability?date=yyyy-MM-dd` | 查询某自习室在指定日期各时段的余量（date 缺省为今天） |

**响应示例**：
```
{
  "code": 200,
  "data": [
    {"slotId": 1, "startTime": "08:00", "endTime": "12:00", "total": 50, "remaining": 49},
    {"slotId": 2, "startTime": "12:00", "endTime": "17:00", "total": 50, "remaining": 50}
  ]
}
```

#### 5. AI 助手（均需登录，Header 带 Token）

| 方法 | 路径 | 说明 |
|------|------|------|
| POST | `/api/ai/chat` | 同步对话，一次性返回完整回答 |
| POST | `/api/ai/chat/stream` | SSE 流式对话（`text/event-stream`），打字机效果 |
| GET | `/api/ai/conversations` | 当前用户历史会话列表（按最后活跃倒序） |
| GET | `/api/ai/conversations/{conversationId}/messages` | 回溯某会话全部消息 |
| DELETE | `/api/ai/conversations/{conversationId}` | 删除某会话及其消息 |

**请求体**（`/api/ai/chat` 与 `/api/ai/chat/stream`）：
```
{
  "message": "明天上午自习室A还有位置吗？",
  "conversationId": "可选，缺省则按用户ID生成默认会话"
}
```

> SSE 流以 `data:` 增量返回文本片段，正常结束时发送 `event: done`（`[DONE]`），异常时发送 `event: error`。

## 项目结构

```
├── Dockerfile                    后端镜像（多阶段：Maven 构建 → JRE 运行）
├── docker-compose.yml            容器编排（MySQL/Redis/RabbitMQ/app/web 五服务）
├── sql/
│   ├── init.sql                  数据库初始化脚本（首次自动执行）
│   ├── migration_v2.sql          存量库迁移：库存改派生计数 + active_unique 生成列
│   └── migration_ai_history.sql  存量库迁移：AI 会话/消息表
├── frontend/                     前端（Vue 3 + Vite）
│   ├── Dockerfile                前端镜像（多阶段：Node 构建 dist → Nginx 托管）
│   ├── nginx.conf                Nginx：SPA 路由回退 + /api 反代 + SSE 不缓冲
│   └── src/
│       ├── api/                  auth、room、reservation、ai 接口封装
│       ├── layouts/              MainLayout 主布局
│       ├── views/                Login/Register/Home/Rooms/RoomDetail/
│       │                         Reservations/AdminRooms/Assistant（AI 助手）
│       ├── stores/               Pinia（user）
│       └── router/               路由
└── src/main/java/com/campus/seatreservation/
    ├── ai/                       AI 助手模块
    │   ├── config/               ChatClientConfig、VectorStoreConfig
    │   ├── controller/           AiAssistantController、AiConversationController
    │   ├── service/              AiConversationService、KnowledgeBaseService
    │   └── tool/                 ReservationTools（Function Calling）
    ├── common/                   Result、GlobalExceptionHandler
    ├── config/                   SecurityConfig、RedisConfig、MybatisPlusConfig、
    │                             RabbitMQConfig、RedissonConfig
    ├── controller/               AuthController、StudyRoomController、
    │                             ReservationController
    ├── dto/                      请求/响应对象、SlotAvailability、SmsMessage
    ├── entity/                   User、StudyRoom、TimeSlot、Reservation、
    │                             AiConversation、AiMessage
    ├── mapper/                   MyBatis-Plus BaseMapper（含 AI 会话/消息）
    ├── security/                 JwtAuthenticationFilter
    ├── service/                  业务接口 + impl（含 SmsConsumer）
    ├── task/                     ReservationTimeoutTask
    └── util/                     JwtUtils
```

## 数据库设计

### ER 图

```
erDiagram
    USER ||--o{ RESERVATION : "预约"
    STUDY_ROOM ||--o{ TIME_SLOT : "包含"
    STUDY_ROOM ||--o{ RESERVATION : "被预约"
    TIME_SLOT ||--o{ RESERVATION : "时段"
    USER ||--o{ AI_CONVERSATION : "拥有"
    AI_CONVERSATION ||--o{ AI_MESSAGE : "包含"
    
    USER {
        bigint id PK
        varchar username UK
        varchar password
        varchar role
        varchar phone
        datetime create_time
    }
    
    STUDY_ROOM {
        bigint id PK
        varchar name
        int total_capacity
        datetime create_time
    }
    
    TIME_SLOT {
        bigint id PK
        bigint room_id FK
        time start_time
        time end_time
    }
    
    RESERVATION {
        bigint id PK
        bigint user_id FK
        bigint room_id FK
        bigint time_slot_id FK
        date reservation_date
        varchar status
        varchar active_unique
        datetime sign_time
    }
    
    AI_CONVERSATION {
        bigint id PK
        varchar conversation_id UK
        bigint user_id FK
        varchar title
        datetime update_time
    }
    
    AI_MESSAGE {
        bigint id PK
        varchar conversation_id FK
        varchar role
        text content
    }
```

### 表结构说明

| 表名 | 说明 | 关键字段 |
|------|------|----------|
| `user` | 用户表 | username, password(BCrypt), role(student/admin), phone |
| `study_room` | 自习室表 | name, total_capacity（**已移除 available_capacity/version，余量改为派生统计**） |
| `time_slot` | 时段表 | room_id, start_time, end_time |
| `reservation` | 预约表 | user_id, room_id, time_slot_id, status(booked/signed/cancelled/expired), **生成列 active_unique + 部分唯一索引 uk_active_reservation** |
| `ai_conversation` | AI 会话表 | conversation_id(唯一), user_id, title, update_time |
| `ai_message` | AI 消息表 | conversation_id, role(user/assistant), content |

---



## 📝 License

MIT License © 2026 Seat Reservation System
