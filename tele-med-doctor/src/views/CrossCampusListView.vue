<template>
  <div class="list-container">
    <div class="page-header">
      <h2>跨院区会诊管理</h2>
      <div class="header-actions">
        <el-radio-group v-model="activeTab" @change="handleTabChange">
          <el-radio-button :value="'received'">收到的会诊</el-radio-button>
          <el-radio-button :value="'sent'">发起的会诊</el-radio-button>
          <el-radio-button :value="'mine'">我的会诊</el-radio-button>
        </el-radio-group>
        <el-select v-model="filterStatus" placeholder="状态筛选" clearable style="width: 140px; margin-left: 12px;" @change="loadList">
          <el-option label="待确认" :value="0" />
          <el-option label="进行中" :value="1" />
          <el-option label="已完成" :value="2" />
          <el-option label="已取消" :value="3" />
        </el-select>
      </div>
    </div>

    <el-card>
      <el-table :data="consultationList" stripe style="width: 100%" v-loading="loading">
        <el-table-column prop="consultationNo" label="会诊编号" width="200" />
        <el-table-column prop="patientName" label="患者姓名" width="100" />
        <el-table-column label="主诊医生" width="120">
          <template #default="{ row }">
            {{ row.primaryDoctorName || row.doctorName }}
          </template>
        </el-table-column>
        <el-table-column label="院区信息" min-width="260">
          <template #default="{ row }">
            <div class="campus-info">
              <div>
                <span class="label">源:</span>
                <el-tag size="small" type="info">{{ row.sourceCampusName }}</el-tag>
              </div>
              <el-icon style="color: #c0c4cc; margin: 4px 8px;"><Right /></el-icon>
              <div>
                <span class="label">目标:</span>
                <el-tag size="small" type="warning">{{ row.targetCampusName }}</el-tag>
              </div>
            </div>
          </template>
        </el-table-column>
        <el-table-column prop="campusTag" label="院区标签" width="220">
          <template #default="{ row }">
            <el-tag size="small" effect="plain" type="success">{{ row.campusTag }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="状态" width="100">
          <template #default="{ row }">
            <el-tag :type="statusTagType(row.status)">{{ row.statusText || statusLabel(row.status) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="expireTime" label="超时时间" width="170">
          <template #default="{ row }">
            <span v-if="row.expireTime && row.status === 0" :class="{ expired: isExpired(row.expireTime) }">
              {{ formatTime(row.expireTime) }}
            </span>
            <span v-else>-</span>
          </template>
        </el-table-column>
        <el-table-column prop="createTime" label="申请时间" width="170">
          <template #default="{ row }">
            {{ formatTime(row.createTime) }}
          </template>
        </el-table-column>
        <el-table-column label="操作" width="260" fixed="right">
          <template #default="{ row }">
            <el-button type="primary" size="small" link @click="handleView(row)">查看</el-button>
            <template v-if="row.status === 0">
              <el-button
                v-if="activeTab === 'received' || canConfirm(row)"
                type="success"
                size="small"
                link
                @click="handleConfirm(row)"
              >确认</el-button>
              <el-button
                v-if="activeTab === 'received' || canConfirm(row)"
                type="danger"
                size="small"
                link
                @click="handleReject(row)"
              >拒绝</el-button>
            </template>
            <el-button
              v-if="row.status === 0 || row.status === 1"
              type="warning"
              size="small"
              link
              @click="handleJoin(row)"
            >进入房间</el-button>
          </template>
        </el-table-column>
      </el-table>

      <el-empty v-if="!loading && consultationList.length === 0" description="暂无会诊记录" />
    </el-card>

    <el-dialog v-model="detailVisible" title="会诊详情" width="720px">
      <div v-if="detail" class="detail-content">
        <el-descriptions :column="2" border>
          <el-descriptions-item label="会诊编号">{{ detail.consultationNo }}</el-descriptions-item>
          <el-descriptions-item label="状态">
            <el-tag :type="statusTagType(detail.status)">{{ detail.statusText }}</el-tag>
          </el-descriptions-item>
          <el-descriptions-item label="患者姓名">{{ detail.patientName }}</el-descriptions-item>
          <el-descriptions-item label="主诊医生">{{ detail.primaryDoctorName }}</el-descriptions-item>
          <el-descriptions-item label="源院区">
            <el-tag type="info">{{ detail.sourceCampusName }}</el-tag>
          </el-descriptions-item>
          <el-descriptions-item label="目标院区">
            <el-tag type="warning">{{ detail.targetCampusName }}</el-tag>
          </el-descriptions-item>
          <el-descriptions-item label="院区标签">
            <el-tag effect="plain" type="success">{{ detail.campusTag }}</el-tag>
          </el-descriptions-item>
          <el-descriptions-item label="房间ID">{{ detail.roomId }}</el-descriptions-item>
          <el-descriptions-item label="开始时间" v-if="detail.startTime">
            {{ formatTime(detail.startTime) }}
          </el-descriptions-item>
          <el-descriptions-item label="结束时间" v-if="detail.endTime">
            {{ formatTime(detail.endTime) }}
          </el-descriptions-item>
          <el-descriptions-item label="申请时间" :span="2">
            {{ formatTime(detail.createTime) }}
          </el-descriptions-item>
          <el-descriptions-item label="超时时间" v-if="detail.expireTime" :span="2">
            <span :class="{ expired: isExpired(detail.expireTime) }">
              {{ formatTime(detail.expireTime) }}
            </span>
          </el-descriptions-item>
        </el-descriptions>

        <div v-if="detail.assistantDoctors && detail.assistantDoctors.length > 0" class="assistant-section">
          <h4>副诊医生</h4>
          <el-table :data="detail.assistantDoctors" size="small" stripe>
            <el-table-column prop="doctorName" label="姓名" />
            <el-table-column prop="title" label="职称" />
            <el-table-column prop="department" label="科室" />
            <el-table-column prop="campusName" label="院区" />
            <el-table-column label="参与状态" width="120">
              <template #default="{ row }">
                <el-tag size="small" :type="joinStatusType(row.joinStatus)">
                  {{ row.joinStatusText }}
                </el-tag>
              </template>
            </el-table-column>
          </el-table>
        </div>
      </div>
      <template #footer>
        <template v-if="detail && detail.status === 0 && (activeTab === 'received' || canConfirm(detail))">
          <el-button type="danger" @click="handleReject(detail)">拒绝</el-button>
          <el-button type="success" @click="handleConfirm(detail)">确认会诊</el-button>
        </template>
        <template v-else-if="detail && (detail.status === 0 || detail.status === 1)">
          <el-button type="primary" @click="handleJoin(detail)">进入会诊房间</el-button>
        </template>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { useUserStore } from '@/stores/user'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Right } from '@element-plus/icons-vue'
import {
  getTargetCampusConsultations,
  getSourceCampusConsultations,
  getDoctorCrossCampusConsultations,
  getCrossCampusConsultationDetail,
  confirmCrossCampusConsultation,
  rejectCrossCampusConsultation
} from '@/api/crossCampus'
import type { CrossCampusConsultation } from '@/types'

const router = useRouter()
const userStore = useUserStore()

const loading = ref(false)
const activeTab = ref('received')
const filterStatus = ref<number | undefined>(undefined)
const consultationList = ref<CrossCampusConsultation[]>([])
const detailVisible = ref(false)
const detail = ref<CrossCampusConsultation | null>(null)

const handleTabChange = () => {
  loadList()
}

const loadList = async () => {
  if (!userStore.doctorInfo && !userStore.campusId) return
  loading.value = true
  try {
    let res
    if (activeTab.value === 'received') {
      res = await getTargetCampusConsultations(
        userStore.campusId || userStore.doctorInfo?.campusId || 0,
        filterStatus.value
      )
    } else if (activeTab.value === 'sent') {
      res = await getSourceCampusConsultations(
        userStore.campusId || userStore.doctorInfo?.campusId || 0,
        filterStatus.value
      )
    } else {
      res = await getDoctorCrossCampusConsultations(
        userStore.doctorInfo?.id || 0,
        filterStatus.value
      )
    }
    consultationList.value = res.data || []
  } catch (e) {
    ElMessage.error('加载会诊列表失败')
  } finally {
    loading.value = false
  }
}

const canConfirm = (row: CrossCampusConsultation) => {
  return userStore.doctorInfo && row.doctorId === userStore.doctorInfo.id
}

const handleView = async (row: CrossCampusConsultation) => {
  try {
    const res = await getCrossCampusConsultationDetail(row.id)
    detail.value = res.data
    detailVisible.value = true
  } catch (e) {
    ElMessage.error('加载详情失败')
  }
}

const handleConfirm = async (row: CrossCampusConsultation) => {
  if (!userStore.doctorInfo) {
    ElMessage.warning('医生信息未找到')
    return
  }
  try {
    await ElMessageBox.confirm('确认接受该跨院区会诊邀请？', '确认', {
      type: 'success'
    })
    await confirmCrossCampusConsultation(row.id, userStore.doctorInfo.id)
    ElMessage.success('会诊已确认')
    loadList()
    if (detailVisible.value && detail.value?.id === row.id) {
      detailVisible.value = false
    }
  } catch (e: any) {
    if (e !== 'cancel') {
      ElMessage.error(e.message || '操作失败')
    }
  }
}

const handleReject = async (row: CrossCampusConsultation) => {
  if (!userStore.doctorInfo) return
  try {
    const { value } = await ElMessageBox.prompt('请输入拒绝原因（可选）', '拒绝会诊', {
      confirmButtonText: '确认拒绝',
      cancelButtonText: '取消',
      inputPlaceholder: '请输入拒绝原因',
      type: 'warning'
    })
    await rejectCrossCampusConsultation(row.id, userStore.doctorInfo.id, value)
    ElMessage.success('已拒绝')
    loadList()
    if (detailVisible.value && detail.value?.id === row.id) {
      detailVisible.value = false
    }
  } catch (e: any) {
    if (e !== 'cancel') {
      ElMessage.error(e.message || '操作失败')
    }
  }
}

const handleJoin = (row: CrossCampusConsultation) => {
  if (row.status === 0) {
    ElMessage.warning('请先确认会诊后再进入房间')
    return
  }
  router.push(`/consultation/${row.id}`)
}

const statusTagType = (status: number) => {
  const map: Record<number, string> = {
    0: 'warning',
    1: 'primary',
    2: 'success',
    3: 'info'
  }
  return map[status] || 'info'
}

const statusLabel = (status: number) => {
  const map: Record<number, string> = {
    0: '待确认',
    1: '进行中',
    2: '已完成',
    3: '已取消'
  }
  return map[status] || '未知'
}

const joinStatusType = (status: number) => {
  const map: Record<number, string> = {
    0: 'warning',
    1: 'success',
    2: 'danger',
    3: 'primary',
    4: 'info'
  }
  return map[status] || 'info'
}

const formatTime = (time: string) => {
  if (!time) return ''
  const d = new Date(time)
  return d.toLocaleString('zh-CN', {
    year: 'numeric',
    month: '2-digit',
    day: '2-digit',
    hour: '2-digit',
    minute: '2-digit'
  })
}

const isExpired = (time: string) => {
  if (!time) return false
  return new Date(time).getTime() < Date.now()
}

onMounted(() => {
  loadList()
})
</script>

<style scoped>
.list-container {
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

.campus-info {
  display: flex;
  align-items: center;
  gap: 4px;
}

.campus-info .label {
  color: #909399;
  font-size: 12px;
  margin-right: 4px;
}

.expired {
  color: #f56c6c;
  font-weight: 600;
}

.detail-content {
  padding: 8px 0;
}

.assistant-section {
  margin-top: 24px;
}

.assistant-section h4 {
  margin: 0 0 12px 0;
  color: #303133;
  font-size: 15px;
  font-weight: 600;
}
</style>
