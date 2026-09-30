<template>
  <div class="page">
    <div class="page-head">
      <h1 class="page-title">自习室列表</h1>
      <p class="page-desc">选择一间自习室，查看时段余量并预约座位</p>
    </div>

    <!-- 搜索栏 -->
    <div class="toolbar soft-card">
      <el-input
        v-model="searchKeyword"
        placeholder="搜索自习室名称"
        clearable
        class="search-input"
        @clear="loadRooms"
        @keyup.enter="loadRooms"
      >
        <template #prefix>
          <el-icon><Search /></el-icon>
        </template>
      </el-input>
      <el-button type="primary" @click="loadRooms">搜索</el-button>
    </div>

    <!-- 房间卡片 -->
    <el-row :gutter="20" v-loading="loading">
      <el-col
        v-for="room in roomList"
        :key="room.id"
        :xs="24" :sm="12" :md="8"
      >
        <div class="room-card soft-card" @click="viewDetail(room.id)">
          <div class="room-cover">
            <el-icon size="46"><Reading /></el-icon>
          </div>
          <div class="room-body">
            <h3 class="room-name">{{ room.name }}</h3>
            <div class="room-meta">
              <el-icon><Clock /></el-icon>
              <span>{{ (room.timeSlots || []).length }} 个开放时段</span>
            </div>
            <div class="slot-tags">
              <el-tag
                v-for="slot in room.timeSlots"
                :key="slot.id"
                size="small"
                effect="plain"
                round
                class="slot-tag"
              >
                {{ slot.startTime?.substring(0, 5) }} - {{ slot.endTime?.substring(0, 5) }}
              </el-tag>
            </div>
            <div class="room-foot">
              <el-tag type="primary" effect="light" round>
                容量 {{ room.totalCapacity }} 人 / 时段
              </el-tag>
              <span class="room-link">
                查看详情<el-icon><ArrowRight /></el-icon>
              </span>
            </div>
          </div>
        </div>
      </el-col>
    </el-row>

    <el-empty v-if="!loading && roomList.length === 0" description="暂无自习室" />
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { getRoomList } from '@/api/room'

const router = useRouter()

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

onMounted(() => {
  loadRooms()
})
</script>

<style scoped>
.toolbar {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 16px;
  margin-bottom: 20px;
}

.search-input {
  max-width: 420px;
}

.room-card {
  overflow: hidden;
  margin-bottom: 20px;
  cursor: pointer;
  padding: 0;
  transition: transform .2s, box-shadow .2s;
}

.room-card:hover {
  transform: translateY(-3px);
  box-shadow: var(--app-shadow-hover);
}

.room-cover {
  display: flex;
  align-items: center;
  justify-content: center;
  height: 120px;
  background: var(--app-primary-soft);
  color: var(--el-color-primary);
}

.room-body {
  padding: 16px 18px 18px;
}

.room-name {
  margin: 0 0 8px;
  font-size: 17px;
  font-weight: 600;
  color: var(--app-text-title);
}

.room-meta {
  display: flex;
  align-items: center;
  gap: 5px;
  font-size: 13px;
  color: var(--app-text-muted);
  margin-bottom: 10px;
}

.slot-tags {
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
  min-height: 48px;
  margin-bottom: 14px;
}

.slot-tag {
  margin: 0;
}

.room-foot {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 8px;
}

.room-link {
  display: inline-flex;
  align-items: center;
  gap: 3px;
  font-size: 13px;
  color: var(--el-color-primary);
  white-space: nowrap;
}
</style>
