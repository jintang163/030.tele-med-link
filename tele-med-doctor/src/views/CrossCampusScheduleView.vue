<template>
  <div class="schedule-container">
    <div class="page-header">
      <h2>跨院区专家排班</h2>
      <div class="header-actions">
        <el-select v-model="selectedCampusId" placeholder="选择院区" @change="handleCampusChange" style="width: 200px; margin-right: 12px;">
          <el-option v-for="campus in campusList" :key="campus.id" :label="campus.name" :value="campus.id" />
        </el-select>
        <el-input v-model="searchDepartment" placeholder="搜索科室" style="width: 180px; margin-right: 12px;" clearable />
        <el-button type="primary" @click="loadSchedules">查询排班</el-button>
      </div>
    </div>

    <el-row :gutter="20">
      <el-col :span="16">
        <el-card>
          <template #header>
            <div class="card-header">
              <span>排班日历 ({{ currentMonthLabel }}) - 15分钟粒度</span>
              <div>
                <el-radio-group v-model="viewMode" size="default">
                  <el-radio-button value="dayGridMonth">月视图</el-radio-button>
                  <el-radio-button value="timeGridWeek">周视图</el-radio-button>
                  <el-radio-button value="timeGridDay">日视图</el-radio-button>
                </el-radio-group>
              </div>
            </div>
          </template>
          <FullCalendar
            ref="calendarRef"
            :options="calendarOptions"
            class="calendar-wrapper"
          />
        </el-card>
      </el-col>

      <el-col :span="8">
        <el-card>
          <template #header>
            <span>医生排班列表</span>
          </template>
          <div class="doctor-list" v-loading="loading">
            <div
              v-for="schedule in scheduleList"
              :key="schedule.doctorId"
              class="doctor-item"
              @click="handleSelectDoctor(schedule)"
              :class="{ active: selectedDoctorId === schedule.doctorId }"
            >
              <div class="doctor-info">
                <div class="doctor-name">
                  {{ schedule.doctorName }}
                  <el-tag size="small" type="primary">{{ schedule.title }}</el-tag>
                </div>
                <div class="doctor-dept">{{ schedule.department }} · {{ schedule.campusName }}</div>
              </div>
              <div class="slot-status">
                <el-tag
                  v-for="slot in schedule.timeSlots.slice(0, 2)"
                  :key="slot.code"
                  size="small"
                  :type="slot.available ? 'success' : 'info'"
                  style="margin-right: 4px;"
                >
                  {{ slot.name }}: {{ slot.available ? '可约' : '已满' }}
                </el-tag>
              </div>
            </div>
            <el-empty v-if="!loading && scheduleList.length === 0" description="暂无排班信息" />
          </div>
        </el-card>

        <el-card style="margin-top: 16px;" v-if="selectedSchedule">
          <template #header>
            <span>医生时间槽详情</span>
          </template>
          <div class="time-slot-detail">
            <div class="detail-header">
              <strong>{{ selectedSchedule.doctorName }}</strong>
              <span class="dept">{{ selectedSchedule.title }} · {{ selectedSchedule.department }}</span>
            </div>
            <div class="detail-date">日期：{{ formatDate(selectedSchedule.scheduleDate) }}</div>
            <div class="slot-list">
              <div
                v-for="slot in selectedSchedule.timeSlots"
                :key="slot.code"
                class="slot-item"
                :class="{ disabled: !slot.available }"
              >
                <div class="slot-name">{{ slot.name }}</div>
                <div class="slot-time">{{ slot.startTime }} - {{ slot.endTime }}</div>
                <el-tag :type="slot.available ? 'success' : 'danger'" size="small">
                  {{ slot.available ? '可预约' : '已约满' }}
                </el-tag>
              </div>
            </div>
          </div>
        </el-card>
      </el-col>
    </el-row>

    <el-dialog v-model="showSlotDetailDialog" title="时段号源详情" width="440px">
      <div v-if="detailSlot">
        <el-descriptions :column="1" border size="default">
          <el-descriptions-item label="医生">
            <span class="hl">{{ detailSlot.doctorName }}</span>
          </el-descriptions-item>
          <el-descriptions-item label="科室/院区">
            {{ detailDoctor?.department || '' }} · {{ detailDoctor?.campusName || '' }}
          </el-descriptions-item>
          <el-descriptions-item label="日期">
            <span class="hl">{{ detailSlot.scheduleDate }}</span>
          </el-descriptions-item>
          <el-descriptions-item label="时段">
            <span class="hl">{{ detailSlot.slotTime }} - {{ addMinutes(detailSlot.slotTime, 15) }}</span>
          </el-descriptions-item>
          <el-descriptions-item label="号源状态">
            <el-tag
              :type="detailSlot.status === 'SUSPENDED' ? 'info' : (detailSlot.remaining > 0 ? 'success' : 'danger')"
            >
              {{ detailSlot.status === 'SUSPENDED' ? '已停诊' : (detailSlot.remaining > 0 ? '可预约' : '已约满') }}
            </el-tag>
          </el-descriptions-item>
          <el-descriptions-item label="剩余号源">
            <el-progress
              :percentage="Math.round(detailSlot.remaining / Math.max(detailSlot.maxPatients, 1) * 100)"
              :color="detailSlot.remaining > 0 ? '#67C23A' : '#F56C6C'"
              :stroke-width="14"
            />
            <div style="margin-top: 6px; font-size: 14px; color: #303133;">
              <span style="color: #409eff; font-weight: 600;">剩余 {{ detailSlot.remaining }}</span>
              <span style="color: #909399;"> / 共 {{ detailSlot.maxPatients }} 个号源</span>
            </div>
          </el-descriptions-item>
          <el-descriptions-item v-if="detailSlot.suspendReason" label="停诊原因">
            {{ detailSlot.suspendReason }}
          </el-descriptions-item>
        </el-descriptions>
      </div>
      <template #footer>
        <el-button @click="showSlotDetailDialog = false">关闭</el-button>
        <el-button
          v-if="detailSlot && detailSlot.status !== 'SUSPENDED' && detailSlot.remaining > 0"
          type="primary"
          @click="goToBook"
        >
          <el-icon><CalendarPlus /></el-icon>
          立即预约
        </el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted, watch } from 'vue'
import { useRouter } from 'vue-router'
import { useUserStore } from '@/stores/user'
import { ElMessage } from 'element-plus'
import { CalendarPlus } from '@element-plus/icons-vue'
import FullCalendar from '@fullcalendar/vue3'
import dayGridPlugin from '@fullcalendar/daygrid'
import timeGridPlugin from '@fullcalendar/timegrid'
import interactionPlugin from '@fullcalendar/interaction'
import type { CalendarOptions, EventApi } from '@fullcalendar/core'
import {
  getCampusList,
  getCampusSchedules,
  getDoctorSchedule,
  getDoctorTimeSlots
} from '@/api/crossCampus'
import { getDoctorDaySchedule } from '@/api/schedule'
import type { Campus, DoctorScheduleVO, ScheduleSlot, Doctor } from '@/types'

const router = useRouter()
const userStore = useUserStore()
const calendarRef = ref()
const loading = ref(false)
const campusList = ref<Campus[]>([])
const selectedCampusId = ref<number | null>(null)
const searchDepartment = ref('')
const scheduleList = ref<DoctorScheduleVO[]>([])
const selectedDoctorId = ref<number | null>(null)
const selectedSchedule = ref<DoctorScheduleVO | null>(null)
const viewMode = ref('timeGridWeek')
const currentDate = ref(new Date())

const showSlotDetailDialog = ref(false)
const detailSlot = ref<ScheduleSlot | null>(null)
const detailDoctor = ref<DoctorScheduleVO | null>(null)

const currentMonthLabel = computed(() => {
  const d = currentDate.value
  return `${d.getFullYear()}年${d.getMonth() + 1}月`
})

const calendarOptions = computed<CalendarOptions>(() => ({
  plugins: [dayGridPlugin, timeGridPlugin, interactionPlugin],
  initialView: viewMode.value,
  locale: 'zh-cn',
  headerToolbar: {
    left: 'prev,next today',
    center: 'title',
    right: ''
  },
  buttonText: {
    today: '今天',
    month: '月',
    week: '周',
    day: '日'
  },
  height: 600,
  eventClick: handleEventClick,
  dateClick: handleDateClick,
  datesSet: handleDatesSet,
  events: [] as any[],
  slotMinTime: '08:00:00',
  slotMaxTime: '18:00:00',
  slotDuration: '00:15:00',
  slotLabelInterval: '01:00:00',
  allDaySlot: false,
  eventTimeFormat: {
    hour: '2-digit',
    minute: '2-digit',
    meridiem: false
  },
  dayHeaderFormat: { weekday: 'short', month: 'numeric', day: 'numeric' }
}))

watch(viewMode, (newVal) => {
  if (calendarRef.value?.getApi()) {
    calendarRef.value.getApi().changeView(newVal)
  }
})

const loadCampusList = async () => {
  try {
    const res = await getCampusList(userStore.hospitalId || undefined)
    campusList.value = res.data || []
    if (campusList.value.length > 0) {
      selectedCampusId.value = userStore.campusId || campusList.value[0].id
    }
  } catch (e) {
    ElMessage.error('加载院区列表失败')
  }
}

const handleCampusChange = () => {
  loadSchedules()
}

const loadSchedules = async () => {
  if (!selectedCampusId.value) return
  loading.value = true
  try {
    const dateStr = formatDateStr(currentDate.value)
    const res = await getCampusSchedules(selectedCampusId.value, dateStr, searchDepartment.value || undefined)
    scheduleList.value = res.data || []
    await updateCalendarEvents()
  } catch (e) {
    ElMessage.error('加载排班失败')
  } finally {
    loading.value = false
  }
}

const updateCalendarEvents = async () => {
  const calendarApi = calendarRef.value?.getApi()
  if (!calendarApi) return

  calendarApi.removeAllEvents()
  const events: any[] = []

  for (const schedule of scheduleList.value) {
    const dateStr = schedule.scheduleDate
    try {
      const slotRes = await getDoctorDaySchedule(schedule.doctorId, dateStr)
      const slots: ScheduleSlot[] = slotRes.data || []
      for (const slot of slots) {
        const bg = getBgColor(slot)
        const endT = addMinutes(slot.slotTime, 15)
        events.push({
          id: `${slot.id}`,
          title: `${slot.doctorName || schedule.doctorName} 剩${slot.remaining}/${slot.maxPatients}`,
          start: `${slot.scheduleDate}T${slot.slotTime}:00`,
          end: `${slot.scheduleDate}T${endT}:00`,
          backgroundColor: bg,
          borderColor: bg,
          textColor: '#fff',
          extendedProps: {
            doctorId: schedule.doctorId,
            schedule,
            slot
          }
        })
      }
    } catch (e) {
      for (const slot of schedule.timeSlots) {
        const start = slot.startTime || (slot.code === 0 ? '08:30:00' : '14:00:00')
        const end = slot.endTime || (slot.code === 0 ? '12:00:00' : '17:30:00')
        events.push({
          title: `${schedule.doctorName}-${slot.name}${slot.available ? `(剩${slot.remainingCapacity})` : '(已满)'}`,
          start: `${dateStr}T${start}`,
          end: `${dateStr}T${end}`,
          backgroundColor: slot.available ? '#67C23A' : '#909399',
          borderColor: slot.available ? '#67C23A' : '#909399',
          extendedProps: {
            doctorId: schedule.doctorId,
            schedule,
            slot,
            fallback: true
          }
        })
      }
    }
  }
  calendarApi.addEventSource(events)
}

const getBgColor = (slot: ScheduleSlot): string => {
  if (slot.status === 'SUSPENDED') return '#909399'
  if (slot.status === 'SHIFTED') return '#E6A23C'
  return slot.remaining > 0 ? '#67C23A' : '#F56C6C'
}

const handleSelectDoctor = async (schedule: DoctorScheduleVO) => {
  selectedDoctorId.value = schedule.doctorId
  try {
    const res = await getDoctorSchedule(schedule.doctorId, schedule.scheduleDate)
    selectedSchedule.value = res.data
  } catch (e) {
    selectedSchedule.value = schedule
  }
}

const handleDatesSet = (arg: any) => {
  currentDate.value = new Date(arg.start)
}

const handleDateClick = async (arg: any) => {
  if (!selectedDoctorId.value) {
    ElMessage.info('请先在右侧选择医生')
    return
  }
  const dateStr = arg.dateStr
  try {
    const res = await getDoctorTimeSlots(selectedDoctorId.value, dateStr)
    ElMessage({
      message: `${dateStr} 可预约时间槽：${res.data.filter(s => s.available).map(s => s.name).join('、') || '无'}`,
      type: 'info',
      duration: 3000
    })
  } catch (e) {}
}

const handleEventClick = (arg: { event: EventApi }) => {
  const props = arg.event.extendedProps as any
  if (props.fallback) {
    if (props.schedule) {
      selectedDoctorId.value = props.doctorId
      selectedSchedule.value = props.schedule
    }
    return
  }
  if (props.slot) {
    detailSlot.value = props.slot
    detailDoctor.value = props.schedule
    showSlotDetailDialog.value = true
  } else if (props.schedule) {
    selectedDoctorId.value = props.doctorId
    selectedSchedule.value = props.schedule
  }
}

const goToBook = () => {
  router.push({ name: 'appointmentBook' })
}

const addMinutes = (time: string, minutes: number): string => {
  const [h, m] = time.split(':').map(Number)
  const d = new Date()
  d.setHours(h, m + minutes, 0, 0)
  return `${String(d.getHours()).padStart(2, '0')}:${String(d.getMinutes()).padStart(2, '0')}`
}

const formatDate = (dateStr: string) => {
  if (!dateStr) return ''
  const d = new Date(dateStr)
  return `${d.getFullYear()}年${d.getMonth() + 1}月${d.getDate()}日`
}

const formatDateStr = (d: Date) => {
  const year = d.getFullYear()
  const month = String(d.getMonth() + 1).padStart(2, '0')
  const day = String(d.getDate()).padStart(2, '0')
  return `${year}-${month}-${day}`
}

onMounted(() => {
  loadCampusList()
  setTimeout(() => loadSchedules(), 300)
})
</script>

<style scoped>
.schedule-container {
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

.page-header h2 {
  margin: 0;
  color: #303133;
}

.header-actions {
  display: flex;
  align-items: center;
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  font-weight: 600;
}

.calendar-wrapper {
  padding: 8px;
}

.doctor-list {
  max-height: 400px;
  overflow-y: auto;
}

.doctor-item {
  padding: 12px;
  border: 1px solid #ebeef5;
  border-radius: 6px;
  margin-bottom: 10px;
  cursor: pointer;
  transition: all 0.2s;
}

.doctor-item:hover {
  border-color: #409eff;
  background: #ecf5ff;
}

.doctor-item.active {
  border-color: #409eff;
  background: #ecf5ff;
  box-shadow: 0 2px 8px rgba(64, 158, 255, 0.15);
}

.doctor-info {
  margin-bottom: 8px;
}

.doctor-name {
  font-size: 15px;
  font-weight: 600;
  color: #303133;
  margin-bottom: 4px;
  display: flex;
  align-items: center;
  gap: 8px;
}

.doctor-dept {
  font-size: 13px;
  color: #909399;
}

.slot-status {
  display: flex;
  align-items: center;
}

.time-slot-detail {
  padding: 4px;
}

.detail-header {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 12px;
  font-size: 16px;
}

.detail-header .dept {
  font-size: 13px;
  color: #909399;
  font-weight: normal;
}

.detail-date {
  font-size: 14px;
  color: #606266;
  margin-bottom: 16px;
}

.slot-list {
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.slot-item {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 12px;
  border: 1px solid #e1f3d8;
  background: #f0f9eb;
  border-radius: 6px;
}

.slot-item.disabled {
  background: #f4f4f5;
  border-color: #e9e9eb;
}

.slot-name {
  font-weight: 600;
  color: #303133;
}

.slot-time {
  font-size: 13px;
  color: #909399;
  flex: 1;
  margin-left: 12px;
}
.hl {
  color: #409eff;
  font-weight: 600;
}
</style>
