import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'
import { fileURLToPath, URL } from 'node:url'

// https://vitejs.dev/config/
export default defineConfig({
  plugins: [vue()],
  resolve: {
    alias: {
      '@': fileURLToPath(new URL('./src', import.meta.url))
    }
  },
  server: {
    host: '0.0.0.0',
    port: 3000,
    // 允许内网穿透工具的域名访问（避免 Vite 返回 Blocked request）
    allowedHosts: true,
    proxy: {
      // AI 助手接口代理到 Python Agent 服务（比 /api 更具体，需放在前面优先匹配）
      '/api/ai': {
        target: 'http://localhost:8000',
        changeOrigin: true
      },
      // 其余业务接口代理到 Java 后端
      '/api': {
        target: 'http://localhost:8080',
        changeOrigin: true
      }
    }
  }
})
