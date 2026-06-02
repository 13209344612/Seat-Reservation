# 🚀 座位预约系统 - Vue 3 + Element Plus 前端

## ✨ 项目概述

这是一个基于 **Vue 3** 和 **Element Plus** 构建的校园座位预约系统前端应用，提供完整的用户认证、自习室浏览、在线预约和预约管理功能。

---

## 📋 目录

- [技术栈](#技术栈)
- [功能特性](#功能特性)
- [快速开始](#快速开始)
- [项目结构](#项目结构)
- [开发指南](#开发指南)
- [部署说明](#部署说明)
- [常见问题](#常见问题)

---

## 🛠️ 技术栈

| 技术 | 版本 | 说明 |
|------|------|------|
| Vue | 3.4.21+ | 渐进式 JavaScript 框架 |
| Vite | 5.2.0+ | 下一代前端构建工具 |
| Element Plus | 2.6.3+ | Vue 3 UI 组件库 |
| Vue Router | 4.3.0+ | 官方路由管理器 |
| Pinia | 2.1.7+ | Vue 官方状态管理库 |
| Axios | 1.6.8+ | HTTP 客户端 |
| Day.js | 1.11.10+ | 轻量级日期处理库 |

---

## 🎯 功能特性

### ✅ 已实现功能

#### 1. 用户认证
- 🔐 用户登录（JWT Token 认证）
- 📝 用户注册（表单验证）
- 🔄 自动登录（Token 持久化）
- 🚪 退出登录

#### 2. 自习室管理
- 📚 自习室列表展示（卡片式布局）
- 🔍 搜索功能（名称/位置）
- 📊 容量信息实时显示
- 🏫 自习室详情查看
- ⏰ 时间段选择

#### 3. 预约系统
- 📅 在线预约（日期+时段）
- 📋 我的预约列表
- 🔖 状态筛选（待使用/已签到/已取消）
- ✅ 在线签到
- ❌ 取消预约
- 📄 预约详情查看

#### 4. 用户体验
- 🎨 现代化 UI 设计
- 📱 响应式布局
- ⚡ 加载状态提示
- 💬 操作反馈（成功/失败）
- ⚠️ 表单验证
- 🔒 路由权限控制

---

## 🚀 快速开始

### 前置要求

- Node.js >= 16.0.0
- npm >= 7.0.0
- 后端服务运行在 http://localhost:8080

### 方式一：一键启动（推荐）

在项目根目录右键点击 `start-frontend.ps1`，选择"使用 PowerShell 运行"

### 方式二：手动启动

```bash
# 1. 进入前端目录
cd frontend

# 2. 安装依赖
npm install

# 3. 启动开发服务器
npm run dev
```

浏览器将自动打开：**http://localhost:3000**

### 首次使用流程

1. 访问 http://localhost:3000
2. 点击"立即注册"创建账号
3. 使用注册的账号登录
4. 浏览自习室并预约

---

## 📁 项目结构

```
frontend/
├── src/
│   ├── api/                    # API 接口层
│   │   ├── auth.js            # 认证接口（登录/注册）
│   │   ├── room.js            # 自习室接口
│   │   └── reservation.js     # 预约接口
│   │
│   ├── router/                 # 路由配置
│   │   └── index.js           # 路由定义和守卫
│   │
│   ├── stores/                 # 状态管理
│   │   └── user.js            # 用户信息和 Token
│   │
│   ├── utils/                  # 工具函数
│   │   └── request.js         # Axios 封装
│   │
│   ├── views/                  # 页面组件
│   │   ├── Login.vue          # 登录页
│   │   ├── Register.vue       # 注册页
│   │   ├── Home.vue           # 首页
│   │   ├── Rooms.vue          # 自习室列表
│   │   ├── RoomDetail.vue     # 自习室详情
│   │   └── Reservations.vue   # 我的预约
│   │
│   ├── App.vue                 # 根组件
│   └── main.js                 # 入口文件
│
├── .env.development            # 开发环境变量
├── .env.production             # 生产环境变量
├── index.html                  # HTML 模板
├── vite.config.js              # Vite 配置
├── package.json                # 依赖配置
└── README.md                   # 项目说明
```

---

## 💻 开发指南

### 常用命令

```bash
# 安装依赖
npm install

# 启动开发服务器（热重载）
npm run dev

# 构建生产版本
npm run build

# 预览生产构建
npm run preview
```

### 开发环境配置

#### API 代理

`vite.config.js` 中已配置：

```javascript
server: {
  port: 3000,
  proxy: {
    '/api': {
      target: 'http://localhost:8080',
      changeOrigin: true
    }
  }
}
```

#### 环境变量

- `.env.development` - 开发环境配置
- `.env.production` - 生产环境配置

### 添加新页面

1. 在 `src/views/` 创建新组件
2. 在 `src/router/index.js` 添加路由
3. 如需 API，在 `src/api/` 创建对应接口文件

### 状态管理示例

```javascript
import { useUserStore } from '@/stores/user'

const userStore = useUserStore()

// 获取用户信息
console.log(userStore.userInfo)

// 设置 Token
userStore.setToken(token)

// 退出登录
userStore.logout()
```

### API 调用示例

```javascript
import { getRoomList } from '@/api/room'

const loadRooms = async () => {
  const res = await getRoomList({ keyword: '' })
  roomList.value = res.data || []
}
```

---

## 🌐 部署说明

### 构建生产版本

```bash
npm run build
```

生成的文件在 `dist` 目录

### Nginx 配置示例

```nginx
server {
    listen 80;
    server_name your-domain.com;
    root /path/to/dist;
    index index.html;

    # SPA 路由支持
    location / {
        try_files $uri $uri/ /index.html;
    }

    # API 代理到后端
    location /api {
        proxy_pass http://localhost:8080;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
    }
}
```

### Docker 部署（可选）

创建 `Dockerfile`：

```dockerfile
FROM node:18-alpine AS build
WORKDIR /app
COPY package*.json ./
RUN npm install
COPY . .
RUN npm run build

FROM nginx:alpine
COPY --from=build /app/dist /usr/share/nginx/html
EXPOSE 80
CMD ["nginx", "-g", "daemon off;"]
```

---

## ❓ 常见问题

### 1. 依赖安装失败

```bash
# 清除缓存
npm cache clean --force

# 使用淘宝镜像
npm config set registry https://registry.npmmirror.com

# 重新安装
rm -rf node_modules
npm install
```

### 2. 端口被占用

修改 `vite.config.js`：

```javascript
server: {
  port: 3001,  // 改为其他端口
}
```

### 3. API 请求 404

- ✅ 确认后端服务已启动
- ✅ 检查 `vite.config.js` 代理配置
- ✅ 查看浏览器控制台 Network 面板

### 4. 登录后刷新页面丢失状态

这是正常的，Token 和用户信息存储在 localStorage 中，刷新后会自动恢复。

如果出现问题：
- 检查浏览器是否禁用了 localStorage
- 查看 Application -> Local Storage 是否有数据

### 5. 样式显示异常

- ✅ 清除浏览器缓存
- ✅ 确保 Element Plus CSS 正确引入
- ✅ 检查控制台是否有 CSS 错误

### 6. 路由跳转空白页

- ✅ 检查路由配置是否正确
- ✅ 确认组件路径是否正确
- ✅ 查看控制台错误信息

---

## 🔗 相关文档

- [FRONTEND_STARTUP.md](./FRONTEND_STARTUP.md) - 详细启动指南
- [FRONTEND_SUMMARY.md](./FRONTEND_SUMMARY.md) - 项目完成说明
- [frontend/README.md](./frontend/README.md) - 前端项目说明

---

## 📚 学习资源

- [Vue 3 官方文档](https://cn.vuejs.org/)
- [Element Plus 文档](https://element-plus.org/zh-CN/)
- [Vite 官方文档](https://cn.vitejs.dev/)
- [Vue Router 文档](https://router.vuejs.org/zh/)
- [Pinia 文档](https://pinia.vuejs.org/zh/)

---

## 🎨 自定义主题

Element Plus 支持主题定制：

```css
/* 在 src/main.js 或全局样式中 */
:root {
  --el-color-primary: #409eff;
  --el-color-success: #67c23a;
  --el-color-warning: #e6a23c;
  --el-color-danger: #f56c6c;
}
```

参考：[Element Plus 主题定制](https://element-plus.org/zh-CN/guide/theming.html)

---

## 🤝 贡献指南

欢迎提交 Issue 和 Pull Request！

---

## 📄 License

MIT

---

## 🎉 总结

这是一个功能完整、代码规范的 Vue 3 + Element Plus 前端项目，包含：

✅ 完整的用户认证流程  
✅ 优雅的 UI 设计和交互  
✅ 模块化的代码组织  
✅ 完善的路由权限控制  
✅ 统一的 HTTP 请求封装  
✅ 响应式布局适配  

**立即开始使用吧！** 🚀

---

**如有问题，请查阅文档或联系开发者。**
