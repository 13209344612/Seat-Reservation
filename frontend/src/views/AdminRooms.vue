<template>
  <div class="admin-container">
    <el-container>
      <el-header>
        <div class="header-content">
          <h1>自习室管理（管理员）</h1>
          <div class="user-info">
            <span>{{ userStore.userInfo?.username }}</span>
            <el-button @click="$router.push('/')">首页</el-button>
            <el-button @click="$router.push('/rooms')">自习室</el-button>
            <el-button type="danger" @click="handleLogout">退出</el-button>
          </div>
        </div>
      </el-header>

      <el-main>
        <div class="toolbar">
          <el-button type="primary" @click="openAdd">
            <el-icon><Plus /></el-icon> 新增自习室
          </el-button>
        </div>

        <el-table :data="roomList" v-loading="loading" border>
          <el-table-column prop="id" label="ID" width="60" />
          <el-table-column prop="name" label="名称" min-width="180" />
          <el-table-column prop="totalCapacity" label="总容量" width="100" />
          <el-table-column prop="availableCapacity" label="剩余" width="100" />
          <el-table-column label="时段" min-width="260">
            <template #default="{ row }">
              <el-tag v-for="slot in row.timeSlots" :key="slot.id" size="small" style="margin-right:6px">
                {{ slot.startTime?.substring(0, 5) }} - {{ slot.endTime?.substring(0, 5) }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column label="操作" width="180" fixed="right">
            <template #default="{ row }">
              <el-button type="primary" size="small" @click="openEdit(row)">编辑</el-button>
              <el-button type="danger" size="small" @click="handleDelete(row.id)">删除</el-button>
            </template>
          </el-table-column>
        </el-table>
      </el-main>
    </el-container>

    <!-- 新增/编辑对话框 -->
    <el-dialog v-model="dialogVisible" :title="isEdit ? '编辑自习室' : '新增自习室'" width="550px">
      <el-form :model="form" :rules="rules" ref="formRef" label-width="100px">
        <el-form-item label="名称" prop="name">
          <el-input v-model="form.name" placeholder="请输入自习室名称" />
        </el-form-item>
        <el-form-item label="总容量" prop="totalCapacity">
          <el-input-number v-model="form.totalCapacity" :min="1" />
        </el-form-item>
        <el-form-item label="时段设置">
          <div v-for="(slot, index) in form.timeSlots" :key="index" class="slot-row">
            <el-time-picker v-model="slot.startTime" format="HH:mm" value-format="HH:mm" placeholder="开始时间" style="width:150px" />
            <span style="margin: 0 8px">至</span>
            <el-time-picker v-model="slot.endTime" format="HH:mm" value-format="HH:mm" placeholder="结束时间" style="width:150px" />
            <el-button type="danger" :icon="Delete" circle size="small" @click="removeSlot(index)" :disabled="form.timeSlots.length <= 1" />
          </div>
          <el-button type="success" size="small" @click="addSlot" style="margin-top: 8px">
            <el-icon><Plus /></el-icon> 添加时段
          </el-button>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="submitting" @click="handleSubmit">确定</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { getRoomList, createRoom, updateRoom, deleteRoom } from '@/api/room'
import { useUserStore } from '@/stores/user'

const router = useRouter()
const userStore = useUserStore()

const loading = ref(false)
const submitting = ref(false)
const roomList = ref([])
const dialogVisible = ref(false)
const isEdit = ref(false)
const editingId = ref(null)
const formRef = ref(null)

const form = reactive({
  name: '',
  totalCapacity: 10,
  timeSlots: [{ startTime: '', endTime: '' }]
})

const rules = {
  name: [{ required: true, message: '请输入名称', trigger: 'blur' }]
}

const loadRooms = async () => {
  loading.value = true
  try {
    const res = await getRoomList()
    roomList.value = res.data || []
  } catch (error) {
    console.error(error)
  } finally {
    loading.value = false
  }
}

const resetForm = () => {
  form.name = ''
  form.totalCapacity = 10
  form.timeSlots = [{ startTime: '', endTime: '' }]
  editingId.value = null
  isEdit.value = false
}

const openAdd = () => {
  resetForm()
  dialogVisible.value = true
}

const openEdit = (row) => {
  isEdit.value = true
  editingId.value = row.id
  form.name = row.name
  form.totalCapacity = row.totalCapacity
  form.timeSlots = (row.timeSlots || []).map(s => ({
    startTime: s.startTime ? s.startTime.substring(0, 5) : '',
    endTime: s.endTime ? s.endTime.substring(0, 5) : ''
  }))
  if (form.timeSlots.length === 0) {
    form.timeSlots = [{ startTime: '', endTime: '' }]
  }
  dialogVisible.value = true
}

const addSlot = () => {
  form.timeSlots.push({ startTime: '', endTime: '' })
}

const removeSlot = (index) => {
  form.timeSlots.splice(index, 1)
}

const handleSubmit = async () => {
  if (!formRef.value) return
  await formRef.value.validate(async (valid) => {
    if (!valid) return

    const validSlots = form.timeSlots.filter(s => s.startTime && s.endTime)
    if (validSlots.length === 0) {
      ElMessage.warning('至少需要一个完整的时段')
      return
    }

    const timeSlots = validSlots.map(s => ({
      startTime: s.startTime + ':00',
      endTime: s.endTime + ':00'
    }))

    submitting.value = true
    try {
      if (isEdit.value) {
        await updateRoom(editingId.value, {
          name: form.name,
          totalCapacity: form.totalCapacity,
          timeSlots
        })
        ElMessage.success('修改成功')
      } else {
        await createRoom({
          name: form.name,
          totalCapacity: form.totalCapacity,
          timeSlots
        })
        ElMessage.success('新增成功')
      }
      dialogVisible.value = false
      loadRooms()
    } catch (error) {
      console.error(error)
    } finally {
      submitting.value = false
    }
  })
}

const handleDelete = async (id) => {
  try {
    await ElMessageBox.confirm('确认删除该自习室？', '警告', {
      type: 'warning',
      confirmButtonText: '确定',
      cancelButtonText: '取消'
    })
    await deleteRoom(id)
    ElMessage.success('删除成功')
    loadRooms()
  } catch (error) {
    if (error !== 'cancel') console.error(error)
  }
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
.admin-container { height: 100vh; }
.el-header { background-color: #409eff; color: white; line-height: 60px; padding: 0 20px; }
.header-content { display: flex; justify-content: space-between; align-items: center; }
.header-content h1 { margin: 0; font-size: 24px; }
.user-info { display: flex; align-items: center; gap: 10px; }
.el-main { background-color: #f5f7fa; padding: 20px; }
.toolbar { margin-bottom: 16px; }
.slot-row { display: flex; align-items: center; margin-bottom: 8px; }
</style>
