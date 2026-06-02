<template>
  <div class="room-detail-container">
    <el-container>
      <el-header>
        <div class="header-content">
          <h1>自习室详情</h1>
          <div class="user-info">
            <el-button @click="$router.back()">返回</el-button>
            <el-button @click="$router.push('/')">首页</el-button>
            <el-button type="danger" @click="handleLogout">退出</el-button>
          </div>
        </div>
      </el-header>

      <el-main v-loading="loading">
        <el-row :gutter="20">
          <el-col :span="12">
            <el-card>
              <div class="room-header">
                <el-icon size="80" color="#409eff"><Reading /></el-icon>
                <h2>{{ roomDetail.roomName }}</h2>
              </div>
              
              <el-descriptions :column="1" border style="margin-top: 20px;">
                <el-descriptions-item label="位置">
                  <el-icon><Location /></el-icon>
                  {{ roomDetail.location }}
                </el-descriptions-item>
                <el-descriptions-item label="总容量">
                  {{ roomDetail.totalCapacity }} 人
                </el-descriptions-item>
                <el-descriptions-item label="可用容量">
                  <el-tag type="success">{{ roomDetail.availableCapacity }} 人</el-tag>
                </el-descriptions-item>
                <el-descriptions-item label="开放时间">
                  {{ roomDetail.openTime }} - {{ roomDetail.closeTime }}
                </el-descriptions-item>
              </el-descriptions>
            </el-card>
          </el-col>

          <el-col :span="12">
            <el-card>
              <h3>预约座位</h3>
              <el-form :model="reserveForm" label-width="100px" style="margin-top: 20px;">
                <el-form-item label="预约日期">
                  <el-date-picker
                    v-model="reserveForm.reservationDate"
                    type="date"
                    placeholder="选择日期"
                    style="width: 100%"
                    :disabled-date="disabledDate"
                  />
                </el-form-item>
                
                <el-form-item label="时间段">
                  <el-select 
                    v-model="reserveForm.timeSlotId" 
                    placeholder="选择时间段"
                    style="width: 100%"
                  >
                    <el-option
                      v-for="slot in timeSlots"
                      :key="slot.id"
                      :label="`${slot.startTime} - ${slot.endTime}`"
                      :value="slot.id"
                    />
                  </el-select>
                </el-form-item>

                <el-form-item>
                  <el-button 
                    type="primary" 
                    size="large"
                    :loading="submitting"
                    @click="handleSubmit"
                    style="width: 100%"
                    :disabled="roomDetail.availableCapacity === 0"
                  >
                    确认预约
                  </el-button>
                </el-form-item>
              </el-form>
            </el-card>
          </el-col>
        </el-row>
      </el-main>
    </el-container>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { ElMessage } from 'element-plus'
import dayjs from 'dayjs'
import { getRoomDetail } from '@/api/room'
import { createReservation } from '@/api/reservation'
import { useUserStore } from '@/stores/user'

const router = useRouter()
const route = useRoute()
const userStore = useUserStore()

const loading = ref(false)
const submitting = ref(false)
const roomDetail = ref({})
const timeSlots = ref([])

const reserveForm = reactive({
  roomId: Number(route.params.id),
  reservationDate: '',
  timeSlotId: ''
})

// 禁用过去的日期
const disabledDate = (time) => {
  return time.getTime() < Date.now() - 8.64e7
}

const loadRoomDetail = async () => {
  loading.value = true
  try {
    const res = await getRoomDetail(route.params.id)
    roomDetail.value = res.data
    
    // 模拟时间段数据（实际应该从后端获取）
    timeSlots.value = [
      { id: 1, startTime: '08:00', endTime: '10:00' },
      { id: 2, startTime: '10:00', endTime: '12:00' },
      { id: 3, startTime: '14:00', endTime: '16:00' },
      { id: 4, startTime: '16:00', endTime: '18:00' },
      { id: 5, startTime: '19:00', endTime: '21:00' }
    ]
  } catch (error) {
    console.error(error)
  } finally {
    loading.value = false
  }
}

const handleSubmit = async () => {
  if (!reserveForm.reservationDate) {
    ElMessage.warning('请选择预约日期')
    return
  }
  if (!reserveForm.timeSlotId) {
    ElMessage.warning('请选择时间段')
    return
  }

  submitting.value = true
  try {
    const formData = {
      ...reserveForm,
      reservationDate: dayjs(reserveForm.reservationDate).format('YYYY-MM-DD')
    }
    await createReservation(formData)
    ElMessage.success('预约成功')
    router.push('/reservations')
  } catch (error) {
    console.error(error)
  } finally {
    submitting.value = false
  }
}

const handleLogout = () => {
  userStore.logout()
  router.push('/login')
}

onMounted(() => {
  loadRoomDetail()
})
</script>

<style scoped>
.room-detail-container {
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
  gap: 10px;
}

.el-main {
  background-color: #f5f7fa;
  padding: 20px;
}

.room-header {
  text-align: center;
  padding: 20px;
}

.room-header h2 {
  margin: 15px 0 0;
  color: #333;
}

.el-card h3 {
  margin: 0 0 20px;
  color: #333;
}
</style>
