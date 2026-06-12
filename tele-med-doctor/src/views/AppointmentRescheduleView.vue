<template>
  <div class="reschedule-view">
    <div class="page-header">
      <h2>预约改约</h2>
      <el-button @click="goBack">
        <el-icon><ArrowLeft /></el-icon>
        返回
      </el-button>
    </div>

    <el-row :gutter="20">
      <el-col :span="10">
        <el-card class="appointment-card">
          <template #header>
            <div class="card-head">
              <span class="title">原预约信息</span>
            </div>
          </template>

          <div class="search-section">
            <el-form :inline="true" :model="searchForm" size="default">
              <el-form-item label="预约ID">
                <el-input
                  v-model="searchForm.appointmentId"
                  placeholder="请输入预约ID"
                  clearable
                  style="width: 200px"
                  @keyup.enter="handleSearchAppointment"
                />
              </el-form-item>
              <el-form-item>
                <el-button type="primary" @click="handleSearchAppointment">查询</el-button>
              </el-form-item>
            </el-form>

            <el-divider content-position="left">
              <span style="font-size: 12px; color: #909399;">或从患者预约列表选择</span>
            </el-divider>

            <el-form :inline="true" size="default">
              <el-form-item label="患者ID">
                <el-input v-model="patientIdForList" placeholder="输入患者ID" style="width: 160px;" />
              </el-form-item>
              <el-form-item>
                <el-button @click="loadPatientAppointments">加载列表</el-button>
              </el-form-item>
            </el-form>

            <div class="appointment-list" v-loading="loadingList" v-if="patientAppointments.length > 0">
              <div
                v-for="apt in patientAppointments"
                :key="apt.id"
                class="apt-item"
                :class="{ active: currentAppointment?.id === apt.id }"
                @click="selectAppointment(apt)"
              >
                <div class="apt-top">
                  <span class="apt-no">#{{ apt.id }}</span>
                  <el-tag size="small" :type="aptTagType(apt.status)">{{ apt.statusText || ('状态' + apt.status) }}</el-tag>
                </div>
                <div class="apt-info">
                  <div><el-icon><User /></el-icon> {{ apt.patientName || ('患者' + apt.patientId) }}</div>
                  <div><el-icon><Calendar /></el-icon> {{ apt.appointmentDate }} {{ apt.timeSlotDesc || ('时段' + apt.timeSlot) }}</div>
                  <div><el-icon><UserFilled /></el-icon> {{ apt.doctorName || '' }} {{ apt.doctorDepartment || '' }}</div>
                </div>
              </div>
            </div>
          </div>

          <el-empty
            v-if="!loadingAppointment && !currentAppointment"
            description="请输入预约ID查询或从列表选择"
            :image-size="80"
            style="padding: 30px 0;"
          />

          <div v-if="currentAppointment" class="detail-section">
            <el-descriptions :column="1" border size="default" title="原预约详情">
              <el-descriptions-item label="预约编号">
                <span class="hl">#{{ currentAppointment.id }}</span>
              </el-descriptions-item>
              <el-descriptions-item label="患者信息">
                <div>{{ currentAppointment.patientName || ('患者' + currentAppointment.patientId) }}</div>
                <div class="sub">患者ID: {{ currentAppointment.patientId }}</div>
              </el-descriptions-item>
              <el-descriptions-item label="就诊医生">
                <div class="hl">{{ currentAppointment.doctorName || '' }}</div>
                <div class="sub">{{ currentAppointment.doctorTitle || '' }} · {{ currentAppointment.doctorDepartment || '' }}</div>
                <div class="sub light">{{ currentAppointment.hospitalName || '' }}</div>
              </el-descriptions-item>
              <el-descriptions-item label="就诊日期">
                <span class="hl">{{ currentAppointment.appointmentDate }}</span>
                <span class="day-label" style="margin-left: 6px;">({{ getDayName(currentAppointment.appointmentDate) }})</span>
              </el-descriptions-item>
              <el-descriptions-item label="就诊时段">
                <span class="hl time">{{ currentAppointment.timeSlotDesc || ('时段' + currentAppointment.timeSlot) }}</span>
              </el-descriptions-item>
              <el-descriptions-item label="当前状态">
                <el-tag :type="aptTagType(currentAppointment.status)">
                  {{ currentAppointment.statusText || ('状态' + currentAppointment.status) }}
                </el-tag>
              </el-descriptions-item>
              <el-descriptions-item v-if="currentAppointment.description" label="备注">
                {{ currentAppointment.description }}
              </el-descriptions-item>
              <el-descriptions-item label="创建时间">
                {{ currentAppointment.createTime }}
              </el-descriptions-item>
            </el-descriptions>

            <el-alert
              v-if="currentAppointment"
              type="info"
              :closable="false"
              style="margin-top: 16px;"
              title="请在右侧选择新的就诊时段"
              description="改约成功后，原时段号源将自动释放，新时段号源自动扣减。"
              show-icon
            />
          </div>
        </el-card>
      </el-col>

      <el-col :span="14">
        <el-card class="slots-card" v-loading="loadingSlots" shadow="never">
          <template #header>
            <div class="card-head">
              <span class="title">
                可选择的新时段
                <el-tag v-if="currentDoctorId" type="primary" size="small" style="margin-left: 8px;">
                  {{ currentDoctorName }} · 未来7天
                </el-tag>
              </span>
              <el-button size="small" @click="refreshAvailableSlots" :disabled="!currentDoctorId">
                <el-icon><Refresh /></el-icon>
                刷新
              </el-button>
            </div>
          </template>

          <el-empty
            v-if="!currentDoctorId && !loadingSlots"
            description="请先在左侧选择要改约的预约"
            :image-size="100"
            style="padding: 60px 0;"
          />

          <div v-else-if="weekSlots.length === 0 && !loadingSlots" class="empty">
            <el-empty description="未来7天暂无排班" :image-size="100" style="padding: 60px 0;" />
          </div>

          <div v-else class="week-grid">
            <div v-for="day in weekSlots" :key="day.date" class="day-col">
              <div class="day-header" :class="{ today: isToday(day.date) }">
                <div class="day-name">{{ getDayName(day.date) }}</div>
                <div class="day-date">{{ formatShortDate(day.date) }}</div>
                <el-tag v-if="isToday(day.date)" size="small" type="danger" effect="dark">今天</el-tag>
              </div>
              <div class="day-slots">
                <div
                  v-for="slot in day.slots"
                  :key="slot.slotTime"
                  class="slot-cell"
                  :class="getSlotClass(slot)"
                  @click="handleSlotSelect(slot)"
                >
                  <div class="slot-time">{{ slot.slotTime }}</div>
                  <div class="slot-capacity">
                    <span v-if="slot.status === 'SUSPENDED'">停诊</span>
                    <span v-else>{{ slot.remaining }}/{{ slot.maxPatients }}</span>
                  </div>
                </div>
                <div v-if="day.slots.length === 0" class="no-slot">无排班</div>
              </div>
            </div>
          </div>
        </el-card>
      </el-col>
    </el-row>

    <el-dialog v-model="showConfirmDialog" title="确认改约" width="480px" :close-on-click-modal="false">
      <div v-if="selectedSlot && currentAppointment" class="confirm-content">
        <div class="compare-box">
          <div class="compare-col old">
            <div class="col-title">原预约</div>
            <div class="col-date">{{ currentAppointment.appointmentDate }}</div>
            <div class="col-time">{{ currentAppointment.timeSlotDesc || ('时段' + currentAppointment.timeSlot) }}</div>
          </div>
          <div class="compare-arrow">
            <el-icon :size="28" color="#409eff"><Right /></el-icon>
          </div>
          <div class="compare-col new">
            <div class="col-title">新预约</div>
            <div class="col-date">{{ selectedSlot.scheduleDate }}</div>
            <div class="col-time">{{ selectedSlot.slotTime }} - {{ addMinute(selectedSlot.slotTime, 15) }}</div>
          </div>
        </div>

        <el-divider />

        <el-form :model="rescheduleForm" label-width="90px" size="default">
          <el-form-item label="改约原因" required>
            <el-input
              v-model="rescheduleForm.reason"
              type="textarea"
              :rows="2"
              placeholder="请输入改约原因"
            />
          </el-form-item>
        </el-form>

        <el-alert type="warning" :closable="false" show-icon style="margin-top: 10px;">
          <template #title>改约后原时段号源自动释放，新时段号源自动扣减</template>
        </el-alert>
      </div>
      <template #footer>
        <el-button @click="showConfirmDialog = false">取消</el-button>
        <el-button type="primary" :loading="rescheduling" @click="confirmReschedule">确认改约</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted, watch } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  ArrowLeft, User, Calendar, UserFilled, Refresh, Right
} from '@element-plus/icons-vue'
import type { Appointment, ScheduleSlot, DailySchedule } from '@/types'
import {
  getAppointmentDetail,
  getPatientAppointments,
  getDoctorDaySchedule,
  rescheduleAppointment
} from '@/api/schedule'
import { useUserStore } from '@/stores/user'

const router = useRouter()
const route = useRoute()
const userStore = useUserStore()

const loadingAppointment = ref(false)
const loadingList = ref(false)
const loadingSlots = ref(false)
const rescheduling = ref(false)

const searchForm = ref({ appointmentId: '' })
const patientIdForList = ref('')
const patientAppointments = ref<Appointment[]>([])
const currentAppointment = ref<Appointment | null>(null)
const currentDoctorId = ref<number | null>(null)
const currentDoctorName = ref('')
const weekSlots = ref<DailySchedule[]>([])

const selectedSlot = ref<ScheduleSlot | null>(null)
const showConfirmDialog = ref(false)
const rescheduleForm = ref({ reason: '' })

const aptTagType = (status: number): 'success' | 'warning' | 'danger' | 'info' => {
  // 假设: 1=待就诊,2=已完成,3=已取消,4=已过期
  const map: Record<number, 'success' | 'warning' | 'danger' | 'info'> = {
    0: 'info', 1: 'warning', 2: 'success', 3: 'danger', 4: 'info'
  }
  return map[status] || 'info'
}

const handleSearchAppointment = async () => {
  if (!searchForm.value.appointmentId) {
    ElMessage.warning('请输入预约ID')
    return
  }
  loadingAppointment.value = true
  try {
    const id = Number(searchForm.value.appointmentId)
    const res = await getAppointmentDetail(id)
    selectAppointment(res.data)
  } catch (e) {
    currentAppointment.value = null
    currentDoctorId.value = null
    weekSlots.value = []
    ElMessage.error('未找到该预约')
  } finally {
    loadingAppointment.value = false
  }
}

const loadPatientAppointments = async () => {
  if (!patientIdForList.value) {
    ElMessage.warning('请输入患者ID')
    return
  }
  loadingList.value = true
  try {
    const res = await getPatientAppointments(Number(patientIdForList.value))
    patientAppointments.value = res.data || []
  } catch (e) {
    patientAppointments.value = []
    ElMessage.error('加载患者预约列表失败')
  } finally {
    loadingList.value = false
  }
}

const selectAppointment = (apt: Appointment) => {
  currentAppointment.value = apt
  currentDoctorId.value = apt.doctorId
  currentDoctorName.value = apt.doctorName || ('医生' + apt.doctorId)
  selectedSlot.value = null
  refreshAvailableSlots()
}

const refreshAvailableSlots = async () => {
  if (!currentDoctorId.value) return
  loadingSlots.value = true
  weekSlots.value = []
  try {
    const dates = getNext7Days()
    const results = await Promise.all(dates.map(d => getDoctorDaySchedule(currentDoctorId.value!, d)))
    weekSlots.value = results.map((res, i) => ({
      date: dates[i],
      dayOfWeek: new Date(dates[i]).getDay(),
      slots: (res.data || []).filter(s => s.status !== 'SUSPENDED' && s.remaining > 0)
    }))
  } catch (e) {
    ElMessage.error('加载可用时段失败')
  } finally {
    loadingSlots.value = false
  }
}

const getSlotClass = (slot: ScheduleSlot) => {
  if (slot.status === 'SUSPENDED' || slot.remaining === 0) return 'full'
  return 'available'
}

const handleSlotSelect = (slot: ScheduleSlot) => {
  if (slot.status === 'SUSPENDED') {
    ElMessage.info('该时段已停诊')
    return
  }
  if (slot.remaining === 0) {
    ElMessage.warning('该时段号源已满')
    return
  }
  selectedSlot.value = slot
  rescheduleForm.value.reason = ''
  showConfirmDialog.value = true
}

const confirmReschedule = async () => {
  if (!currentAppointment.value || !selectedSlot.value) return
  if (!rescheduleForm.value.reason.trim()) {
    ElMessage.warning('请输入改约原因')
    return
  }
  rescheduling.value = true
  try {
    await ElMessageBox.confirm('确认要执行改约吗？', '改约确认', { type: 'warning' })
    await rescheduleAppointment({
      appointmentId: currentAppointment.value.id,
      newScheduleSlotId: selectedSlot.value.id,
      reason: rescheduleForm.value.reason,
      operatorId: userStore.userId || 1
    })
    ElMessage({ message: '改约成功！患者通知已自动记录', type: 'success', duration: 3000 })
    showConfirmDialog.value = false
    selectedSlot.value = null
    const updated = await getAppointmentDetail(currentAppointment.value.id)
    currentAppointment.value = updated.data
    refreshAvailableSlots()
  } catch (e: any) {
    if (e !== 'cancel') ElMessage.error('改约失败')
  } finally {
    rescheduling.value = false
  }
}

const goBack = () => router.back()

const getNext7Days = (): string[] => {
  const result: string[] = []
  const today = new Date()
  for (let i = 0; i < 7; i++) {
    const d = new Date(today)
    d.setDate(today.getDate() + i)
    result.push(formatDate(d))
  }
  return result
}
const formatDate = (d: Date): string => {
  const y = d.getFullYear()
  const m = String(d.getMonth() + 1).padStart(2, '0')
  const day = String(d.getDate()).padStart(2, '0')
  return `${y}-${m}-${day}`
}
const formatShortDate = (d: string): string => {
  const date = new Date(d)
  return `${date.getMonth() + 1}月${date.getDate()}日`
}
const getDayName = (d: string): string => {
  return ['周日', '周一', '周二', '周三', '周四', '周五', '周六'][new Date(d).getDay()]
}
const isToday = (d: string): boolean => formatDate(new Date()) === d
const addMinute = (t: string, minutes: number): string => {
  const [h, m] = t.split(':').map(Number)
  const d = new Date()
  d.setHours(h, m + minutes, 0, 0)
  return `${String(d.getHours()).padStart(2, '0')}:${String(d.getMinutes()).padStart(2, '0')}`
}

onMounted(() => {
  const idFromRoute = route.params.appointmentId as string
  if (idFromRoute) {
    searchForm.value.appointmentId = idFromRoute
    handleSearchAppointment()
  }
})

watch(() => route.params.appointmentId, (newId) => {
  if (newId && newId !== searchForm.value.appointmentId) {
    searchForm.value.appointmentId = String(newId)
    handleSearchAppointment()
  }
})
</script>

<style scoped>
.reschedule-view {
  padding: 20px;
  background: #f5f7fa;
  min-height: 100vh;
}
.page-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20px;
  padding: 16px 24px;
  background: #fff;
  border-radius: 8px;
}
.page-header h2 { margin: 0; color: #303133; }
.card-head { display: flex; justify-content: space-between; align-items: center; font-weight: 600; }
.card-head .title { color: #303133; }
.search-section { padding: 4px 0 12px 0; }
.appointment-list {
  max-height: 200px;
  overflow-y: auto;
  border: 1px solid #ebeef5;
  border-radius: 6px;
  background: #fafafa;
}
.apt-item {
  padding: 10px 12px;
  border-bottom: 1px solid #ebeef5;
  cursor: pointer;
  transition: all 0.2s;
}
.apt-item:last-child { border-bottom: none; }
.apt-item:hover { background: #ecf5ff; }
.apt-item.active { background: #ecf5ff; border-left: 3px solid #409eff; }
.apt-top {
  display: flex; justify-content: space-between; align-items: center; margin-bottom: 4px;
}
.apt-no { font-weight: 600; color: #303133; font-size: 13px; }
.apt-info { font-size: 12px; color: #606266; line-height: 1.8; }
.apt-info div { display: flex; align-items: center; gap: 4px; }
.hl { color: #409eff; font-weight: 600; }
.hl.time { font-size: 15px; }
.sub { font-size: 12px; color: #606266; margin-top: 3px; }
.sub.light { color: #909399; }
.day-label { color: #909399; font-size: 12px; }
.slots-card :deep(.el-card__header) { padding: 14px 20px; }
.week-grid {
  display: grid;
  grid-template-columns: repeat(7, 1fr);
  gap: 10px;
}
.day-col {
  border: 1px solid #ebeef5;
  border-radius: 8px;
  overflow: hidden;
  background: #fff;
  display: flex;
  flex-direction: column;
}
.day-header {
  padding: 10px 8px;
  text-align: center;
  background: #fafafa;
  border-bottom: 1px solid #ebeef5;
}
.day-header.today { background: #fef0f0; }
.day-header .day-name { font-weight: 600; color: #303133; font-size: 14px; }
.day-header .day-date { font-size: 12px; color: #909399; margin: 2px 0 4px 0; }
.day-slots {
  padding: 8px 6px;
  display: flex;
  flex-direction: column;
  gap: 5px;
  max-height: 540px;
  overflow-y: auto;
}
.slot-cell {
  padding: 6px 8px;
  border-radius: 5px;
  cursor: pointer;
  transition: all 0.18s;
  display: flex;
  justify-content: space-between;
  align-items: center;
  font-size: 12px;
  user-select: none;
  border: 1px solid transparent;
}
.slot-cell.available {
  background: #f0f9eb;
  border-color: #e1f3d8;
  color: #529b2e;
}
.slot-cell.available:hover {
  background: #67C23A;
  border-color: #67C23A;
  color: #fff;
  transform: translateY(-1px);
  box-shadow: 0 2px 6px rgba(103, 194, 58, 0.3);
}
.slot-cell.full {
  background: #fef0f0;
  border-color: #fde2e2;
  color: #f56c6c;
  cursor: not-allowed;
}
.slot-time { font-weight: 500; }
.no-slot {
  padding: 30px 8px;
  text-align: center;
  font-size: 12px;
  color: #c0c4cc;
}
.confirm-content .compare-box {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 16px;
  padding: 16px;
  background: #fafafa;
  border-radius: 8px;
}
.compare-col {
  flex: 1;
  max-width: 160px;
  padding: 14px 10px;
  border-radius: 8px;
  text-align: center;
  border: 2px solid;
}
.compare-col.old {
  border-color: #f56c6c;
  background: #fef0f0;
}
.compare-col.new {
  border-color: #67C23A;
  background: #f0f9eb;
}
.compare-col .col-title {
  font-size: 12px;
  font-weight: 600;
  margin-bottom: 8px;
}
.compare-col.old .col-title { color: #f56c6c; }
.compare-col.new .col-title { color: #67C23A; }
.compare-col .col-date {
  font-size: 14px;
  font-weight: 600;
  color: #303133;
  margin-bottom: 4px;
}
.compare-col .col-time {
  font-size: 13px;
  color: #606266;
}
</style>
