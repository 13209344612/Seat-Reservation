<template>
  <div class="page" v-loading="loading">
    <div class="page-head">
      <el-button text class="back-link" @click="$router.push('/rooms')">
        <el-icon><ArrowLeft /></el-icon>返回列表
      </el-button>
      <h1 class="page-title">{{ roomDetail.name || '自习室详情' }}</h1>
    </div>

    <el-row :gutter="20">
      <!-- 左：自习室信息 -->
      <el-col :xs="24" :md="14">
        <div class="soft-card info-card">
          <div class="info-cover">
            <el-icon size="54"><Reading /></el-icon>
          </div>
          <div class="info-body">
            <h2>{{ roomDetail.name }}</h2>
            <el-descriptions :column="2" border class="info-desc-table">
              <el-descriptions-item label="总容量">
                {{ roomDetail.totalCapacity }} 人 / 时段
              </el-descriptions-item>
              <el-descriptions-item label="开放时段">
                {{ timeSlots.length }} 个
              </el-descriptions-item>
            </el-descriptions>
            <div class="open-slots">
              <div class="open-slots-title">每日开放时段</div>
              <div class="open-slot-list">
                <span v-for="slot in timeSlots" :key="slot.id" class="open-slot-chip">
                  <el-icon><Clock /></el-icon>
                  {{ slot.startTime?.substring(0, 5) }} - {{ slot.endTime?.substring(0, 5) }}
                </span>
                <span v-if="!timeSlots.length" class="muted">暂无时段</span>
              </div>
            </div>
          </div>
        </div>
      </el-col>

      <!-- 右：预约 -->
      <el-col :xs="24" :md="10">
        <div class="soft-card reserve-card">
          <h3 class="card-title">预约座位</h3>

          <div class="field">
            <label class="field-label">预约日期</label>
            <el-date-picker
              v-model="reserveForm.reservationDate"
              type="date"
              placeholder="选择日期"
              style="width: 100%"
              :disabled-date="disabledDate"
              @change="handleDateChange"
            />
          </div>

          <div class="field">
            <label class="field-label">时间段</label>
            <div v-if="availability.length" class="slot-grid">
              <button
                v-for="slot in availability"
                :key="slot.slotId"
                type="button"
                class="slot-chip"
                :class="{ active: reserveForm.timeSlotId === slot.slotId, disabled: slot.remaining === 0 }"
                :disabled="slot.remaining === 0"
                @click="selectSlot(slot)"
              >
                <span class="slot-time">{{ slot.startTime.substring(0, 5) }} - {{ slot.endTime.substring(0, 5) }}</span>
                <span class="slot-left">{{ slot.remaining === 0 ? '已满' : `剩余 ${slot.remaining}/${slot.total}` }}</span>
              </button>
            </div>
            <div v-else class="slot-empty">
              {{ reserveForm.reservationDate ? '暂无可用时段' : '请先选择日期' }}
            </div>
          </div>

          <el-button
            type="primary"
            size="large"
            round
            style="width: 100%"
            :loading="submitting"
            :disabled="!canSubmit"
            @click="handleSubmit"
          >
            确认预约
          </el-button>
        </div>
      </el-col>
    </el-row>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { ElMessage } from 'element-plus'
import dayjs from 'dayjs'
import { getRoomDetail, getAvailability } from '@/api/room'
import { createReservation } from '@/api/reservation'

const router = useRouter()
const route = useRoute()

const loading = ref(false)
const submitting = ref(false)
const roomDetail = ref({})
const timeSlots = ref([])
const availability = ref([])

const reserveForm = reactive({
  roomId: Number(route.params.id),
  reservationDate: '',
  timeSlotId: ''
})

// 禁用过去的日期
const disabledDate = (time) => {
  return time.getTime() < Date.now() - 8.64e7
}

// 选中时段的剩余座位
const selectedRemaining = computed(() => {
  const slot = availability.value.find(s => s.slotId === reserveForm.timeSlotId)
  return slot ? slot.remaining : 0
})

// 是否可提交：已选日期、已选时段且该时段仍有余量
const canSubmit = computed(() => {
  return !!reserveForm.reservationDate && !!reserveForm.timeSlotId && selectedRemaining.value > 0
})

// 选择时段（满员不可选）
const selectSlot = (slot) => {
  if (slot.remaining === 0) return
  reserveForm.timeSlotId = slot.slotId
}

// 加载指定日期的各时段余量
const loadAvailability = async (date) => {
  if (!date) {
    availability.value = []
    return
  }
  try {
    const res = await getAvailability(route.params.id, dayjs(date).format('YYYY-MM-DD'))
    availability.value = res.data || []
  } catch (error) {
    console.error(error)
    availability.value = []
  }
}

// 日期变化：重置已选时段并重新加载余量
const handleDateChange = (val) => {
  reserveForm.timeSlotId = ''
  loadAvailability(val)
}

const loadRoomDetail = async () => {
  loading.value = true
  try {
    const res = await getRoomDetail(route.params.id)
    roomDetail.value = res.data
    timeSlots.value = res.data.timeSlots || []
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

onMounted(() => {
  loadRoomDetail()
})
</script>

<style scoped>
.back-link {
  padding-left: 0;
  color: var(--app-text-muted);
  margin-bottom: 6px;
}

.info-card {
  padding: 0;
  overflow: hidden;
  margin-bottom: 20px;
}

.info-cover {
  display: flex;
  align-items: center;
  justify-content: center;
  height: 150px;
  background: var(--app-primary-soft);
  color: var(--el-color-primary);
}

.info-body {
  padding: 22px 24px 24px;
}

.info-body h2 {
  margin: 0 0 16px;
  font-size: 22px;
  font-weight: 600;
  color: var(--app-text-title);
}

.open-slots {
  margin-top: 18px;
}

.open-slots-title {
  font-size: 14px;
  font-weight: 500;
  color: var(--app-text-body);
  margin-bottom: 10px;
}

.open-slot-list {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.open-slot-chip {
  display: inline-flex;
  align-items: center;
  gap: 5px;
  padding: 6px 12px;
  border-radius: 999px;
  background: var(--app-primary-soft);
  color: var(--el-color-primary);
  font-size: 13px;
}

.reserve-card {
  padding: 22px 24px 24px;
  margin-bottom: 20px;
}

.card-title {
  margin: 0 0 20px;
  font-size: 17px;
  font-weight: 600;
  color: var(--app-text-title);
}

.field {
  margin-bottom: 20px;
}

.field-label {
  display: block;
  margin-bottom: 8px;
  font-size: 14px;
  font-weight: 500;
  color: var(--app-text-body);
}

.slot-grid {
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.slot-chip {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 10px;
  padding: 12px 16px;
  border: 1px solid var(--el-border-color);
  border-radius: 10px;
  background: #fff;
  cursor: pointer;
  transition: all .18s;
  font-family: inherit;
}

.slot-chip:hover:not(.disabled) {
  border-color: var(--el-color-primary-light-5);
}

.slot-chip.active {
  border-color: var(--el-color-primary);
  background: var(--app-primary-soft);
}

.slot-chip.disabled {
  cursor: not-allowed;
  background: #f6f7f9;
  color: #b4b8bf;
}

.slot-time {
  font-size: 15px;
  font-weight: 500;
  color: var(--app-text-title);
}

.slot-chip.disabled .slot-time {
  color: #b4b8bf;
}

.slot-left {
  font-size: 13px;
  color: var(--app-text-muted);
}

.slot-chip.active .slot-left {
  color: var(--el-color-primary);
}

.slot-empty {
  padding: 18px;
  text-align: center;
  font-size: 14px;
  color: var(--app-text-muted);
  background: #f8f9fb;
  border-radius: 10px;
}

.info-desc-table {
  margin-top: 4px;
}
</style>
