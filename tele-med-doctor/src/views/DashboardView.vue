<template>
  <div class="dashboard-container">
    <div class="dashboard-header">
      <h2>医生工作台</h2>
      <div class="header-right">
        <span class="user-info">{{ userStore.user?.realName || userStore.user?.username }}</span>
        <el-button type="danger" text @click="handleLogout">退出登录</el-button>
      </div>
    </div>
    <el-row :gutter="20" class="dashboard-content">
      <el-col :span="12">
        <el-card>
          <template #header>
            <div class="card-header">
              <span>待接诊</span>
              <el-tag type="warning">{{ waitingList.length }}</el-tag>
            </div>
          </template>
          <el-table :data="waitingList" stripe style="width: 100%" v-loading="waitingLoading">
            <el-table-column prop="patientName" label="患者姓名" />
            <el-table-column prop="createTime" label="请求时间" width="180">
              <template #default="{ row }">
                {{ formatTime(row.createTime) }}
              </template>
            </el-table-column>
            <el-table-column label="操作" width="120">
              <template #default="{ row }">
                <el-button type="primary" size="small" @click="handleAccept(row)">接诊</el-button>
              </template>
            </el-table-column>
          </el-table>
        </el-card>
      </el-col>
      <el-col :span="12">
        <el-card>
          <template #header>
            <div class="card-header">
              <span>预约列表</span>
            </div>
          </template>
          <el-table :data="appointmentList" stripe style="width: 100%" v-loading="appointmentLoading">
            <el-table-column prop="patientName" label="患者姓名" />
            <el-table-column prop="appointmentDate" label="预约日期" width="120" />
            <el-table-column label="时间段" width="100">
              <template #default="{ row }">
                {{ row.timeSlotDesc || (row.timeSlot === 0 ? '上午' : '下午') }}
              </template>
            </el-table-column>
            <el-table-column label="状态" width="100">
              <template #default="{ row }">
                <el-tag :type="statusTagType(row.status)">{{ row.statusText || statusLabel(row.status) }}</el-tag>
              </template>
            </el-table-column>
            <el-table-column label="操作" width="120">
              <template #default="{ row }">
                <el-button
                  type="success"
                  size="small"
                  @click="handleStart(row)"
                  :disabled="row.status !== 0 && row.status !== 1"
                >开始</el-button>
              </template>
            </el-table-column>
          </el-table>
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, onUnmounted } from 'vue'
import { useRouter } from 'vue-router'
import { useUserStore } from '@/stores/user'
import { getWaitingList, acceptConsultation } from '@/api/consultation'
import { getDoctorAppointments, startAppointment } from '@/api/appointment'
import { ElMessage } from 'element-plus'
import type { Consultation, Appointment } from '@/types'

const router = useRouter()
const userStore = useUserStore()
const waitingList = ref<Consultation[]>([])
const appointmentList = ref<Appointment[]>([])
const waitingLoading = ref(false)
const appointmentLoading = ref(false)
let refreshTimer: ReturnType<typeof setInterval> | null = null

const formatTime = (time: string) => {
  if (!time) return ''
  return new Date(time).toLocaleString()
}

const statusTagType = (status: number) => {
  const map: Record<number, string> = {
    0: 'warning',
    1: 'primary',
    2: 'info',
    3: 'success',
    4: 'danger'
  }
  return map[status] || 'info'
}

const statusLabel = (status: number) => {
  const map: Record<number, string> = {
    0: '待确认',
    1: '已确认',
    2: '已取消',
    3: '已完成',
    4: '进行中'
  }
  return map[status] || '未知'
}

const fetchWaitingList = async () => {
  waitingLoading.value = true
  try {
    const res = await getWaitingList()
    waitingList.value = res.data || []
  } catch {
    waitingList.value = []
  } finally {
    waitingLoading.value = false
  }
}

const fetchAppointments = async () => {
  if (!userStore.doctorInfo?.id) return
  appointmentLoading.value = true
  try {
    const res = await getDoctorAppointments(userStore.doctorInfo.id)
    appointmentList.value = res.data || []
  } catch {
    appointmentList.value = []
  } finally {
    appointmentLoading.value = false
  }
}

const handleAccept = async (row: Consultation) => {
  if (!userStore.doctorInfo?.id) return
  try {
    const res = await acceptConsultation(row.id, userStore.doctorInfo.id)
    ElMessage.success('接诊成功')
    router.push(`/consultation/${res.data.id}`)
  } catch {
    ElMessage.error('接诊失败')
  }
}

const handleStart = async (row: Appointment) => {
  if (!userStore.doctorInfo?.id) return
  try {
    const res = await startAppointment(row.id, userStore.doctorInfo.id)
    ElMessage.success('已开始问诊')
    if (res.data.consultationId) {
      router.push(`/consultation/${res.data.consultationId}`)
    }
  } catch {
    ElMessage.error('开始问诊失败')
  }
}

const handleLogout = () => {
  userStore.logout()
}

onMounted(() => {
  fetchWaitingList()
  fetchAppointments()
  refreshTimer = setInterval(fetchWaitingList, 10000)
})

onUnmounted(() => {
  if (refreshTimer) {
    clearInterval(refreshTimer)
    refreshTimer = null
  }
})
</script>

<style scoped>
.dashboard-container {
  padding: 20px;
  background: #f5f7fa;
  min-height: 100vh;
}

.dashboard-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20px;
  padding: 16px 24px;
  background: #fff;
  border-radius: 8px;
  box-shadow: 0 2px 4px rgba(0, 0, 0, 0.06);
}

.dashboard-header h2 {
  margin: 0;
  color: #303133;
}

.header-right {
  display: flex;
  align-items: center;
  gap: 12px;
}

.user-info {
  color: #606266;
  font-size: 14px;
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  font-weight: 600;
  font-size: 16px;
}

.dashboard-content {
  margin-top: 0;
}
</style>
