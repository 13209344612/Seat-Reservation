# ---- 构建阶段：用 Node 打包前端到 dist ----
FROM node:20-alpine AS build
WORKDIR /app

# 先只拷贝依赖清单，充分利用 Docker 层缓存
COPY package*.json ./
RUN npm ci || npm install

# 再拷贝源码并构建（产物在 /app/dist）
COPY . .
RUN npm run build

# ---- 运行阶段：Nginx 托管静态资源 + 反向代理 /api ----
FROM nginx:1.27-alpine
# 前端构建产物
COPY --from=build /app/dist /usr/share/nginx/html
# 自定义 Nginx 配置（覆盖默认站点）
COPY nginx.conf /etc/nginx/conf.d/default.conf
EXPOSE 80
CMD ["nginx", "-g", "daemon off;"]
