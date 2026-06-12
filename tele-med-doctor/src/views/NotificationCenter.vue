<template>
  <div class="notification-center">
    <div class="page-header">
      <h2>通知中心
        <el-badge
          v-if="unreadCount > 0"
          :value="unreadCount"
          :max="99"
          class="header-badge"
        />
      </h2>
      <div class="header-actions">
        <el-button
          type="primary"
          :disabled="unreadCount === 0"
          :loading="markingAll"
          @click="handleMarkAllRead"
        >
          <el-icon><Check /></el-icon>
          一键全部已读
        </el-button>
        <el-button @click="refreshNotifications">
          <el-icon><Refresh /></el-icon>
          刷新
        </el-button>
      </div>
    </div>

    <el-card class="main-card" shadow="never">
      <el-tabs v-model="activeTab" type="card" class="top-tabs" @tab-change="handleTabChange">
        <el-tab-pane label="全部" name="all" />
        <el-tab-pane
          :label="`未读${unreadCount > 0 ? ` (${unreadCount})` : ''}`"
          name="unread"
        />
        <el-tab-pane label="预约提醒" name="APPOINTMENT_REMINDER" />
        <el-tab-pane label="停诊通知" name="SCHEDULE_SUSPENDED" />
        <el-tab-pane label="改约通知" name="APPOINTMENT_RESCHEDULED" />
      </el-tabs>

      <el-row :gutter="20" class="content-row">
        <el-col :span="10">
          <div class="list-container" v-loading="loading">
            <div
              v-for="item in filteredNotifications"
              :key="item.id"
              class="notification-card"
              :class="[
                getCardClass(item),
                { unread: isUnread(item), active: selectedNotif?.id === item.id }
              ]"
              @click="handleSelectNotif(item)"
            >
              <div class="card-left">
                <div class="card-icon" :class="getIconClass(item)">
                  <el-icon :size="20">
                    <component :is="getIconComp(item)" />
                  </el-icon>
                </div>
                <div v-if="isUnread(item)" class="unread-dot"></div>
              </div>
              <div class="card-body">
                <div class="card-head">
                  <el-tag size="small" :type="getTagType(item)" effect="light">
                    {{ item.typeText }}
                  </el-tag>
                  <span class="card-time">{{ formatTime(item.createTime) }}</span>
                </div>
                <div class="card-title">{{ item.title }}</div>
                <div class="card-content-summary">
                  {{ truncate(item.content, 48) }}
                </div>
              </div>
            </div>
            <el-empty
              v-if="!loading && filteredNotifications.length === 0"
              description="暂无通知"
              :image-size="80"
              style="padding: 60px 0;"
            />
          </div>
        </el-col>

        <el-col :span="14">
          <div class="detail-panel" v-if="selectedNotif">
            <div class="detail-header" :class="getCardClass(selectedNotif)">
              <div class="detail-icon" :class="getIconClass(selectedNotif)">
                <el-icon :size="24">
                  <component :is="getIconComp(selectedNotif)" />
                </el-icon>
              </div>
              <div class="detail-meta">
                <div class="detail-title-row">
                  <h3>{{ selectedNotif.title }}</h3>
                  <el-tag size="small" :type="getTagType(selectedNotif)">
                    {{ selectedNotif.typeText }}
                  </el-tag>
                </div>
                <div class="detail-sub">
                  <span>
                    <el-icon><User /></el-icon>
                    {{ selectedNotif.patientName || ('患者' + selectedNotif.patientId) }}
                  </span>
                  <span class="divider">·</span>
                  <span>
                    <el-icon><Clock /></el-icon>
                    {{ formatFullTime(selectedNotif.createTime) }}
                  </span>
                  <span v-if="isUnread(selectedNotif)" class="status-unread">未读</span>
                  <span v-else class="status-read">已读</span>
                </div>
              </div>
            </div>

            <div class="detail-body">
              <div class="detail-content">
                {{ selectedNotif.content }}
              </div>

              <el-descriptions :column="1" size="small" border style="margin-top: 16px;">
                <el-descriptions-item v-if="selectedNotif.appointmentId" label="关联预约ID">
                  <el-link type="primary" :underline="false">
                    #{{ selectedNotif.appointmentId }}
                  </el-link>
                </el-descriptions-item>
                <el-descriptions-item v-if="selectedNotif.scheduleSlotId" label="关联排班ID">
                  #{{ selectedNotif.scheduleSlotId }}
                </el-descriptions-item>
                <el-descriptions-item label="通知类型">
                  {{ selectedNotif.typeText }} ({{ selectedNotif.type }})
                </el-descriptions-item>
                <el-descriptions-item label="状态">
                  <el-tag :type="isUnread(selectedNotif) ? 'danger' : 'success'" size="small">
                    {{ selectedNotif.statusText || (isUnread(selectedNotif) ? '未读' : '已读') }}
                  </el-tag>
                </el-descriptions-item>
              </el-descriptions>
            </div>

            <div class="detail-footer">
              <el-space>
                <el-button
                  v-if="selectedNotif.type === 'APPOINTMENT_RESCHEDULED' && selectedNotif.appointmentId"
                  type="primary"
                  @click="goToReschedule(selectedNotif.appointmentId)"
                >
                  <el-icon><Switch /></el-icon>
                  去改约
                </el-button>
                <el-button
                  v-if="selectedNotif.type === 'SCHEDULE_SUSPENDED'"
                  type="success"
                  @click="goToBook"
                >
                  <el-icon><CalendarPlus /></el-icon>
                  重新预约
                </el-button>
                <el-button
                  v-if="selectedNotif.type === 'APPOINTMENT_REMINDER' && selectedNotif.appointmentId"
                  type="warning"
                  @click="goToReschedule(selectedNotif.appointmentId)"
                >
                  查看详情
                </el-button>
                <el-button
                  v-if="isUnread(selectedNotif)"
                  :loading="markingOne"
                  @click="handleMarkOneRead(selectedNotif.id)"
                >
                  <el-icon><Check /></el-icon>
                  标记已读
                </el-button>
              </el-space>
            </div>
          </div>

          <div v-else class="empty-detail">
            <el-empty description="点击左侧通知查看详情" :image-size="120" />
          </div>
        </el-col>
      </el-row>
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import {
  Refresh, Check, User, Clock, Switch, CalendarPlus,
  Calendar, Warning, Bell
} from '@element-plus/icons-vue'
import type { PatientNotification, NotificationType } from '@/types'
import {
  getNotifications,
  getUnreadNotificationCount,
  markNotificationRead,
  markAllNotificationsRead
} from '@/api/schedule'
import { useUserStore } from '@/stores/user'

const router = useRouter()
const userStore = useUserStore()

const loading = ref(false)
const markingAll = ref(false)
const markingOne = ref(false)

const notifications = ref<PatientNotification[]>([])
const unreadCount = ref(0)
const selectedNotif = ref<PatientNotification | null>(null)
const activeTab = ref('all')

const patientId = computed(() => {
  return (userStore.userId as number) || 1
})

const filteredNotifications = computed(() => {
  let list = [...notifications.value]
  list.sort((a, b) => new Date(b.createTime).getTime() - new Date(a.createTime).getTime())
  if (activeTab.value === 'all') return list
  if (activeTab.value === 'unread') return list.filter(n => isUnread(n))
  return list.filter(n => n.type === activeTab.value)
})

const isUnread = (n: PatientNotification): boolean => n.status === 0 || n.statusText === '未读'

const getCardClass = (n: PatientNotification): string => {
  const map: Record<NotificationType, string> = {
    APPOINTMENT_REMINDER: 'type-reminder',
    SCHEDULE_SUSPENDED: 'type-suspended',
    APPOINTMENT_RESCHEDULED: 'type-rescheduled'
  }
  return map[n.type] || 'type-default'
}

const getIconClass = (n: PatientNotification): string => {
  const map: Record<NotificationType, string> = {
    APPOINTMENT_REMINDER: 'icon-blue',
    SCHEDULE_SUSPENDED: 'icon-red',
    APPOINTMENT_RESCHEDULED: 'icon-orange'
  }
  return map[n.type] || 'icon-default'
}

const getIconComp = (n: PatientNotification) => {
  const map: Record<NotificationType, any> = {
    APPOINTMENT_REMINDER: Bell,
    SCHEDULE_SUSPENDED: Warning,
    APPOINTMENT_RESCHEDULED: Switch
  }
  return map[n.type] || Bell
}

const getTagType = (n: PatientNotification): '' | 'success' | 'warning' | 'danger' | 'info' | 'primary' => {
  const map: Record<NotificationType, '' | 'success' | 'warning' | 'danger' | 'info' | 'primary'> = {
    APPOINTMENT_REMINDER: 'primary',
    SCHEDULE_SUSPENDED: 'danger',
    APPOINTMENT_RESCHEDULED: 'warning'
  }
  return map[n.type] || 'info'
}

const truncate = (s: string, len: number): string => {
  if (!s) return ''
  return s.length > len ? s.slice(0, len) + '...' : s
}

const formatTime = (t: string): string => {
  if (!t) return ''
  const d = new Date(t)
  const now = new Date()
  const diff = now.getTime() - d.getTime()
  if (diff < 60 * 1000) return '刚刚'
  if (diff < 60 * 60 * 1000) return `${Math.floor(diff / 60000)}分钟前`
  if (diff < 24 * 60 * 60 * 1000) return `${Math.floor(diff / 3600000)}小时前`
  if (diff < 7 * 24 * 60 * 60 * 1000) return `${Math.floor(diff / 86400000)}天前`
  return `${d.getMonth() + 1}月${d.getDate()}日`
}

const formatFullTime = (t: string): string => {
  if (!t) return ''
  const d = new Date(t)
  return `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, '0')}-${String(d.getDate()).padStart(2, '0')} ${String(d.getHours()).padStart(2, '0')}:${String(d.getMinutes()).padStart(2, '0')}`
}

const refreshNotifications = async () => {
  loading.value = true
  try {
    const res = await getNotifications(patientId.value)
    notifications.value = res.data || []
  } catch (e) {
    ElMessage.error('加载通知列表失败')
  } finally {
    loading.value = false
  }
  loadUnreadCount()
}

const loadUnreadCount = async () => {
  try {
    const res = await getUnreadNotificationCount(patientId.value)
    unreadCount.value = typeof res.data === 'number' ? res.data : 0
  } catch (e) {
    unreadCount.value = 0
  }
}

const handleSelectNotif = async (n: PatientNotification) => {
  selectedNotif.value = n
  if (isUnread(n)) {
    handleMarkOneRead(n.id, false)
  }
}

const handleMarkOneRead = async (id: number, showToast = true) => {
  markingOne.value = true
  try {
    const res = await markNotificationRead(id)
    const idx = notifications.value.findIndex(n => n.id === id)
    if (idx >= 0) {
      notifications.value[idx] = res.data || { ...notifications.value[idx], status: 1, statusText: '已读' }
    }
    if (selectedNotif.value?.id === id) {
      selectedNotif.value = res.data || { ...selectedNotif.value, status: 1, statusText: '已读' }
    }
    unreadCount.value = Math.max(0, unreadCount.value - 1)
    if (showToast) ElMessage.success('已标记为已读')
  } catch (e) {
    if (showToast) ElMessage.error('标记失败')
  } finally {
    markingOne.value = false
  }
}

const handleMarkAllRead = async () => {
  markingAll.value = true
  try {
    await markAllNotificationsRead(patientId.value)
    notifications.value = notifications.value.map(n => ({ ...n, status: 1, statusText: '已读' }))
    unreadCount.value = 0
    if (selectedNotif.value && isUnread(selectedNotif.value)) {
      selectedNotif.value = { ...selectedNotif.value, status: 1, statusText: '已读' }
    }
    ElMessage.success('已全部标记为已读')
  } catch (e) {
    ElMessage.error('操作失败')
  } finally {
    markingAll.value = false
  }
}

const handleTabChange = () => {
  selectedNotif.value = null
}

const goToReschedule = (appointmentId: number) => {
  router.push({ name: 'appointmentRescheduleWithId', params: { appointmentId: String(appointmentId) } })
}

const goToBook = () => {
  router.push({ name: 'appointmentBook' })
}

onMounted(() => {
  refreshNotifications()
})
</script>

<style scoped>
.notification-center {
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
  display: flex;
  align-items: center;
  gap: 10px;
}
.header-badge :deep(.el-badge__content) { top: 6px; }
.main-card { padding: 0; }
.main-card :deep(.el-card__body) { padding: 16px 20px 20px; }
.top-tabs :deep(.el-tabs__item) { height: 40px; line-height: 40px; }
.content-row { margin-top: 16px; }
.list-container {
  display: flex;
  flex-direction: column;
  gap: 10px;
  max-height: 640px;
  overflow-y: auto;
  padding-right: 6px;
}
.notification-card {
  display: flex;
  gap: 12px;
  padding: 12px;
  border-radius: 8px;
  background: #fff;
  border: 1px solid #ebeef5;
  cursor: pointer;
  position: relative;
  transition: all 0.2s;
}
.notification-card:hover {
  transform: translateY(-1px);
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.06);
}
.notification-card.active {
  border-color: #409eff;
  background: #ecf5ff;
}
.notification-card.unread {
  background: #fff;
}
.notification-card.type-reminder {
  border-left: 4px solid #409eff;
}
.notification-card.type-suspended {
  border-left: 4px solid #f56c6c;
}
.notification-card.type-rescheduled {
  border-left: 4px solid #e6a23c;
}
.card-left {
  position: relative;
  flex-shrink: 0;
}
.card-icon {
  width: 38px;
  height: 38px;
  border-radius: 8px;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #fff;
}
.card-icon.icon-blue { background: linear-gradient(135deg, #409eff, #66b1ff); }
.card-icon.icon-red { background: linear-gradient(135deg, #f56c6c, #f78989); }
.card-icon.icon-orange { background: linear-gradient(135deg, #e6a23c, #ebb563); }
.card-icon.icon-default { background: #909399; }
.unread-dot {
  position: absolute;
  top: -2px; right: -2px;
  width: 8px; height: 8px;
  background: #409eff;
  border-radius: 50%;
  border: 2px solid #fff;
}
.card-body { flex: 1; min-width: 0; }
.card-head {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 4px;
}
.card-time { font-size: 11px; color: #909399; }
.card-title {
  font-size: 14px;
  font-weight: 600;
  color: #303133;
  margin-bottom: 4px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.unread .card-title { color: #303133; }
.card-content-summary {
  font-size: 12px;
  color: #909399;
  line-height: 1.5;
}
.detail-panel {
  border: 1px solid #ebeef5;
  border-radius: 10px;
  overflow: hidden;
  background: #fff;
}
.detail-header {
  padding: 18px 20px;
  display: flex;
  gap: 14px;
  align-items: flex-start;
  border-bottom: 1px solid #f0f2f5;
}
.detail-header.type-reminder { background: #ecf5ff; }
.detail-header.type-suspended { background: #fef0f0; }
.detail-header.type-rescheduled { background: #fdf6ec; }
.detail-icon {
  width: 48px;
  height: 48px;
  border-radius: 10px;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #fff;
  flex-shrink: 0;
}
.detail-icon.icon-blue { background: linear-gradient(135deg, #409eff, #66b1ff); }
.detail-icon.icon-red { background: linear-gradient(135deg, #f56c6c, #f78989); }
.detail-icon.icon-orange { background: linear-gradient(135deg, #e6a23c, #ebb563); }
.detail-icon.icon-default { background: #909399; }
.detail-meta { flex: 1; }
.detail-title-row {
  display: flex;
  align-items: center;
  gap: 10px;
  margin-bottom: 6px;
}
.detail-title-row h3 {
  margin: 0;
  font-size: 16px;
  color: #303133;
  font-weight: 600;
}
.detail-sub {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 13px;
  color: #606266;
  flex-wrap: wrap;
}
.detail-sub .divider { color: #dcdfe6; }
.detail-sub :deep(.el-icon) { vertical-align: -1px; }
.status-unread {
  padding: 1px 8px;
  background: #f56c6c;
  color: #fff;
  border-radius: 10px;
  font-size: 11px;
  margin-left: 6px;
}
.status-read {
  padding: 1px 8px;
  background: #e1f3d8;
  color: #529b2e;
  border-radius: 10px;
  font-size: 11px;
  margin-left: 6px;
}
.detail-body { padding: 20px; }
.detail-content {
  padding: 16px;
  background: #fafbfc;
  border-radius: 8px;
  font-size: 14px;
  color: #303133;
  line-height: 1.7;
  border: 1px solid #ebeef5;
}
.detail-footer {
  padding: 16px 20px;
  border-top: 1px solid #f0f2f5;
  background: #fafafa;
}
.empty-detail {
  border: 1px solid #ebeef5;
  border-radius: 10px;
  background: #fff;
  min-height: 400px;
  display: flex;
  align-items: center;
  justify-content: center;
}
</style>
