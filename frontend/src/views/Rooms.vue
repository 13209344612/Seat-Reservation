<template>
  <div class="rooms-container">
    <el-container>
      <el-header>
        <div class="header-content">
          <h1>自习室列表</h1>
          <div class="user-info">
            <span>{{ userStore.userInfo?.username }}</span>
            <el-button @click="$router.push('/')">首页</el-button>
            <el-button @click="$router.push('/reservations')">我的预约</el-button>
            <el-button type="danger" @click="handleLogout">退出</el-button>
          </div>
        </div>
      </el-header>

      <el-main>
        <div class="search-bar">
          <el-input
            v-model="searchKeyword"
            placeholder="搜索自习室名称或位置"
            prefix-icon="Search"
            clearable
            style="width: 400px"
            @clear="loadRooms"
            @keyup.enter="loadRooms"
          >
            <template #append>
              <el-button icon="Search" @click="loadRooms">搜索</el-button>
            </template>
          </el-input>
        </div>

        <el-row :gutter="20" v-loading="loading">
          <el-col 
            :span="8" 
            v-for="room in roomList" 
            :key="room.id"
          >
            <el-card class="room-card" shadow="hover" @click="viewDetail(room.id)">
              <div class="room-image">
                <el-icon size="60" color="#409eff"><Reading /></el-icon>
              </div>
              <h3>{{ room.roomName }}</h3>
              <p class="location">
                <el-icon><Location /></el-icon>
                {{ room.location }}
              </p>
              <div class="room-info">
                <el-tag type="success">容量: {{ room.totalCapacity }}</el-tag>
                <el-tag type="warning">剩余: {{ room.availableCapacity }}</el-tag>
              </div>
              <p class="time">{{ room.openTime }} - {{ room.closeTime }}</p>
              <el-button 
                type="primary" 
                style="width: 100%; margin-top: 10px"
                :disabled="room.availableCapacity === 0"
              >
                {{ room.availableCapacity > 0 ? '立即预约' : '已满' }}
              </el-button>
            </el-card>
          </el-col>
        </el-row>

        <el-empty v-if="!loading && roomList.length === 0" description="暂无自习室" />
      </el-main>
    </el-container>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { getRoomList } from '@/api/room'
import { useUserStore } from '@/stores/user'

const router = useRouter()
const userStore = useUserStore()

const loading = ref(false)
const roomList = ref([])
const searchKeyword = ref('')

const loadRooms = async () => {
  loading.value = true
  try {
    const res = await getRoomList({ keyword: searchKeyword.value })
    roomList.value = res.data || []
  } catch (error) {
    console.error(error)
  } finally {
    loading.value = false
  }
}

const viewDetail = (id) => {
  router.push(`/rooms/${id}`)
}

const handleLogout = () => {
  userStore.logout()
  router.push('/login')
}

onMounted(() => {
  loadRooms()
})
</script>

<style scoped>
.rooms-container {
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
  gap: 10px;
}

.el-main {
  background-color: #f5f7fa;
  padding: 20px;
}

.search-bar {
  margin-bottom: 20px;
  text-align: center;
}

.room-card {
  cursor: pointer;
  transition: transform 0.3s;
  margin-bottom: 20px;
  text-align: center;
}

.room-card:hover {
  transform: translateY(-5px);
}

.room-image {
  height: 120px;
  display: flex;
  align-items: center;
  justify-content: center;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  border-radius: 4px;
  margin-bottom: 15px;
}

.room-card h3 {
  margin: 0 0 10px;
  color: #333;
  font-size: 18px;
}

.location {
  color: #666;
  margin: 10px 0;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 5px;
}

.room-info {
  display: flex;
  gap: 10px;
  justify-content: center;
  margin: 10px 0;
}

.time {
  color: #999;
  font-size: 14px;
  margin: 10px 0;
}
</style>
