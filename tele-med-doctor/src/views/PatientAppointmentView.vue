<template>
  <div class="appointment-book">
    <div class="page-header">
      <h2>患者预约挂号</h2>
      <div class="header-actions">
        <el-button @click="refreshSlots">
          <el-icon><Refresh /></el-icon>
          刷新号源
        </el-button>
      </div>
    </div>

    <el-card class="select-card">
      <el-row :gutter="20" align="middle">
        <el-col :span="10">
          <div class="label">选择医生</div>
          <el-select
            v-model="selectedDoctorId"
            placeholder="请选择就诊医生"
            filterable
            style="width: 100%"
            size="large"
            @change="handleDoctorChange"
          >
            <el-option
              v-for="doctor in doctorList"
              :key="doctor.id"
              :label="doctor.name || ('医生' + doctor.id)"
              :value="doctor.id"
            >
              <div class="doctor-option">
                <div class="opt-row">
                  <span class="doctor-name">{{ doctor.name || ('医生' + doctor.id) }}</span>
                  <el-tag size="small" type="primary">{{ doctor.title || '医师' }}</el-tag>
                </div>
                <div class="opt-sub">
                  {{ doctor.department || '-' }} · {{ doctor.hospitalName || '' }} {{ doctor.campusName || '' }}
                </div>
              </div>
            </el-option>
          </el-select>
        </el-col>
        <el-col :span="10">
          <div class="label">患者信息</div>
          <el-row :gutter="10">
            <el-col :span="12">
              <el-input v-model="patientInfo.name" placeholder="患者姓名" size="large" />
            </el-col>
            <el-col :span="12">
              <el-input v-model="patientInfo.idCard" placeholder="身份证号" size="large" />
            </el-col>
          </el-row>
        </el-col>
        <el-col :span="4">
          <el-button type="primary" size="large" :disabled="!selectedDoctorId" @click="refreshSlots" style="width:100%; margin-top: 20px;">
            <el-icon><Search /></el-icon>
            查询号源
          </el-button>
        </el-col>
      </el-row>
    </el-card>

    <el-card v-loading="loadingSlots" class="slots-card" shadow="never">
      <template #header>
        <div class="card-head">
          <span class="title">未来7天排班（15分钟粒度）</span>
          <div class="legend">
            <span class="lg-item"><span class="lg-box green"></span>可预约</span>
            <span class="lg-item"><span class="lg-box red"></span>已约满</span>
            <span class="lg-item"><span class="lg-box gray"></span>已停诊</span>
          </div>
        </div>
      </template>

      <div v-if="!selectedDoctorId" class="empty-center">
        <el-empty description="请先选择医生" :image-size="100" />
      </div>

      <div v-else-if="weekSlots.length === 0 && !loadingSlots" class="empty-center">
        <el-empty description="该医生未来7天暂无排班" :image-size="100" />
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
              :disabled="slot.status === 'SUSPENDED' || slot.remaining === 0"
              @click="handleSlotClick(slot)"
            >
              <div class="slot-time">{{ slot.slotTime }}</div>
              <div class="slot-capacity">
                <el-badge
                  v-if="slot.status === 'SUSPENDED'"
                  is-dot
                  type="info"
                  class="suspend-badge"
                />
                <span>{{ slot.remaining }}/{{ slot.maxPatients }}</span>
              </div>
            </div>
            <div v-if="day.slots.length === 0" class="no-slot">无排班</div>
          </div>
        </div>
      </div>
    </el-card>

    <el-dialog v-model="showBookDialog" title="确认预约" width="480px" :close-on-click-modal="false">
      <div v-if="bookingSlot" class="book-content">
        <el-descriptions :column="1" border size="default">
          <el-descriptions-item label="医生">
            <span class="highlight">{{ currentDoctor?.name || ('医生' + selectedDoctorId) }}</span>
            <el-tag size="small" type="primary" style="margin-left: 8px;">{{ currentDoctor?.title || '医师' }}</el-tag>
          </el-descriptions-item>
          <el-descriptions-item label="科室/医院">
            {{ currentDoctor?.department || '-' }} · {{ currentDoctor?.hospitalName || '-' }}
          </el-descriptions-item>
          <el-descriptions-item label="就诊日期">
            <span class="highlight">{{ bookingSlot.scheduleDate }}</span>
            <span class="day-label">({{ getDayName(bookingSlot.scheduleDate) }})</span>
          </el-descriptions-item>
          <el-descriptions-item label="就诊时段">
            <span class="highlight time">{{ bookingSlot.slotTime }} - {{ addMinute(bookingSlot.slotTime, 15) }}</span>
          </el-descriptions-item>
          <el-descriptions-item label="剩余号源">
            <el-tag :type="bookingSlot.remaining > 0 ? 'success' : 'danger'" size="small">
              {{ bookingSlot.remaining }} / {{ bookingSlot.maxPatients }}
            </el-tag>
          </el-descriptions-item>
        </el-descriptions>

        <el-divider />

        <el-form :model="bookForm" label-width="90px" size="default">
          <el-form-item label="患者姓名" required>
            <el-input v-model="bookForm.patientName" placeholder="请输入患者姓名" />
          </el-form-item>
          <el-form-item label="身份证号">
            <el-input v-model="bookForm.idCard" placeholder="请输入身份证号（选填）" />
          </el-form-item>
          <el-form-item label="备注说明">
            <el-input v-model="bookForm.description" type="textarea" :rows="2" placeholder="请输入症状或备注说明（选填）" />
          </el-form-item>
        </el-form>
      </div>
      <template #footer>
        <el-button @click="showBookDialog = false">取消</el-button>
        <el-button type="primary" :loading="booking" @click="confirmBook">确认预约</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { Refresh, Search } from '@element-plus/icons-vue'
import type { Doctor, ScheduleSlot, DailySchedule } from '@/types'
import { getDoctorDaySchedule, bookAppointment } from '@/api/schedule'
import { getCampusDoctors } from '@/api/crossCampus'
import { useUserStore } from '@/stores/user'

const userStore = useUserStore()
const loadingSlots = ref(false)
const booking = ref(false)

const doctorList = ref<Doctor[]>([])
const selectedDoctorId = ref<number | null>(null)
const patientInfo = ref({ name: '', idCard: '' })

const weekSlots = ref<DailySchedule[]>([])
const bookingSlot = ref<ScheduleSlot | null>(null)
const showBookDialog = ref(false)

const bookForm = ref({
  patientName: '',
  idCard: '',
  description: ''
})

const currentDoctor = computed(() =>
  doctorList.value.find(d => d.id === selectedDoctorId.value) || null
)

const handleDoctorChange = () => {
  weekSlots.value = []
  refreshSlots()
}

const refreshSlots = async () => {
  if (!selectedDoctorId.value) {
    ElMessage.info('请先选择医生')
    return
  }
  loadingSlots.value = true
  weekSlots.value = []
  try {
    const dates = getNext7Days()
    const promises = dates.map(d => getDoctorDaySchedule(selectedDoctorId.value!, d))
    const results = await Promise.all(promises)
    weekSlots.value = results.map((res, i) => ({
      date: dates[i],
      dayOfWeek: new Date(dates[i]).getDay(),
      slots: res.data || []
    }))
  } catch (e) {
    ElMessage.error('加载排班号源失败')
  } finally {
    loadingSlots.value = false
  }
}

const getSlotClass = (slot: ScheduleSlot) => {
  if (slot.status === 'SUSPENDED') return 'suspended'
  if (slot.remaining === 0) return 'full'
  return 'available'
}

const handleSlotClick = (slot: ScheduleSlot) => {
  if (slot.status === 'SUSPENDED') {
    ElMessage.info('该时段已停诊')
    return
  }
  if (slot.remaining === 0) {
    ElMessage.warning('该时段号源已约满')
    return
  }
  bookingSlot.value = slot
  bookForm.value.patientName = patientInfo.value.name || ''
  bookForm.value.idCard = patientInfo.value.idCard || ''
  bookForm.value.description = ''
  showBookDialog.value = true
}

const confirmBook = async () => {
  if (!bookForm.value.patientName.trim()) {
    ElMessage.warning('请输入患者姓名')
    return
  }
  if (!bookingSlot.value) return
  booking.value = true
  try {
    const patientId = Date.now() // 实际应从患者信息获取或通过接口创建
    const res = await bookAppointment(
      bookingSlot.value.id,
      patientId,
      bookForm.value.description
    )
    ElMessage({
      message: `预约成功！预约号：${res.data?.id || '已生成'}`,
      type: 'success',
      duration: 3000
    })
    patientInfo.value.name = bookForm.value.patientName
    patientInfo.value.idCard = bookForm.value.idCard
    showBookDialog.value = false
    refreshSlots()
  } catch (e) {
    ElMessage.error('预约失败，请稍后重试')
  } finally {
    booking.value = false
  }
}

const loadDoctors = async () => {
  try {
    const campusId = userStore.campusId
    if (campusId) {
      const res = await getCampusDoctors(campusId)
      doctorList.value = res.data || []
    }
  } catch (e) {
    doctorList.value = []
  }
}

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

const formatShortDate = (dateStr: string): string => {
  const d = new Date(dateStr)
  return `${d.getMonth() + 1}月${d.getDate()}日`
}

const getDayName = (dateStr: string): string => {
  const names = ['周日', '周一', '周二', '周三', '周四', '周五', '周六']
  return names[new Date(dateStr).getDay()]
}

const isToday = (dateStr: string): boolean => {
  const today = formatDate(new Date())
  return dateStr === today
}

const addMinute = (time: string, minutes: number): string => {
  const [h, m] = time.split(':').map(Number)
  const d = new Date()
  d.setHours(h, m + minutes, 0, 0)
  return `${String(d.getHours()).padStart(2, '0')}:${String(d.getMinutes()).padStart(2, '0')}`
}

onMounted(() => {
  loadDoctors()
})
</script>

<style scoped>
.appointment-book {
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
.select-card { margin-bottom: 16px; }
.select-card .label { font-size: 13px; color: #606266; margin-bottom: 6px; }
.doctor-option { display: flex; flex-direction: column; gap: 2px; }
.opt-row { display: flex; align-items: center; gap: 8px; }
.opt-row .doctor-name { font-weight: 500; }
.opt-sub { font-size: 12px; color: #909399; }
.slots-card :deep(.el-card__header) { padding: 14px 20px; }
.card-head {
  display: flex; justify-content: space-between; align-items: center;
  font-weight: 600;
}
.card-head .title { color: #303133; }
.legend { display: flex; gap: 14px; font-size: 12px; color: #606266; font-weight: normal; }
.lg-item { display: flex; align-items: center; gap: 5px; }
.lg-box { width: 14px; height: 14px; border-radius: 3px; display: inline-block; }
.lg-box.green { background: #67C23A; }
.lg-box.red { background: #F56C6C; }
.lg-box.gray { background: #909399; }
.empty-center {
  padding: 60px 20px;
  display: flex; justify-content: center;
}
.week-grid {
  display: grid;
  grid-template-columns: repeat(7, 1fr);
  gap: 10px;
}
.day-col {
  border: 1px solid #ebeef5;
  border-radius: 8px;
  overflow: hidden;
  display: flex;
  flex-direction: column;
  background: #fff;
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
.slot-cell.suspended {
  background: #f4f4f5;
  border-color: #e9e9eb;
  color: #909399;
  cursor: not-allowed;
  position: relative;
  text-decoration: line-through;
}
.slot-cell.suspended::after {
  content: '';
  position: absolute;
  top: 50%; left: 5%;
  width: 90%; height: 1px;
  background: rgba(144, 147, 153, 0.5);
  transform: rotate(-12deg);
}
.slot-time { font-weight: 500; }
.slot-capacity { display: flex; align-items: center; gap: 4px; }
.suspend-badge { margin-right: 0 !important; }
.no-slot {
  padding: 30px 8px;
  text-align: center;
  font-size: 12px;
  color: #c0c4cc;
}
.book-content .highlight {
  font-weight: 600;
  color: #409eff;
}
.book-content .time { font-size: 16px; }
.book-content .day-label {
  color: #909399;
  font-size: 12px;
  margin-left: 6px;
}
</style>
