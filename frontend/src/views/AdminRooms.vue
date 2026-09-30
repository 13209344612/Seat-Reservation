<template>
  <div class="page">
    <div class="page-head head-row">
      <div>
        <h1 class="page-title">自习室管理</h1>
        <p class="page-desc">新增、编辑与删除自习室及其开放时段</p>
      </div>
      <el-button type="primary" round @click="openAdd">
        <el-icon><Plus /></el-icon>新增自习室
      </el-button>
    </div>

    <div class="soft-card table-card">
      <el-table :data="roomList" v-loading="loading" style="width: 100%">
        <el-table-column prop="id" label="ID" width="70" />
        <el-table-column prop="name" label="名称" min-width="160" />
        <el-table-column prop="totalCapacity" label="总容量" width="100" />
        <el-table-column label="开放时段" min-width="280">
          <template #default="{ row }">
            <el-tag
              v-for="slot in row.timeSlots"
              :key="slot.id"
              size="small"
              effect="light"
              round
              class="slot-tag"
            >
              {{ slot.startTime?.substring(0, 5) }} - {{ slot.endTime?.substring(0, 5) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="160" fixed="right">
          <template #default="{ row }">
            <el-button type="primary" link @click="openEdit(row)">编辑</el-button>
            <el-button type="danger" link @click="handleDelete(row.id)">删除</el-button>
          </template>
        </el-table-column>
        <template #empty>
          <el-empty description="暂无自习室" />
        </template>
      </el-table>
    </div>

    <!-- 新增/编辑对话框 -->
    <el-dialog v-model="dialogVisible" :title="isEdit ? '编辑自习室' : '新增自习室'" width="560px">
      <el-form :model="form" :rules="rules" ref="formRef" label-width="90px">
        <el-form-item label="名称" prop="name">
          <el-input v-model="form.name" placeholder="请输入自习室名称" />
        </el-form-item>
        <el-form-item label="总容量" prop="totalCapacity">
          <el-input-number v-model="form.totalCapacity" :min="1" />
        </el-form-item>
        <el-form-item label="时段设置">
          <div class="slot-editor">
            <div v-for="(slot, index) in form.timeSlots" :key="index" class="slot-row">
              <el-time-picker v-model="slot.startTime" format="HH:mm" value-format="HH:mm" placeholder="开始" style="width:130px" />
              <span class="slot-sep">至</span>
              <el-time-picker v-model="slot.endTime" format="HH:mm" value-format="HH:mm" placeholder="结束" style="width:130px" />
              <el-button type="danger" circle size="small" plain @click="removeSlot(index)" :disabled="form.timeSlots.length <= 1">
                <el-icon><Delete /></el-icon>
              </el-button>
            </div>
            <el-button type="primary" plain size="small" @click="addSlot">
              <el-icon><Plus /></el-icon>添加时段
            </el-button>
          </div>
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
import { ElMessage, ElMessageBox } from 'element-plus'
import { getRoomList, createRoom, updateRoom, deleteRoom } from '@/api/room'

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
  timeSlots: [{ id: null, startTime: '', endTime: '' }]
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
  form.timeSlots = [{ id: null, startTime: '', endTime: '' }]
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
    id: s.id,
    startTime: s.startTime ? s.startTime.substring(0, 5) : '',
    endTime: s.endTime ? s.endTime.substring(0, 5) : ''
  }))
  if (form.timeSlots.length === 0) {
    form.timeSlots = [{ id: null, startTime: '', endTime: '' }]
  }
  dialogVisible.value = true
}

const addSlot = () => {
  form.timeSlots.push({ id: null, startTime: '', endTime: '' })
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
      id: s.id ?? null,
      startTime: s.startTime + ':00',
      endTime: s.endTime + ':00'
    }))

    const payload = {
      name: form.name,
      totalCapacity: form.totalCapacity,
      timeSlots
    }

    submitting.value = true
    try {
      if (isEdit.value) {
        await updateRoom(editingId.value, payload)
        ElMessage.success('修改成功')
      } else {
        await createRoom(payload)
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

onMounted(() => {
  loadRooms()
})
</script>

<style scoped>
.head-row {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 16px;
}

.table-card {
  padding: 8px 12px 12px;
  overflow: hidden;
}

.slot-tag {
  margin: 3px 6px 3px 0;
}

.slot-editor {
  width: 100%;
}

.slot-row {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 10px;
}

.slot-sep {
  color: var(--app-text-muted);
  font-size: 14px;
}
</style>
