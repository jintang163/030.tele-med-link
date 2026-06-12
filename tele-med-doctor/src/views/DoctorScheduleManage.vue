<template>
  <div class="schedule-manage">
    <div class="page-header">
      <h2>医生排班管理</h2>
      <div class="header-actions">
        <el-button type="primary" @click="handleRefresh">
          <el-icon><Refresh /></el-icon>
          刷新
        </el-button>
      </div>
    </div>

    <div class="main-layout">
      <div class="left-panel">
        <el-card class="doctor-card">
          <template #header>
            <div class="card-title">
              <el-icon><User /></el-icon>
              医生选择
            </div>
          </template>
          <el-select
            v-model="selectedDoctorId"
            placeholder="选择医生"
            filterable
            style="width: 100%"
            @change="handleDoctorChange"
          >
            <el-option
              v-for="doctor in doctorList"
              :key="doctor.id"
              :label="doctor.name || doctor.id"
              :value="doctor.id"
            >
              <div class="doctor-option">
                <span class="doctor-name">{{ doctor.name || ('医生' + doctor.id) }}</span>
                <span class="doctor-info">
                  {{ doctor.department || '' }} · {{ doctor.hospitalName || '' }}
                </span>
              </div>
            </el-option>
          </el-select>
          <div v-if="currentDoctor" class="current-doctor-info">
            <el-tag size="small" type="primary">{{ currentDoctor.title || '医师' }}</el-tag>
            <div class="info-line">{{ currentDoctor.department || '-' }}</div>
            <div class="info-line light">{{ currentDoctor.hospitalName || '-' }} {{ currentDoctor.campusName || '' }}</div>
          </div>
        </el-card>

        <el-card class="template-card">
          <template #header>
            <div class="card-title with-action">
              <span>
                <el-icon><Collection /></el-icon>
                排班模板
              </span>
              <el-button size="small" type="primary" link @click="showTemplateDialog = true">
                新建
              </el-button>
            </div>
          </template>
          <div class="template-list" v-loading="loadingTemplates">
            <div
              v-for="tpl in templateList"
              :key="tpl.id"
              class="template-item"
            >
              <div class="template-head">
                <span class="tpl-name">{{ tpl.templateName }}</span>
                <el-dropdown trigger="click" @command="(cmd) => handleTemplateCmd(cmd, tpl)">
                  <el-button size="small" text>
                    <el-icon><MoreFilled /></el-icon>
                  </el-button>
                  <template #dropdown>
                    <el-dropdown-menu>
                      <el-dropdown-item command="apply">应用到日期</el-dropdown-item>
                      <el-dropdown-item command="delete" divided>删除模板</el-dropdown-item>
                    </el-dropdown-menu>
                  </template>
                </el-dropdown>
              </div>
              <div class="tpl-meta">
                <el-tag size="small" type="info">{{ tpl.dayOfWeekLabel }}</el-tag>
                <span class="tpl-count">{{ tpl.slotTimes.length }}个时段</span>
              </div>
            </div>
            <el-empty v-if="!loadingTemplates && templateList.length === 0" description="暂无模板" :image-size="60" />
          </div>
        </el-card>
      </div>

      <div class="center-panel">
        <el-card>
          <template #header>
            <div class="card-title with-action">
              <span>排班日历（15分钟粒度）</span>
              <div class="legend">
                <span class="legend-item"><span class="dot green"></span>可约</span>
                <span class="legend-item"><span class="dot red"></span>已满</span>
                <span class="legend-item"><span class="dot gray"></span>停诊</span>
                <span class="legend-item"><span class="dot orange"></span>调班</span>
              </div>
            </div>
          </template>
          <FullCalendar
            ref="calendarRef"
            :options="calendarOptions"
            class="calendar-wrapper"
          />
        </el-card>
      </div>

      <div class="right-panel">
        <el-tabs v-model="activeTab" type="card" class="right-tabs">
          <el-tab-pane label="详情/操作" name="detail">
            <div v-if="selectedSlot" class="slot-detail">
              <div class="detail-section">
                <h4>当前时段</h4>
                <el-descriptions :column="1" size="small" border>
                  <el-descriptions-item label="日期">{{ selectedSlot.scheduleDate }}</el-descriptions-item>
                  <el-descriptions-item label="时间">{{ selectedSlot.slotTime }}</el-descriptions-item>
                  <el-descriptions-item label="状态">
                    <el-tag :type="slotStatusTagType">{{ slotStatusText }}</el-tag>
                  </el-descriptions-item>
                  <el-descriptions-item label="号源">
                    <span :class="{ full: selectedSlot.remaining === 0 }">
                      剩余 {{ selectedSlot.remaining }} / {{ selectedSlot.maxPatients }}
                    </span>
                  </el-descriptions-item>
                  <el-descriptions-item v-if="selectedSlot.suspendReason" label="停诊原因">
                    {{ selectedSlot.suspendReason }}
                  </el-descriptions-item>
                  <el-descriptions-item v-if="selectedSlot.shiftToDoctorId" label="调至">
                    {{ selectedSlot.shiftToDoctorName }} {{ selectedSlot.shiftToDate }} {{ selectedSlot.shiftToSlotTime }}
                  </el-descriptions-item>
                </el-descriptions>
              </div>

              <div class="detail-section">
                <el-space wrap>
                  <el-button v-if="selectedSlot.status === 'NORMAL'" type="warning" size="small" @click="showSuspendDialog = true">
                    停诊
                  </el-button>
                  <el-button v-if="selectedSlot.status === 'SUSPENDED'" type="success" size="small" @click="handleResume">
                    恢复
                  </el-button>
                  <el-button v-if="selectedSlot.status === 'NORMAL'" type="primary" size="small" @click="showShiftDialog = true">
                    调班
                  </el-button>
                  <el-button type="danger" size="small" @click="handleDeleteSlot">
                    删除
                  </el-button>
                </el-space>
              </div>
            </div>
            <div v-else-if="createMode" class="create-form">
              <div class="detail-section">
                <h4>创建排班</h4>
                <el-form :model="createForm" label-width="90px" size="small">
                  <el-form-item label="日期范围">
                    <el-date-picker
                      v-model="createForm.dateRange"
                      type="daterange"
                      range-separator="至"
                      start-placeholder="开始日期"
                      end-placeholder="结束日期"
                      style="width: 100%"
                      value-format="YYYY-MM-DD"
                    />
                  </el-form-item>
                  <el-form-item label="选择时段">
                    <div class="slot-checkboxes">
                      <el-checkbox
                        v-for="t in allSlotTimes"
                        :key="t"
                        :value="t"
                        v-model="createForm.slotTimes"
                        size="small"
                      >{{ t }}</el-checkbox>
                    </div>
                  </el-form-item>
                  <el-form-item label="号源数">
                    <el-input-number v-model="createForm.maxPatientsPerSlot" :min="1" :max="50" />
                  </el-form-item>
                  <el-form-item>
                    <el-button type="primary" @click="handleCreateSchedule" :loading="creating">
                      创建排班
                    </el-button>
                    <el-button @click="cancelCreate">取消</el-button>
                  </el-form-item>
                </el-form>
              </div>
            </div>
            <el-empty v-else description="点击日历空白创建排班，或点击已有时段查看详情" :image-size="80" />
          </el-tab-pane>

          <el-tab-pane label="批量复制" name="copy">
            <div class="detail-section">
              <h4>批量复制排班</h4>
              <el-form :model="copyForm" label-width="90px" size="small">
                <el-form-item label="源日期">
                  <el-date-picker
                    v-model="copyForm.sourceRange"
                    type="daterange"
                    range-separator="至"
                    start-placeholder="开始"
                    end-placeholder="结束"
                    style="width: 100%"
                    value-format="YYYY-MM-DD"
                  />
                </el-form-item>
                <el-form-item label="目标日期">
                  <el-date-picker
                    v-model="copyForm.targetDates"
                    type="dates"
                    placeholder="多选日期"
                    style="width: 100%"
                    value-format="YYYY-MM-DD"
                  />
                </el-form-item>
                <el-form-item>
                  <el-button type="primary" @click="handleBatchCopy" :loading="copying">
                    执行复制
                  </el-button>
                </el-form-item>
              </el-form>
            </div>
          </el-tab-pane>

          <el-tab-pane label="保存为模板" name="saveTpl">
            <div class="detail-section" v-if="weeklySchedule && weeklySchedule.length > 0">
              <h4>将当前周排班保存为模板</h4>
              <el-form :model="saveTplForm" label-width="90px" size="small">
                <el-form-item label="选择星期">
                  <el-select v-model="saveTplForm.dayOfWeek" placeholder="选择星期" style="width: 100%">
                    <el-option label="周一" :value="1" />
                    <el-option label="周二" :value="2" />
                    <el-option label="周三" :value="3" />
                    <el-option label="周四" :value="4" />
                    <el-option label="周五" :value="5" />
                    <el-option label="周六" :value="6" />
                    <el-option label="周日" :value="0" />
                  </el-select>
                </el-form-item>
                <el-form-item label="模板名称">
                  <el-input v-model="saveTplForm.templateName" placeholder="如：周一上午班" />
                </el-form-item>
                <el-form-item label="号源数">
                  <el-input-number v-model="saveTplForm.maxPatientsPerSlot" :min="1" :max="50" />
                </el-form-item>
                <el-form-item>
                  <el-button type="primary" @click="handleSaveTemplate" :loading="savingTpl">
                    保存模板
                  </el-button>
                </el-form-item>
              </el-form>
            </div>
            <el-empty v-else description="请先选择医生并加载排班" :image-size="80" />
          </el-tab-pane>
        </el-tabs>
      </div>
    </div>

    <el-dialog v-model="showSuspendDialog" title="确认停诊" width="420px">
      <el-form :model="suspendForm" label-width="100px" size="default">
        <el-form-item label="停诊原因" required>
          <el-input v-model="suspendForm.suspendReason" type="textarea" :rows="2" placeholder="请输入停诊原因" />
        </el-form-item>
        <el-form-item>
          <el-checkbox v-model="suspendForm.autoReschedule">自动改约已预约患者</el-checkbox>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="showSuspendDialog = false">取消</el-button>
        <el-button type="primary" @click="handleSuspend" :loading="suspending">确认停诊</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="showShiftDialog" title="调班设置" width="460px">
      <el-form :model="shiftForm" label-width="100px" size="default">
        <el-form-item label="目标医生" required>
          <el-select v-model="shiftForm.shiftToDoctorId" placeholder="选择医生" filterable style="width: 100%">
            <el-option
              v-for="doctor in doctorList"
              :key="doctor.id"
              :label="doctor.name || ('医生' + doctor.id)"
              :value="doctor.id"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="目标日期" required>
          <el-date-picker
            v-model="shiftForm.shiftToDate"
            type="date"
            placeholder="选择日期"
            style="width: 100%"
            value-format="YYYY-MM-DD"
          />
        </el-form-item>
        <el-form-item label="目标时段" required>
          <el-select v-model="shiftForm.shiftToSlotTime" placeholder="选择时段" style="width: 100%">
            <el-option v-for="t in allSlotTimes" :key="t" :label="t" :value="t" />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-checkbox v-model="shiftForm.autoReschedule">自动改约已预约患者</el-checkbox>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="showShiftDialog = false">取消</el-button>
        <el-button type="primary" @click="handleShift" :loading="shifting">确认调班</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="showTemplateDialog" title="新建排班模板" width="460px">
      <el-form :model="newTplForm" label-width="100px" size="default">
        <el-form-item label="模板名称" required>
          <el-input v-model="newTplForm.templateName" placeholder="如：标准上午班" />
        </el-form-item>
        <el-form-item label="适用星期" required>
          <el-select v-model="newTplForm.dayOfWeek" placeholder="选择星期" style="width: 100%">
            <el-option label="周一" :value="1" />
            <el-option label="周二" :value="2" />
            <el-option label="周三" :value="3" />
            <el-option label="周四" :value="4" />
            <el-option label="周五" :value="5" />
            <el-option label="周六" :value="6" />
            <el-option label="周日" :value="0" />
          </el-select>
        </el-form-item>
        <el-form-item label="包含时段" required>
          <div class="slot-checkboxes">
            <el-checkbox v-for="t in allSlotTimes" :key="t" :value="t" v-model="newTplForm.slotTimes">
              {{ t }}
            </el-checkbox>
          </div>
        </el-form-item>
        <el-form-item label="每时段号源" required>
          <el-input-number v-model="newTplForm.maxPatientsPerSlot" :min="1" :max="50" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="showTemplateDialog = false">取消</el-button>
        <el-button type="primary" @click="handleCreateTemplate" :loading="creatingTpl">创建模板</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="showApplyDialog" title="应用模板到日期" width="420px">
      <el-form label-width="100px" size="default">
        <el-form-item label="当前模板">
          <el-tag type="info">{{ applyingTemplate?.templateName }}</el-tag>
        </el-form-item>
        <el-form-item label="目标日期" required>
          <el-date-picker
            v-model="applyDates"
            type="dates"
            placeholder="选择要应用的日期"
            style="width: 100%"
            value-format="YYYY-MM-DD"
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="showApplyDialog = false">取消</el-button>
        <el-button type="primary" @click="handleApplyTemplate" :loading="applyingTpl">确认应用</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, watch, onMounted, shallowRef } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  Refresh, User, Collection, MoreFilled
} from '@element-plus/icons-vue'
import FullCalendar from '@fullcalendar/vue3'
import dayGridPlugin from '@fullcalendar/daygrid'
import timeGridPlugin from '@fullcalendar/timegrid'
import interactionPlugin from '@fullcalendar/interaction'
import type { CalendarOptions, EventApi, DateSelectArg, EventClickArg } from '@fullcalendar/core'
import type { Doctor, ScheduleSlot, WeeklySchedule, ScheduleTemplate, ScheduleTemplateCreateDTO } from '@/types'
import {
  getWeeklySchedule,
  getSlotTimes,
  createSchedule,
  batchCopySchedule,
  suspendSchedule,
  resumeSchedule,
  shiftSchedule,
  deleteSchedule,
  getScheduleTemplates,
  createScheduleTemplate,
  applyScheduleTemplate,
  deleteScheduleTemplate
} from '@/api/schedule'
import { getCampusDoctors } from '@/api/crossCampus'
import { useUserStore } from '@/stores/user'

const router = useRouter()
const userStore = useUserStore()
const calendarRef = shallowRef<InstanceType<typeof FullCalendar> | null>(null)

const loading = ref(false)
const loadingTemplates = ref(false)
const creating = ref(false)
const copying = ref(false)
const suspending = ref(false)
const shifting = ref(false)
const savingTpl = ref(false)
const creatingTpl = ref(false)
const applyingTpl = ref(false)

const doctorList = ref<Doctor[]>([])
const selectedDoctorId = ref<number | null>(null)
const weeklySchedule = ref<WeeklySchedule>([])
const templateList = ref<ScheduleTemplate[]>([])
const allSlotTimes = ref<string[]>([])

const selectedSlot = ref<ScheduleSlot | null>(null)
const createMode = ref(false)
const activeTab = ref('detail')
const selectionInfo = ref<{ startStr: string; endStr: string } | null>(null)

const showSuspendDialog = ref(false)
const showShiftDialog = ref(false)
const showTemplateDialog = ref(false)
const showApplyDialog = ref(false)
const applyingTemplate = ref<ScheduleTemplate | null>(null)
const applyDates = ref<string[]>([])

const createForm = ref({
  dateRange: [] as string[],
  slotTimes: [] as string[],
  maxPatientsPerSlot: 5
})

const copyForm = ref({
  sourceRange: [] as string[],
  targetDates: [] as string[]
})

const saveTplForm = ref({
  dayOfWeek: 1,
  templateName: '',
  maxPatientsPerSlot: 5
})

const suspendForm = ref({
  suspendReason: '',
  autoReschedule: true
})

const shiftForm = ref({
  shiftToDoctorId: null as number | null,
  shiftToDate: '',
  shiftToSlotTime: '',
  autoReschedule: true
})

const newTplForm = ref<ScheduleTemplateCreateDTO>({
  doctorId: 0,
  templateName: '',
  dayOfWeek: 1,
  slotTimes: [],
  maxPatientsPerSlot: 5
})

const currentDoctor = computed(() =>
  doctorList.value.find(d => d.id === selectedDoctorId.value) || null
)

const slotStatusText = computed(() => {
  if (!selectedSlot.value) return ''
  const map: Record<string, string> = { NORMAL: '正常', SUSPENDED: '停诊', SHIFTED: '调班' }
  return map[selectedSlot.value.status] || selectedSlot.value.status
})

const slotStatusTagType = computed(() => {
  if (!selectedSlot.value) return '' as const
  const map: Record<string, 'success' | 'danger' | 'info' | 'warning'> = {
    NORMAL: selectedSlot.value.remaining > 0 ? 'success' : 'danger',
    SUSPENDED: 'info',
    SHIFTED: 'warning'
  }
  return map[selectedSlot.value.status] || 'info'
})

const calendarOptions = computed<CalendarOptions>(() => ({
  plugins: [dayGridPlugin, timeGridPlugin, interactionPlugin],
  initialView: 'timeGridWeek',
  locale: 'zh-cn',
  headerToolbar: {
    left: 'prev,next today',
    center: 'title',
    right: 'timeGridWeek,timeGridDay'
  },
  buttonText: {
    today: '今天',
    week: '周',
    day: '日'
  },
  height: 620,
  slotDuration: '00:15:00',
  slotLabelInterval: '01:00:00',
  slotMinTime: '08:00:00',
  slotMaxTime: '18:00:00',
  allDaySlot: false,
  selectable: true,
  selectMirror: true,
  selectOverlap: false,
  eventOverlap: false,
  nowIndicator: true,
  scrollTime: '08:00:00',
  dayHeaderFormat: { weekday: 'short', month: 'numeric', day: 'numeric' },
  eventTimeFormat: {
    hour: '2-digit',
    minute: '2-digit',
    meridiem: false
  },
  select: handleSelect,
  eventClick: handleEventClick,
  datesSet: handleDatesSet,
  events: [] as any[]
}))

const handleSelect = (selectInfo: DateSelectArg) => {
  if (!selectedDoctorId.value) {
    ElMessage.info('请先选择医生')
    const calApi = calendarRef.value?.getApi()
    calApi?.unselect()
    return
  }
  selectedSlot.value = null
  createMode.value = true
  activeTab.value = 'detail'
  selectionInfo.value = { startStr: selectInfo.startStr, endStr: selectInfo.endStr }

  const start = new Date(selectInfo.startStr)
  const end = new Date(selectInfo.endStr)
  const date = formatDateStr(start)

  if (createForm.value.dateRange.length === 0) {
    createForm.value.dateRange = [date, date]
  }

  const times: string[] = []
  const cur = new Date(start)
  while (cur < end) {
    const hh = String(cur.getHours()).padStart(2, '0')
    const mm = String(cur.getMinutes()).padStart(2, '0')
    times.push(`${hh}:${mm}`)
    cur.setMinutes(cur.getMinutes() + 15)
  }
  const existing = new Set(createForm.value.slotTimes)
  times.forEach(t => existing.add(t))
  createForm.value.slotTimes = Array.from(existing).sort()
}

const handleEventClick = (clickInfo: EventClickArg) => {
  const props = clickInfo.event.extendedProps as any
  if (props.slot) {
    selectedSlot.value = props.slot
    createMode.value = false
    activeTab.value = 'detail'
  }
}

const handleDatesSet = () => {
  loadWeeklySchedule()
  loadTemplates()
}

const handleDoctorChange = () => {
  selectedSlot.value = null
  createMode.value = false
  loadWeeklySchedule()
  loadTemplates()
  newTplForm.value.doctorId = selectedDoctorId.value || 0
}

const handleRefresh = () => {
  loadDoctorList()
  loadSlotTimes()
  if (selectedDoctorId.value) {
    loadWeeklySchedule()
    loadTemplates()
  }
}

const cancelCreate = () => {
  createMode.value = false
  createForm.value = { dateRange: [], slotTimes: [], maxPatientsPerSlot: 5 }
  const calApi = calendarRef.value?.getApi()
  calApi?.unselect()
}

const loadDoctorList = async () => {
  try {
    const campusId = userStore.campusId
    if (campusId) {
      const res = await getCampusDoctors(campusId)
      doctorList.value = res.data || []
    } else {
      doctorList.value = []
    }
    if (doctorList.value.length > 0 && !selectedDoctorId.value) {
      selectedDoctorId.value = doctorList.value[0].id
      newTplForm.value.doctorId = selectedDoctorId.value
      setTimeout(() => {
        loadWeeklySchedule()
        loadTemplates()
      }, 100)
    }
  } catch (e) {
    ElMessage.error('加载医生列表失败')
  }
}

const loadSlotTimes = async () => {
  try {
    const res = await getSlotTimes()
    allSlotTimes.value = res.data || generateDefaultSlotTimes()
  } catch (e) {
    allSlotTimes.value = generateDefaultSlotTimes()
  }
}

const generateDefaultSlotTimes = (): string[] => {
  const times: string[] = []
  for (let h = 8; h < 18; h++) {
    for (let m = 0; m < 60; m += 15) {
      times.push(`${String(h).padStart(2, '0')}:${String(m).padStart(2, '0')}`)
    }
  }
  return times
}

const loadWeeklySchedule = async () => {
  if (!selectedDoctorId.value) return
  loading.value = true
  try {
    const calApi = calendarRef.value?.getApi()
    const viewStart = calApi?.view.activeStart
    const weekStart = viewStart ? formatDateStr(viewStart) : getCurrentWeekStart()
    const res = await getWeeklySchedule(selectedDoctorId.value, weekStart)
    weeklySchedule.value = res.data || []
    updateCalendarEvents()
  } catch (e) {
    ElMessage.error('加载排班失败')
  } finally {
    loading.value = false
  }
}

const updateCalendarEvents = () => {
  const calApi = calendarRef.value?.getApi()
  if (!calApi) return
  calApi.removeAllEvents()
  const events: any[] = []

  for (const daily of weeklySchedule.value) {
    for (const slot of daily.slots) {
      const bgColor = getSlotBgColor(slot)
      const end = addMinutesToTime(slot.slotTime, 15)
      events.push({
        id: String(slot.id),
        title: `剩余 ${slot.remaining}/${slot.maxPatients} ${slot.doctorName || ''}`,
        start: `${slot.scheduleDate}T${slot.slotTime}:00`,
        end: `${slot.scheduleDate}T${end}:00`,
        backgroundColor: bgColor,
        borderColor: bgColor,
        textColor: '#fff',
        display: 'block',
        extendedProps: { slot }
      })
    }
  }
  calApi.addEventSource(events)
}

const getSlotBgColor = (slot: ScheduleSlot): string => {
  if (slot.status === 'SUSPENDED') return '#909399'
  if (slot.status === 'SHIFTED') return '#E6A23C'
  return slot.remaining > 0 ? '#67C23A' : '#F56C6C'
}

const loadTemplates = async () => {
  if (!selectedDoctorId.value) return
  loadingTemplates.value = true
  try {
    const res = await getScheduleTemplates(selectedDoctorId.value)
    templateList.value = res.data || []
  } catch (e) {
    templateList.value = []
  } finally {
    loadingTemplates.value = false
  }
}

const handleCreateSchedule = async () => {
  if (!selectedDoctorId.value) return
  if (createForm.value.dateRange.length < 2) {
    ElMessage.warning('请选择日期范围')
    return
  }
  if (createForm.value.slotTimes.length === 0) {
    ElMessage.warning('请至少选择一个时段')
    return
  }
  creating.value = true
  try {
    const dates = getDatesInRange(createForm.value.dateRange[0], createForm.value.dateRange[1])
    let created: any[] = []
    for (const d of dates) {
      const res = await createSchedule({
        doctorId: selectedDoctorId.value!,
        scheduleDate: d,
        slotTimes: [...createForm.value.slotTimes],
        maxPatientsPerSlot: createForm.value.maxPatientsPerSlot,
        operatorId: userStore.userId || 1
      })
      created = created.concat(res.data || [])
    }
    ElMessage.success(`成功创建 ${created.length} 个排班时段`)
    cancelCreate()
    loadWeeklySchedule()
  } catch (e) {
    ElMessage.error('创建排班失败')
  } finally {
    creating.value = false
  }
}

const handleBatchCopy = async () => {
  if (!selectedDoctorId.value) return
  if (copyForm.value.sourceRange.length < 2) {
    ElMessage.warning('请选择源日期范围')
    return
  }
  if (copyForm.value.targetDates.length === 0) {
    ElMessage.warning('请选择目标日期')
    return
  }
  copying.value = true
  try {
    const res = await batchCopySchedule({
      doctorId: selectedDoctorId.value!,
      sourceStartDate: copyForm.value.sourceRange[0],
      sourceEndDate: copyForm.value.sourceRange[1],
      targetDates: copyForm.value.targetDates,
      operatorId: userStore.userId || 1
    })
    ElMessage.success(`成功复制 ${(res.data || []).length} 个排班时段`)
    loadWeeklySchedule()
    copyForm.value = { sourceRange: [], targetDates: [] }
  } catch (e) {
    ElMessage.error('批量复制失败')
  } finally {
    copying.value = false
  }
}

const handleSuspend = async () => {
  if (!selectedSlot.value) return
  if (!suspendForm.value.suspendReason.trim()) {
    ElMessage.warning('请输入停诊原因')
    return
  }
  suspending.value = true
  try {
    await suspendSchedule({
      scheduleId: selectedSlot.value.id,
      suspendReason: suspendForm.value.suspendReason,
      operatorId: userStore.userId || 1
    })
    ElMessage.success('已停诊')
    showSuspendDialog.value = false
    suspendForm.value = { suspendReason: '', autoReschedule: true }
    loadWeeklySchedule()
    selectedSlot.value = null
  } catch (e) {
    ElMessage.error('停诊失败')
  } finally {
    suspending.value = false
  }
}

const handleResume = async () => {
  if (!selectedSlot.value) return
  try {
    await ElMessageBox.confirm('确认恢复该时段的排班？', '恢复确认', { type: 'warning' })
    await resumeSchedule(selectedSlot.value.id, userStore.userId || 1)
    ElMessage.success('已恢复')
    loadWeeklySchedule()
    selectedSlot.value = null
  } catch (e: any) {
    if (e !== 'cancel') ElMessage.error('恢复失败')
  }
}

const handleShift = async () => {
  if (!selectedSlot.value) return
  if (!shiftForm.value.shiftToDoctorId || !shiftForm.value.shiftToDate || !shiftForm.value.shiftToSlotTime) {
    ElMessage.warning('请填写完整调班信息')
    return
  }
  shifting.value = true
  try {
    await shiftSchedule({
      scheduleId: selectedSlot.value.id,
      shiftToDoctorId: shiftForm.value.shiftToDoctorId!,
      shiftToDate: shiftForm.value.shiftToDate,
      shiftToSlotTime: shiftForm.value.shiftToSlotTime,
      operatorId: userStore.userId || 1
    })
    ElMessage.success('调班成功')
    showShiftDialog.value = false
    shiftForm.value = { shiftToDoctorId: null, shiftToDate: '', shiftToSlotTime: '', autoReschedule: true }
    loadWeeklySchedule()
    selectedSlot.value = null
  } catch (e) {
    ElMessage.error('调班失败')
  } finally {
    shifting.value = false
  }
}

const handleDeleteSlot = async () => {
  if (!selectedSlot.value) return
  try {
    await ElMessageBox.confirm('确认删除该排班时段？已预约的号源将被释放。', '删除确认', { type: 'error' })
    await deleteSchedule(selectedSlot.value.id, userStore.userId || 1)
    ElMessage.success('已删除')
    loadWeeklySchedule()
    selectedSlot.value = null
  } catch (e: any) {
    if (e !== 'cancel') ElMessage.error('删除失败')
  }
}

const handleCreateTemplate = async () => {
  if (!selectedDoctorId.value) return
  if (!newTplForm.value.templateName.trim()) {
    ElMessage.warning('请输入模板名称')
    return
  }
  if (newTplForm.value.slotTimes.length === 0) {
    ElMessage.warning('请选择时段')
    return
  }
  creatingTpl.value = true
  try {
    await createScheduleTemplate({ ...newTplForm.value, doctorId: selectedDoctorId.value! })
    ElMessage.success('模板创建成功')
    showTemplateDialog.value = false
    newTplForm.value = { doctorId: selectedDoctorId.value!, templateName: '', dayOfWeek: 1, slotTimes: [], maxPatientsPerSlot: 5 }
    loadTemplates()
  } catch (e) {
    ElMessage.error('创建模板失败')
  } finally {
    creatingTpl.value = false
  }
}

const handleSaveTemplate = async () => {
  if (!selectedDoctorId.value || weeklySchedule.value.length === 0) return
  if (!saveTplForm.value.templateName.trim()) {
    ElMessage.warning('请输入模板名称')
    return
  }
  const daySchedule = weeklySchedule.value.find(d => d.dayOfWeek === saveTplForm.value.dayOfWeek)
  if (!daySchedule || daySchedule.slots.length === 0) {
    ElMessage.warning('该星期没有可保存的排班')
    return
  }
  savingTpl.value = true
  try {
    await createScheduleTemplate({
      doctorId: selectedDoctorId.value!,
      templateName: saveTplForm.value.templateName,
      dayOfWeek: saveTplForm.value.dayOfWeek,
      slotTimes: daySchedule.slots.map(s => s.slotTime),
      maxPatientsPerSlot: saveTplForm.value.maxPatientsPerSlot
    })
    ElMessage.success('模板保存成功')
    loadTemplates()
    saveTplForm.value = { dayOfWeek: 1, templateName: '', maxPatientsPerSlot: 5 }
    activeTab.value = 'detail'
  } catch (e) {
    ElMessage.error('保存模板失败')
  } finally {
    savingTpl.value = false
  }
}

const handleTemplateCmd = (cmd: string, tpl: ScheduleTemplate) => {
  if (cmd === 'apply') {
    applyingTemplate.value = tpl
    applyDates.value = []
    showApplyDialog.value = true
  } else if (cmd === 'delete') {
    ElMessageBox.confirm(`确认删除模板「${tpl.templateName}」？`, '删除确认', { type: 'error' })
      .then(async () => {
        await deleteScheduleTemplate(tpl.id)
        ElMessage.success('模板已删除')
        loadTemplates()
      })
      .catch(() => {})
  }
}

const handleApplyTemplate = async () => {
  if (!applyingTemplate.value || !selectedDoctorId.value) return
  if (applyDates.value.length === 0) {
    ElMessage.warning('请选择目标日期')
    return
  }
  applyingTpl.value = true
  try {
    const res = await applyScheduleTemplate(
      applyingTemplate.value.id,
      selectedDoctorId.value!,
      applyDates.value,
      userStore.userId || 1
    )
    ElMessage.success(`成功应用 ${(res.data || []).length} 个时段`)
    showApplyDialog.value = false
    loadWeeklySchedule()
  } catch (e) {
    ElMessage.error('应用模板失败')
  } finally {
    applyingTpl.value = false
  }
}

const formatDateStr = (d: Date): string => {
  const year = d.getFullYear()
  const month = String(d.getMonth() + 1).padStart(2, '0')
  const day = String(d.getDate()).padStart(2, '0')
  return `${year}-${month}-${day}`
}

const getCurrentWeekStart = (): string => {
  const d = new Date()
  const day = d.getDay() === 0 ? 6 : d.getDay() - 1
  d.setDate(d.getDate() - day)
  return formatDateStr(d)
}

const getDatesInRange = (start: string, end: string): string[] => {
  const result: string[] = []
  const s = new Date(start)
  const e = new Date(end)
  for (let d = new Date(s); d <= e; d.setDate(d.getDate() + 1)) {
    result.push(formatDateStr(d))
  }
  return result
}

const addMinutesToTime = (time: string, minutes: number): string => {
  const [h, m] = time.split(':').map(Number)
  const d = new Date()
  d.setHours(h, m + minutes, 0, 0)
  return `${String(d.getHours()).padStart(2, '0')}:${String(d.getMinutes()).padStart(2, '0')}`
}

onMounted(() => {
  loadSlotTimes()
  loadDoctorList()
})
</script>

<style scoped>
.schedule-manage {
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
.main-layout {
  display: flex;
  gap: 16px;
}
.left-panel { width: 240px; flex-shrink: 0; display: flex; flex-direction: column; gap: 16px; }
.center-panel { flex: 1; min-width: 0; }
.right-panel { width: 320px; flex-shrink: 0; }
.card-title {
  display: flex; align-items: center; gap: 6px;
  font-weight: 600; color: #303133;
}
.card-title.with-action { justify-content: space-between; }
.doctor-card .doctor-option {
  display: flex; flex-direction: column; gap: 2px;
}
.doctor-option .doctor-name { font-weight: 500; }
.doctor-option .doctor-info { font-size: 12px; color: #909399; }
.current-doctor-info { margin-top: 12px; display: flex; flex-direction: column; gap: 6px; }
.current-doctor-info .info-line { font-size: 13px; color: #606266; }
.current-doctor-info .info-line.light { color: #909399; font-size: 12px; }
.template-card .template-list { max-height: 380px; overflow-y: auto; }
.template-item {
  padding: 10px 12px;
  border: 1px solid #ebeef5;
  border-radius: 6px;
  margin-bottom: 8px;
  cursor: pointer;
  transition: all 0.2s;
}
.template-item:hover { border-color: #409eff; background: #ecf5ff; }
.template-head {
  display: flex; justify-content: space-between; align-items: center; margin-bottom: 6px;
}
.tpl-name { font-weight: 500; color: #303133; }
.tpl-meta { display: flex; align-items: center; justify-content: space-between; font-size: 12px; color: #909399; }
.tpl-count { font-size: 12px; }
.legend { display: flex; gap: 14px; font-size: 12px; color: #606266; }
.legend-item { display: flex; align-items: center; gap: 5px; }
.legend .dot { width: 10px; height: 10px; border-radius: 50%; display: inline-block; }
.legend .dot.green { background: #67C23A; }
.legend .dot.red { background: #F56C6C; }
.legend .dot.gray { background: #909399; }
.legend .dot.orange { background: #E6A23C; }
.calendar-wrapper { padding: 8px; }
.right-tabs :deep(.el-tabs__content) { padding: 4px; }
.detail-section { margin-bottom: 16px; }
.detail-section h4 {
  margin: 0 0 12px 0; padding-bottom: 8px;
  border-bottom: 1px solid #ebeef5;
  font-size: 14px; color: #303133;
}
.slot-checkboxes {
  display: grid; grid-template-columns: repeat(3, 1fr); gap: 6px;
  max-height: 240px; overflow-y: auto;
}
.slot-checkboxes :deep(.el-checkbox) { margin-right: 0; }
.slot-detail .full { color: #F56C6C; font-weight: 600; }
</style>
