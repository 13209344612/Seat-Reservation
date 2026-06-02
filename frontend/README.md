# 座位预约系统前端

基于 Vue 3 + Element Plus 的校园座位预约系统前端应用。

## 技术栈

- **Vue 3** - 渐进式 JavaScript 框架
- **Vite** - 下一代前端构建工具
- **Element Plus** - 基于 Vue 3 的组件库
- **Vue Router** - Vue.js 官方路由管理器
- **Pinia** - Vue 官方状态管理库
- **Axios** - HTTP 客户端

## 功能特性

- ✅ 用户登录/注册
- ✅ 自习室浏览和搜索
- ✅ 在线预约座位
- ✅ 预约管理（查看、取消、签到）
- ✅ 响应式设计

## 快速开始

### 安装依赖

```bash
npm install
```

### 开发模式

```bash
npm run dev
```

访问 http://localhost:3000

### 构建生产版本

```bash
npm run build
```

### 预览生产构建

```bash
npm run preview
```

## 项目结构

```
frontend/
├── src/
│   ├── api/              # API 接口
│   │   ├── auth.js
│   │   ├── room.js
│   │   └── reservation.js
│   ├── router/           # 路由配置
│   │   └── index.js
│   ├── stores/           # 状态管理
│   │   └── user.js
│   ├── utils/            # 工具函数
│   │   └── request.js
│   ├── views/            # 页面组件
│   │   ├── Login.vue
│   │   ├── Register.vue
│   │   ├── Home.vue
│   │   ├── Rooms.vue
│   │   ├── RoomDetail.vue
│   │   └── Reservations.vue
│   ├── App.vue           # 根组件
│   └── main.js           # 入口文件
├── index.html
├── vite.config.js        # Vite 配置
└── package.json
```

## 与后端集成

开发环境下，Vite 配置了代理，将 `/api` 请求转发到后端服务：

- 前端: http://localhost:3000
- 后端: http://localhost:8080

确保后端服务已启动。

## 部署

构建后的静态文件可以部署到任何 Web 服务器：

1. 运行 `npm run build` 生成 `dist` 目录
2. 将 `dist` 目录的内容部署到 Nginx、Apache 或其他静态文件服务器

## 注意事项

- 需要 dayjs 库来处理日期，请运行：`npm install dayjs`
- 确保后端 API 正常运行
- Token 存储在 localStorage 中
