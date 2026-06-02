# 座位预约系统 - 前端启动指南

## 📦 安装步骤

### 1. 进入前端目录

```bash
cd frontend
```

### 2. 安装依赖

```bash
npm install
```

这将安装以下依赖：
- Vue 3
- Element Plus
- Vue Router
- Pinia
- Axios
- dayjs（日期处理）

### 3. 启动开发服务器

```bash
npm run dev
```

浏览器会自动打开 http://localhost:3000

## 🔧 后端要求

确保后端服务正在运行：

```bash
# 在后端项目根目录
mvn spring-boot:run
```

后端应该运行在 http://localhost:8080

## 🎯 主要功能页面

1. **登录页面** (`/login`)
   - 用户名/密码登录
   - JWT Token 认证

2. **注册页面** (`/register`)
   - 新用户注册
   - 表单验证

3. **首页** (`/`)
   - 系统概览
   - 快捷入口

4. **自习室列表** (`/rooms`)
   - 浏览所有自习室
   - 搜索功能
   - 查看余量

5. **自习室详情** (`/rooms/:id`)
   - 查看详细信息
   - 选择日期和时段
   - 提交预约

6. **我的预约** (`/reservations`)
   - 查看所有预约
   - 状态筛选
   - 签到/取消操作

## 🚀 生产部署

### 构建

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

    location / {
        try_files $uri $uri/ /index.html;
    }

    # 代理 API 请求到后端
    location /api {
        proxy_pass http://localhost:8080;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
    }
}
```

## 🐛 常见问题

### 1. 安装依赖失败

```bash
# 清除缓存后重试
npm cache clean --force
npm install
```

### 2. 端口被占用

修改 `vite.config.js` 中的端口号：

```javascript
server: {
  port: 3001,  // 改为其他端口
  ...
}
```

### 3. API 请求失败

检查：
- 后端是否正常运行
- `vite.config.js` 中的代理配置是否正确
- 浏览器控制台是否有错误信息

### 4. 登录后刷新页面需要重新登录

这是正常行为，Token 已保存在 localStorage 中。如果出现问题，检查：
- localStorage 是否正常
- Token 是否有效

## 📝 开发建议

1. **浏览器扩展**
   - Vue Devtools - Vue 调试工具
   - Element Plus 官方文档

2. **代码规范**
   - 使用 ESLint + Prettier
   - 遵循 Vue 3 Composition API 最佳实践

3. **状态管理**
   - 用户信息存储在 Pinia store
   - Token 同时存储在 localStorage

## 🎨 UI 定制

Element Plus 主题可以通过 CSS 变量自定义：

```css
:root {
  --el-color-primary: #409eff;
  /* 其他变量 */
}
```

参考：https://element-plus.org/zh-CN/guide/theming.html
