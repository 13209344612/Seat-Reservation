<template>
  <div class="page">
    <!-- 欢迎区 -->
    <section class="hero soft-card">
      <div class="hero-text">
        <h1>你好，{{ userStore.userInfo?.username }} 👋</h1>
        <p>欢迎使用校园座位预约系统，快速预约，轻松学习。</p>
        <el-button type="primary" size="large" round @click="$router.push('/rooms')">
          开始预约
          <el-icon class="btn-icon"><ArrowRight /></el-icon>
        </el-button>
      </div>
      <div class="hero-art">
        <el-icon><Reading /></el-icon>
      </div>
    </section>

    <!-- 快捷入口 -->
    <div class="section-head">
      <h2>快捷入口</h2>
    </div>
    <el-row :gutter="20">
      <el-col
        v-for="entry in entries"
        :key="entry.path"
        :xs="24" :sm="12" :md="8"
      >
        <div class="entry-card soft-card" @click="$router.push(entry.path)">
          <span class="icon-tile entry-icon">
            <el-icon size="24"><component :is="entry.icon" /></el-icon>
          </span>
          <div class="entry-body">
            <h3>{{ entry.title }}</h3>
            <p>{{ entry.desc }}</p>
          </div>
          <el-icon class="entry-arrow"><ArrowRight /></el-icon>
        </div>
      </el-col>
    </el-row>
  </div>
</template>

<script setup>
import { computed } from 'vue'
import { useUserStore } from '@/stores/user'

const userStore = useUserStore()

const entries = computed(() => {
  const list = [
    { path: '/rooms', icon: 'Reading', title: '自习室浏览', desc: '查看可用自习室与余量' },
    { path: '/reservations', icon: 'Tickets', title: '我的预约', desc: '签到、取消与管理预约' },
    { path: '/assistant', icon: 'ChatDotRound', title: 'AI 预约助手', desc: '对话式智能查询与预约' }
  ]
  if (userStore.userInfo?.role === 'admin') {
    list.push({ path: '/admin/rooms', icon: 'Setting', title: '自习室管理', desc: '新增、编辑与删除自习室' })
  }
  return list
})
</script>

<style scoped>
.hero {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 24px;
  padding: 36px 32px;
  margin-bottom: 28px;
  background: linear-gradient(135deg, #ffffff 0%, #eef3fe 100%);
}

.hero-text h1 {
  margin: 0 0 8px;
  font-size: 26px;
  font-weight: 600;
  color: var(--app-text-title);
}

.hero-text p {
  margin: 0 0 20px;
  font-size: 15px;
  color: var(--app-text-muted);
}

.btn-icon {
  margin-left: 6px;
}

.hero-art {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 120px;
  height: 120px;
  flex-shrink: 0;
  border-radius: 24px;
  background: var(--app-primary-soft);
  color: var(--el-color-primary);
  font-size: 56px;
}

.section-head {
  margin-bottom: 16px;
}

.section-head h2 {
  margin: 0;
  font-size: 17px;
  font-weight: 600;
  color: var(--app-text-title);
}

.entry-card {
  display: flex;
  align-items: center;
  gap: 14px;
  padding: 20px;
  margin-bottom: 20px;
  cursor: pointer;
  transition: transform .2s, box-shadow .2s;
}

.entry-card:hover {
  transform: translateY(-3px);
  box-shadow: var(--app-shadow-hover);
}

.entry-icon {
  width: 52px;
  height: 52px;
  flex-shrink: 0;
}

.entry-body {
  flex: 1;
  min-width: 0;
}

.entry-body h3 {
  margin: 0 0 4px;
  font-size: 16px;
  font-weight: 600;
  color: var(--app-text-title);
}

.entry-body p {
  margin: 0;
  font-size: 13px;
  color: var(--app-text-muted);
}

.entry-arrow {
  color: #c7ccd4;
  flex-shrink: 0;
}

@media (max-width: 768px) {
  .hero {
    padding: 28px 22px;
  }
  .hero-art {
    display: none;
  }
}
</style>
