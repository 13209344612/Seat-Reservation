<template>
  <div class="layout">
    <header class="app-header">
      <div class="header-inner">
        <div class="brand" @click="go('/')">
          <span class="brand-logo"><el-icon><Notebook /></el-icon></span>
          <span class="brand-name">座位预约</span>
        </div>

        <!-- 桌面端横向菜单 -->
        <el-menu
          class="desktop-menu"
          mode="horizontal"
          router
          :default-active="activeMenu"
          :ellipsis="false"
        >
          <el-menu-item v-for="item in navItems" :key="item.path" :index="item.path">
            <el-icon><component :is="item.icon" /></el-icon>
            <span>{{ item.label }}</span>
          </el-menu-item>
        </el-menu>

        <div class="header-right">
          <!-- 窄屏汉堡菜单 -->
          <el-dropdown class="mobile-menu" trigger="click" @command="handleCommand">
            <el-button text circle>
              <el-icon size="20"><Menu /></el-icon>
            </el-button>
            <template #dropdown>
              <el-dropdown-menu>
                <el-dropdown-item v-for="item in navItems" :key="item.path" :command="item.path">
                  <el-icon><component :is="item.icon" /></el-icon>{{ item.label }}
                </el-dropdown-item>
              </el-dropdown-menu>
            </template>
          </el-dropdown>

          <!-- 用户下拉 -->
          <el-dropdown trigger="click" @command="handleCommand">
            <span class="user-chip">
              <span class="avatar">{{ userInitial }}</span>
              <span class="username">{{ userStore.userInfo?.username }}</span>
              <el-icon class="caret"><ArrowDown /></el-icon>
            </span>
            <template #dropdown>
              <el-dropdown-menu>
                <el-dropdown-item command="logout" divided>
                  <el-icon><SwitchButton /></el-icon>退出登录
                </el-dropdown-item>
              </el-dropdown-menu>
            </template>
          </el-dropdown>
        </div>
      </div>
    </header>

    <main class="app-main">
      <router-view />
    </main>
  </div>
</template>

<script setup>
import { computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useUserStore } from '@/stores/user'

const route = useRoute()
const router = useRouter()
const userStore = useUserStore()

const isAdmin = computed(() => userStore.userInfo?.role === 'admin')

const navItems = computed(() => {
  const items = [
    { path: '/', label: '首页', icon: 'HomeFilled' },
    { path: '/rooms', label: '自习室', icon: 'Reading' },
    { path: '/reservations', label: '我的预约', icon: 'Tickets' },
    { path: '/assistant', label: 'AI 助手', icon: 'ChatDotRound' }
  ]
  if (isAdmin.value) {
    items.push({ path: '/admin/rooms', label: '自习室管理', icon: 'Setting' })
  }
  return items
})

// /rooms/:id 时仍高亮“自习室”
const activeMenu = computed(() => {
  if (route.path.startsWith('/rooms')) return '/rooms'
  return route.path
})

const userInitial = computed(() => {
  const name = userStore.userInfo?.username || ''
  return name ? name.charAt(0).toUpperCase() : 'U'
})

const go = (path) => router.push(path)

const handleCommand = (cmd) => {
  if (cmd === 'logout') {
    userStore.logout()
    router.push('/login')
  } else {
    router.push(cmd)
  }
}
</script>

<style scoped>
.layout {
  min-height: 100%;
  display: flex;
  flex-direction: column;
  background: var(--app-page-bg);
}

.app-header {
  position: sticky;
  top: 0;
  z-index: 100;
  background: #fff;
  border-bottom: 1px solid var(--app-border);
  box-shadow: 0 1px 3px rgba(16, 24, 40, .04);
}

.header-inner {
  max-width: 1200px;
  margin: 0 auto;
  height: 60px;
  padding: 0 20px;
  display: flex;
  align-items: center;
  gap: 20px;
}

.brand {
  display: flex;
  align-items: center;
  gap: 10px;
  cursor: pointer;
  flex-shrink: 0;
}

.brand-logo {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 32px;
  height: 32px;
  border-radius: 9px;
  background: var(--el-color-primary);
  color: #fff;
  font-size: 18px;
}

.brand-name {
  font-size: 17px;
  font-weight: 600;
  color: var(--app-text-title);
  white-space: nowrap;
}

.desktop-menu {
  flex: 1;
  border-bottom: none !important;
}

.desktop-menu :deep(.el-menu-item) {
  height: 60px;
  line-height: 60px;
  border-bottom: 2px solid transparent;
}

.desktop-menu :deep(.el-menu-item.is-active) {
  border-bottom-color: var(--el-color-primary);
}

.header-right {
  display: flex;
  align-items: center;
  gap: 8px;
  flex-shrink: 0;
}

.user-chip {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 4px 10px 4px 4px;
  border-radius: 999px;
  cursor: pointer;
  transition: background .2s;
  outline: none;
}

.user-chip:hover {
  background: #f3f4f6;
}

.user-chip .avatar {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 30px;
  height: 30px;
  border-radius: 50%;
  background: var(--app-primary-soft);
  color: var(--el-color-primary);
  font-weight: 600;
  font-size: 14px;
}

.user-chip .username {
  font-size: 14px;
  color: var(--app-text-body);
  max-width: 120px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.user-chip .caret {
  font-size: 12px;
  color: var(--app-text-muted);
}

.app-main {
  flex: 1;
}

.mobile-menu {
  display: none;
}

@media (max-width: 768px) {
  .desktop-menu {
    display: none;
  }
  .mobile-menu {
    display: inline-flex;
  }
  .username {
    display: none;
  }
  .header-inner {
    gap: 8px;
    padding: 0 12px;
  }
}
</style>
