# 🎓 校园座位预约系统 - 完整项目

> 基于 Spring Boot + Vue 3 的全栈座位预约系统

---

## 📊 项目概览

这是一个完整的校园座位预约系统，包含后端 API 服务和前端用户界面。

### 技术架构

```
┌─────────────────────────────────────────────┐
│              前端 (Vue 3)                    │
│  Element Plus + Vite + Pinia + Vue Router   │
└──────────────────┬──────────────────────────┘
                   │ HTTP/REST API
                   │ (Axios)
┌──────────────────▼──────────────────────────┐
│            后端 (Spring Boot)                │
│  Spring Security + JWT + MyBatis Plus       │
└──────────────────┬──────────────────────────┘
                   │ JDBC
┌──────────────────▼──────────────────────────┐
│          数据库 (MySQL + Redis)              │
│  MySQL: 持久化数据                           │
│  Redis: 缓存 + 分布式锁                      │
└─────────────────────────────────────────────┘
```

---

## 🗂️ 项目结构

```
SeatReservation/
│
├── 📁 src/                          # 后端源代码
│   ├── main/java/com/campus/seatreservation/
│   │   ├── common/                  # 通用类
│   │   │   └── GlobalExceptionHandler.java
│   │   │   └── Result.java
│   │   ├── config/                  # 配置类
│   │   │   ├── MybatisPlusConfig.java
│   │   │   ├── RabbitMQConfig.java
│   │   │   ├── RedisConfig.java
│   │   │   ├── RedissonConfig.java
│   │   │   └── SecurityConfig.java
│   │   ├── controller/              # 控制器
│   │   │   ├── AuthController.java
│   │   │   ├── ReservationController.java
│   │   │   └── StudyRoomController.java
│   │   ├── dto/                     # 数据传输对象
│   │   │   ├── LoginRequest.java
│   │   │   ├── LoginResponse.java
│   │   │   ├── RegisterRequest.java
│   │   │   ├── ReserveRequest.java
│   │   │   ├── ReserveResponse.java
│   │   │   └── ...
│   │   ├── entity/                  # 实体类
│   │   │   ├── Reservation.java
│   │   │   ├── StudyRoom.java
│   │   │   ├── TimeSlot.java
│   │   │   └── User.java
│   │   ├── mapper/                  # MyBatis Mapper
│   │   │   ├── ReservationMapper.java
│   │   │   ├── StudyRoomMapper.java
│   │   │   ├── TimeSlotMapper.java
│   │   │   └── UserMapper.java
│   │   ├── security/                # 安全相关
│   │   │   └── JwtAuthenticationFilter.java
│   │   ├── service/                 # 服务层
│   │   │   ├── impl/
│   │   │   │   ├── ReservationServiceImpl.java
│   │   │   │   ├── SmsConsumer.java
│   │   │   │   ├── StudyRoomServiceImpl.java
│   │   │   │   └── UserServiceImpl.java
│   │   │   ├── ReservationService.java
│   │   │   ├── StudyRoomService.java
│   │   │   └── UserService.java
│   │   ├── task/                    # 定时任务
│   │   │   └── ReservationTimeoutTask.java
│   │   ├── util/                    # 工具类
│   │   │   └── JwtUtils.java
│   │   └── SeatReservationApplication.java
│   │
│   └── main/resources/              # 资源配置
│       ├── application.yml
│       ├── db/migration/
│       ├── static/
│       └── templates/
│
├── 📁 frontend/                     # 前端项目 ⭐ 新增
│   ├── src/
│   │   ├── api/                     # API 接口
│   │   │   ├── auth.js
│   │   │   ├── room.js
│   │   │   └── reservation.js
│   │   ├── router/                  # 路由配置
│   │   │   └── index.js
│   │   ├── stores/                  # 状态管理
│   │   │   └── user.js
│   │   ├── utils/                   # 工具函数
│   │   │   └── request.js
│   │   ├── views/                   # 页面组件
│   │   │   ├── Login.vue           # 登录页
│   │   │   ├── Register.vue        # 注册页
│   │   │   ├── Home.vue            # 首页
│   │   │   ├── Rooms.vue           # 自习室列表
│   │   │   ├── RoomDetail.vue      # 自习室详情
│   │   │   └── Reservations.vue    # 我的预约
│   │   ├── App.vue
│   │   └── main.js
│   ├── index.html
│   ├── vite.config.js
│   ├── package.json
│   └── README.md
│
├── 📁 sql/                          # 数据库脚本
│   └── init.sql
│
├── 📄 文档
│   ├── README.md
│   ├── VUE_FRONTEND_GUIDE.md       # Vue 前端完整指南 ⭐
│   ├── FRONTEND_STARTUP.md         # 前端启动指南 ⭐
│   ├── FRONTEND_SUMMARY.md         # 前端完成说明 ⭐
│   ├── DOCKER_DEPLOYMENT.md        # Docker 部署指南
│   ├── 项目升级方案.md
│   ├── 短信通知升级方案.md
│   └── 面试准备.md
│
├── 📄 配置文件
│   ├── pom.xml                      # Maven 配置
│   ├── docker-compose.yml           # Docker Compose
│   ├── Dockerfile                   # Docker 镜像
│   └── .dockerignore
│
└── 🚀 启动脚本
    └── start-frontend.ps1          # 前端一键启动 ⭐
```

---

## 🎯 核心功能

### 后端功能

#### 1. 用户认证
- ✅ JWT Token 认证
- ✅ Spring Security 权限控制
- ✅ BCrypt 密码加密
- ✅ 用户注册和登录

#### 2. 自习室管理
- ✅ 自习室 CRUD
- ✅ 容量管理
- ✅ 时间段管理
- ✅ 乐观锁版本控制

#### 3. 预约系统
- ✅ 在线预约
- ✅ 分布式锁（Redisson）
- ✅ 预约取消
- ✅ 签到功能
- ✅ 超时自动释放

#### 4. 高并发处理
- ✅ Redisson 分布式锁
- ✅ 乐观锁机制
- ✅ 数据库事务
- ✅ 消息队列（RabbitMQ）

#### 5. 其他功能
- ✅ 短信通知（异步）
- ✅ 定时任务
- ✅ 全局异常处理
- ✅ 统一响应格式

### 前端功能

#### 1. 用户界面
- ✅ 登录/注册页面
- ✅ 表单验证
- ✅ 错误提示
- ✅ 加载状态

#### 2. 自习室浏览
- ✅ 卡片式列表
- ✅ 搜索功能
- ✅ 详情查看
- ✅ 容量显示

#### 3. 预约操作
- ✅ 日期选择
- ✅ 时段选择
- ✅ 预约提交
- ✅ 预约管理

#### 4. 用户体验
- ✅ 响应式设计
- ✅ 路由守卫
- ✅ Token 自动管理
- ✅ 友好的交互反馈

---

## 🚀 快速开始

### 方式一：分别启动

#### 1️⃣ 启动后端

```bash
# 确保 MySQL 和 Redis 已启动
mvn spring-boot:run
```

后端运行在：http://localhost:8080

#### 2️⃣ 启动前端

**Windows PowerShell:**
```powershell
# 在项目根目录右键运行
start-frontend.ps1
```

**或手动启动:**
```bash
cd frontend
npm install
npm run dev
```

前端运行在：http://localhost:3000

### 方式二：Docker 一键启动

```bash
docker compose up -d
```

---

## 📝 使用流程

### 首次使用

1. **访问前端**: http://localhost:3000
2. **注册账号**: 点击"立即注册"
3. **登录系统**: 输入用户名和密码
4. **浏览自习室**: 查看可用自习室
5. **预约座位**: 选择日期和时段，提交预约
6. **查看预约**: 在"我的预约"中查看
7. **签到使用**: 到自习室后点击"签到"

### 日常使用

1. 登录系统
2. 查看我的预约
3. 按时签到使用
4. 或使用完毕后离开

---

## 🔧 技术栈对比

| 层次 | 技术 | 说明 |
|------|------|------|
| **前端框架** | Vue 3 | Composition API |
| **UI 组件库** | Element Plus | 基于 Vue 3 |
| **构建工具** | Vite | 快速开发体验 |
| **状态管理** | Pinia | Vue 官方推荐 |
| **路由管理** | Vue Router | SPA 路由 |
| **HTTP 客户端** | Axios | 请求封装 |
| **后端框架** | Spring Boot 3.5.14 | Java 17 |
| **安全框架** | Spring Security | JWT 认证 |
| **ORM 框架** | MyBatis Plus | 增强 MyBatis |
| **数据库** | MySQL | 关系型数据库 |
| **缓存** | Redis | 缓存 + 分布式锁 |
| **消息队列** | RabbitMQ | 异步消息 |
| **分布式锁** | Redisson | Redis 客户端 |

---

## 📊 数据库设计

### 核心表结构

#### 1. 用户表 (user)
```sql
- id: 用户ID
- username: 用户名
- password: 密码（BCrypt）
- role: 角色（student/admin）
- phone: 手机号
- create_time: 创建时间
```

#### 2. 自习室表 (study_room)
```sql
- id: 自习室ID
- room_name: 自习室名称
- location: 位置
- total_capacity: 总容量
- available_capacity: 可用容量
- open_time: 开放时间
- close_time: 关闭时间
- version: 版本号（乐观锁）
```

#### 3. 预约表 (reservation)
```sql
- id: 预约ID
- user_id: 用户ID
- room_id: 自习室ID
- time_slot_id: 时段ID
- reservation_date: 预约日期
- status: 状态（PENDING/SIGNED/CANCELLED/EXPIRED）
- create_time: 创建时间
```

#### 4. 时段表 (time_slot)
```sql
- id: 时段ID
- room_id: 自习室ID
- start_time: 开始时间
- end_time: 结束时间
```

---

## 🔐 安全特性

- ✅ JWT Token 认证
- ✅ BCrypt 密码加密
- ✅ Spring Security 权限控制
- ✅ CORS 跨域配置
- ✅ SQL 注入防护
- ✅ XSS 攻击防护
- ✅ 分布式锁防超卖

---

## 📈 性能优化

### 后端优化
- ✅ Redis 缓存热点数据
- ✅ MyBatis Plus 分页查询
- ✅ 数据库索引优化
- ✅ 异步消息处理
- ✅ 连接池配置

### 前端优化
- ✅ Vite 快速热重载
- ✅ 路由懒加载
- ✅ 组件按需引入
- ✅ 图片懒加载
- ✅ 防抖节流

---

## 🧪 测试

### 后端测试
```bash
mvn test
```

### 前端测试
```bash
# 单元测试（待添加）
npm run test
```

---

## 📦 部署

### 开发环境
- 后端: `mvn spring-boot:run`
- 前端: `npm run dev`

### 生产环境
- 后端: 打包为 JAR 运行
- 前端: 构建静态文件，部署到 Nginx
- 或使用 Docker Compose 一键部署

详细部署方案参考：[DOCKER_DEPLOYMENT.md](./DOCKER_DEPLOYMENT.md)

---

## 📚 文档导航

### 前端相关
- 📘 [VUE_FRONTEND_GUIDE.md](./VUE_FRONTEND_GUIDE.md) - Vue 前端完整指南
- 📗 [FRONTEND_STARTUP.md](./FRONTEND_STARTUP.md) - 前端启动指南
- 📙 [FRONTEND_SUMMARY.md](./FRONTEND_SUMMARY.md) - 前端完成说明

### 后端相关
- 📕 [README.md](./README.md) - 项目主文档
- 📔 [DOCKER_DEPLOYMENT.md](./DOCKER_DEPLOYMENT.md) - Docker 部署

### 其他文档
- 📋 [项目升级方案.md](./项目升级方案.md)
- 📋 [短信通知升级方案.md](./短信通知升级方案.md)
- 📋 [面试准备.md](./面试准备.md)

---

## 🎓 学习要点

### 后端技术点
1. Spring Boot 3 新特性
2. Spring Security + JWT 认证
3. MyBatis Plus 使用
4. Redis 缓存和分布式锁
5. RabbitMQ 消息队列
6. 事务管理和乐观锁
7. 全局异常处理

### 前端技术点
1. Vue 3 Composition API
2. Element Plus 组件库
3. Vue Router 路由管理
4. Pinia 状态管理
5. Axios HTTP 请求
6. 响应式布局
7. 表单验证

---

## 🤝 贡献

欢迎提交 Issue 和 Pull Request！

---

## 📄 License

MIT

---

## 🎉 总结

这是一个**功能完整、技术先进、代码规范**的全栈项目：

✅ **后端**: Spring Boot + 微服务架构  
✅ **前端**: Vue 3 + Element Plus 现代化 UI  
✅ **数据库**: MySQL + Redis 高性能组合  
✅ **安全**: JWT + Spring Security 多层防护  
✅ **性能**: 缓存 + 分布式锁 + 消息队列  
✅ **部署**: Docker 容器化部署  

**适合学习、面试、项目实战！** 🚀

---

**开始使用吧！** Happy Coding! 💻
