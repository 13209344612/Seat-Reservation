<template>
  <div class="home-container">
    <el-container>
      <el-header>
        <div class="header-content">
          <h1>座位预约系统</h1>
          <div class="user-info">
            <span>{{ userStore.userInfo?.username }}</span>
            <el-button type="danger" size="small" @click="handleLogout">退出</el-button>
          </div>
        </div>
      </el-header>
      
      <el-main>
        <div class="welcome-card">
          <h2>欢迎使用校园座位预约系统</h2>
          <p>快速预约，轻松学习</p>
          <el-button type="primary" size="large" @click="$router.push('/rooms')">
            开始预约
          </el-button>
        </div>

        <el-row :gutter="20" style="margin-top: 30px;">
          <el-col :span="userStore.userInfo?.role === 'admin' ? 6 : 8">
            <el-card shadow="hover" @click="$router.push('/rooms')">
              <el-icon size="40" color="#409eff"><Reading /></el-icon>
              <h3>自习室浏览</h3>
              <p>查看可用自习室</p>
            </el-card>
          </el-col>
          <el-col :span="userStore.userInfo?.role === 'admin' ? 6 : 8">
            <el-card shadow="hover" @click="$router.push('/rooms')">
              <el-icon size="40" color="#67c23a"><Calendar /></el-icon>
              <h3>在线预约</h3>
              <p>随时随地预约座位</p>
            </el-card>
          </el-col>
          <el-col :span="userStore.userInfo?.role === 'admin' ? 6 : 8">
            <el-card shadow="hover" @click="$router.push('/reservations')">
              <el-icon size="40" color="#e6a23c"><Tickets /></el-icon>
              <h3>预约管理</h3>
              <p>查看和管理我的预约</p>
            </el-card>
          </el-col>
          <el-col :span="6" v-if="userStore.userInfo?.role === 'admin'">
            <el-card shadow="hover" @click="$router.push('/admin/rooms')">
              <el-icon size="40" color="#f56c6c"><Setting /></el-icon>
              <h3>自习室管理</h3>
              <p>增删改自习室</p>
            </el-card>
          </el-col>
        </el-row>
      </el-main>
    </el-container>
  </div>
</template>

<script setup>
import { useUserStore } from '@/stores/user'
import { useRouter } from 'vue-router'

const router = useRouter()
const userStore = useUserStore()

const handleLogout = () => {
  userStore.logout()
  router.push('/login')
}
</script>

<style scoped>
.home-container {
  height: 100vh;
}

.el-header {
  background-color: #409eff;
  color: white;
  line-height: 60px;
  padding: 0 20px;
}

.header-content {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.header-content h1 {
  margin: 0;
  font-size: 24px;
}

.user-info {
  display: flex;
  align-items: center;
  gap: 15px;
}

.el-main {
  background-color: #f5f7fa;
  padding: 20px;
}

.welcome-card {
  text-align: center;
  padding: 60px 20px;
  background: white;
  border-radius: 8px;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.1);
}

.welcome-card h2 {
  font-size: 32px;
  margin-bottom: 10px;
  color: #333;
}

.welcome-card p {
  font-size: 16px;
  color: #666;
  margin-bottom: 30px;
}

.el-card {
  text-align: center;
  padding: 20px;
  cursor: pointer;
  transition: transform 0.3s;
}

.el-card:hover {
  transform: translateY(-5px);
}

.el-card h3 {
  margin: 15px 0 10px;
  color: #333;
}

.el-card p {
  color: #666;
  font-size: 14px;
}
</style>
