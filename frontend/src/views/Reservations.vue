<template>
  <div class="reservations-container">
    <el-container>
      <el-header>
        <div class="header-content">
          <h1>我的预约</h1>
          <div class="user-info">
            <el-button @click="$router.push('/')">首页</el-button>
            <el-button @click="$router.push('/rooms')">自习室</el-button>
            <el-button v-if="userStore.userInfo?.role === 'admin'" @click="$router.push('/admin/rooms')">自习室管理</el-button>
            <el-button type="danger" @click="handleLogout">退出</el-button>
          </div>
        </div>
      </el-header>

      <el-main>
        <div class="filter-bar">
          <el-radio-group v-model="filterStatus" @change="loadReservations">
            <el-radio-button label="">全部</el-radio-button>
            <el-radio-button label="booked">待使用</el-radio-button>
            <el-radio-button label="signed">已签到</el-radio-button>
            <el-radio-button label="cancelled">已取消</el-radio-button>
          </el-radio-group>
        </div>

        <el-table 
          :data="reservationList" 
          v-loading="loading"
          style="width: 100%"
        >
          <el-table-column prop="id" label="ID" width="80" />
          <el-table-column prop="roomName" label="自习室" min-width="150" />
          <el-table-column prop="reservationDate" label="预约日期" width="120" />
          <el-table-column label="时间段" width="130">
            <template #default="{ row }">
              {{ row.startTime?.substring(0, 5) }} - {{ row.endTime?.substring(0, 5) }}
            </template>
          </el-table-column>
          <el-table-column label="状态" width="100">
            <template #default="{ row }">
              <el-tag :type="getStatusType(row.status)">
                {{ getStatusText(row.status) }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column prop="createTime" label="创建时间" width="180" />
          <el-table-column label="操作" width="200" fixed="right">
            <template #default="{ row }">
              <el-button 
                v-if="row.status === 'booked'"
                type="success" 
                size="small"
                @click="handleSign(row.id)"
              >
                签到
              </el-button>
              <el-button 
                v-if="row.status === 'booked'"
                type="danger" 
                size="small"
                @click="handleCancel(row.id)"
              >
                取消
              </el-button>
              <el-button 
                size="small"
                @click="viewDetail(row.id)"
              >
                详情
              </el-button>
            </template>
          </el-table-column>
        </el-table>

        <el-empty v-if="!loading && reservationList.length === 0" description="暂无预约记录" />

        <!-- 分页 -->
        <div class="pagination" v-if="total > 0">
          <el-pagination
            v-model:current-page="currentPage"
            v-model:page-size="pageSize"
            :total="total"
            layout="total, prev, pager, next"
            @current-change="loadReservations"
          />
        </div>
      </el-main>
    </el-container>

    <!-- 详情对话框 -->
    <el-dialog v-model="detailVisible" title="预约详情" width="600px">
      <el-descriptions :column="1" border v-if="currentReservation">
        <el-descriptions-item label="预约ID">{{ currentReservation.id }}</el-descriptions-item>
        <el-descriptions-item label="自习室">{{ currentReservation.roomName }}</el-descriptions-item>
        <el-descriptions-item label="预约日期">{{ currentReservation.reservationDate }}</el-descriptions-item>
        <el-descriptions-item label="时间段">
          {{ currentReservation.startTime?.substring(0, 5) }} - {{ currentReservation.endTime?.substring(0, 5) }}
        </el-descriptions-item>
        <el-descriptions-item label="状态">
          <el-tag :type="getStatusType(currentReservation.status)">
            {{ getStatusText(currentReservation.status) }}
          </el-tag>
        </el-descriptions-item>
        <el-descriptions-item label="创建时间">{{ currentReservation.createTime }}</el-descriptions-item>
      </el-descriptions>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { getReservationList, cancelReservation, signReservation, getReservationDetail } from '@/api/reservation'
import { useUserStore } from '@/stores/user'

const router = useRouter()
const userStore = useUserStore()

const loading = ref(false)
const reservationList = ref([])
const filterStatus = ref('')
const currentPage = ref(1)
const pageSize = ref(10)
const total = ref(0)

const detailVisible = ref(false)
const currentReservation = ref(null)

const loadReservations = async () => {
  loading.value = true
  try {
    const res = await getReservationList({
      status: filterStatus.value,
      pageNum: currentPage.value,
      pageSize: pageSize.value
    })
    reservationList.value = res.data || []
    total.value = (res.data || []).length
  } catch (error) {
    console.error(error)
  } finally {
    loading.value = false
  }
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

const handleLogout = () => {
  userStore.logout()
  router.push('/login')
}

onMounted(() => {
  loadReservations()
})
</script>

<style scoped>
.reservations-container {
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

.filter-bar {
  margin-bottom: 20px;
}

.pagination {
  margin-top: 20px;
  display: flex;
  justify-content: center;
}
</style>
