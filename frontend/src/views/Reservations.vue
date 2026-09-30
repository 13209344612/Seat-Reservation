<template>
  <div class="page">
    <div class="page-head">
      <h1 class="page-title">我的预约</h1>
      <p class="page-desc">查看预约状态，进行签到或取消</p>
    </div>

    <!-- 状态筛选 -->
    <div class="filter-bar">
      <el-radio-group v-model="filterStatus" @change="handleFilterChange">
        <el-radio-button label="">全部</el-radio-button>
        <el-radio-button label="booked">待使用</el-radio-button>
        <el-radio-button label="signed">已签到</el-radio-button>
        <el-radio-button label="cancelled">已取消</el-radio-button>
      </el-radio-group>
    </div>

    <div class="soft-card table-card">
      <el-table
        :data="pagedList"
        v-loading="loading"
        style="width: 100%"
      >
        <el-table-column prop="roomName" label="自习室" min-width="150" />
        <el-table-column prop="reservationDate" label="预约日期" width="120" />
        <el-table-column label="时间段" width="140">
          <template #default="{ row }">
            {{ row.startTime?.substring(0, 5) }} - {{ row.endTime?.substring(0, 5) }}
          </template>
        </el-table-column>
        <el-table-column label="状态" width="100">
          <template #default="{ row }">
            <el-tag :type="getStatusType(row.status)" effect="light" round>
              {{ getStatusText(row.status) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="createTime" label="创建时间" width="180" />
        <el-table-column label="操作" width="180" fixed="right">
          <template #default="{ row }">
            <el-button
              v-if="row.status === 'booked'"
              type="success" link
              @click="handleSign(row.id)"
            >签到</el-button>
            <el-button
              v-if="row.status === 'booked'"
              type="danger" link
              @click="handleCancel(row.id)"
            >取消</el-button>
            <el-button type="primary" link @click="viewDetail(row.id)">详情</el-button>
          </template>
        </el-table-column>
        <template #empty>
          <el-empty description="暂无预约记录" />
        </template>
      </el-table>

      <div class="pagination" v-if="total > pageSize">
        <el-pagination
          v-model:current-page="currentPage"
          :page-size="pageSize"
          :total="total"
          layout="total, prev, pager, next"
          background
        />
      </div>
    </div>

    <!-- 详情对话框 -->
    <el-dialog v-model="detailVisible" title="预约详情" width="560px">
      <el-descriptions :column="1" border v-if="currentReservation">
        <el-descriptions-item label="预约ID">{{ currentReservation.id }}</el-descriptions-item>
        <el-descriptions-item label="自习室">{{ currentReservation.roomName }}</el-descriptions-item>
        <el-descriptions-item label="预约日期">{{ currentReservation.reservationDate }}</el-descriptions-item>
        <el-descriptions-item label="时间段">
          {{ currentReservation.startTime?.substring(0, 5) }} - {{ currentReservation.endTime?.substring(0, 5) }}
        </el-descriptions-item>
        <el-descriptions-item label="状态">
          <el-tag :type="getStatusType(currentReservation.status)" effect="light" round>
            {{ getStatusText(currentReservation.status) }}
          </el-tag>
        </el-descriptions-item>
        <el-descriptions-item label="创建时间">{{ currentReservation.createTime }}</el-descriptions-item>
      </el-descriptions>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { getReservationList, cancelReservation, signReservation, getReservationDetail } from '@/api/reservation'

const loading = ref(false)
const allReservations = ref([])
const filterStatus = ref('')
const currentPage = ref(1)
const pageSize = ref(10)

const detailVisible = ref(false)
const currentReservation = ref(null)

// 客户端分页：total 为筛选后总条数
const total = computed(() => allReservations.value.length)
const pagedList = computed(() => {
  const start = (currentPage.value - 1) * pageSize.value
  return allReservations.value.slice(start, start + pageSize.value)
})

const loadReservations = async () => {
  loading.value = true
  try {
    const res = await getReservationList({ status: filterStatus.value })
    allReservations.value = res.data || []
  } catch (error) {
    console.error(error)
  } finally {
    loading.value = false
  }
}

const handleFilterChange = () => {
  currentPage.value = 1
  loadReservations()
}

const handleSign = async (id) => {
  try {
    await ElMessageBox.confirm('确认要签到吗？', '提示', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning'
    })
    await signReservation(id)
    ElMessage.success('签到成功')
    loadReservations()
  } catch (error) {
    if (error !== 'cancel') {
      console.error(error)
    }
  }
}

const handleCancel = async (id) => {
  try {
    await ElMessageBox.confirm('确认要取消预约吗？', '提示', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning'
    })
    await cancelReservation(id)
    ElMessage.success('取消成功')
    loadReservations()
  } catch (error) {
    if (error !== 'cancel') {
      console.error(error)
    }
  }
}

const viewDetail = async (id) => {
  try {
    const res = await getReservationDetail(id)
    currentReservation.value = res.data
    detailVisible.value = true
  } catch (error) {
    console.error(error)
  }
}

const getStatusType = (status) => {
  const types = {
    'booked': 'warning',
    'signed': 'success',
    'cancelled': 'info',
    'expired': 'danger'
  }
  return types[status] || ''
}

const getStatusText = (status) => {
  const texts = {
    'booked': '待使用',
    'signed': '已签到',
    'cancelled': '已取消',
    'expired': '已过期'
  }
  return texts[status] || status
}

onMounted(() => {
  loadReservations()
})
</script>

<style scoped>
.filter-bar {
  margin-bottom: 16px;
}

.table-card {
  padding: 8px 12px 16px;
  overflow: hidden;
}

.pagination {
  margin-top: 16px;
  display: flex;
  justify-content: center;
}
</style>
