<template>
  <div class="token-viewer-page">
    <div v-if="!tokenValidated" class="token-input-section">
      <div class="token-card">
        <div class="card-header">
          <el-icon :size="40" color="#409eff"><Picture /></el-icon>
          <h2>DICOM 影像查看</h2>
          <p>输入访问令牌以查看会诊影像</p>
        </div>
        <div class="card-body">
          <el-form @submit.prevent="validateAndLoad">
            <el-form-item>
              <el-input
                v-model="inputToken"
                placeholder="请输入30位访问令牌"
                size="large"
                clearable
                @keyup.enter="validateAndLoad"
              >
                <template #prefix>
                  <el-icon><Key /></el-icon>
                </template>
              </el-input>
            </el-form-item>
            <el-button
              type="primary"
              size="large"
              style="width: 100%"
              :loading="validating"
              @click="validateAndLoad"
            >
              查看影像
            </el-button>
          </el-form>
        </div>
      </div>
    </div>

    <div v-else class="token-viewer-section">
      <div class="viewer-header">
        <div class="header-left">
          <el-icon :size="20" color="#409eff"><Picture /></el-icon>
          <span class="header-title">DICOM 影像查看</span>
          <el-tag v-if="tokenInfo" type="info" size="small">
            会诊 #{{ tokenInfo.consultationId }}
          </el-tag>
        </div>
        <div class="header-right">
          <span class="expire-hint" v-if="expireHint">
            <el-icon><Clock /></el-icon>
            {{ expireHint }}
          </span>
          <el-button size="small" @click="resetToken">切换令牌</el-button>
        </div>
      </div>
      <div class="viewer-body">
        <DicomImageViewer
          ref="dicomViewerRef"
          :images="dicomImages"
          :access-token="activeToken"
          :consultation-id="tokenInfo?.consultationId || 0"
          :user-id="0"
          user-name="参会者"
        />
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRoute } from 'vue-router'
import { ElMessage } from 'element-plus'
import { Picture, Key, Clock } from '@element-plus/icons-vue'
import type { DicomImage, DicomToken } from '@/types'
import { validateDicomToken, getDicomImagesByToken } from '@/api/dicom'
import DicomImageViewer from '@/components/DicomImageViewer.vue'

const route = useRoute()

const inputToken = ref('')
const activeToken = ref('')
const tokenValidated = ref(false)
const validating = ref(false)
const tokenInfo = ref<DicomToken | null>(null)
const dicomImages = ref<DicomImage[]>([])
const dicomViewerRef = ref<InstanceType<typeof DicomImageViewer> | null>(null)

const expireHint = ref('')

function formatExpireTime(expireTime: string) {
  if (!expireTime) return ''
  try {
    const d = new Date(expireTime)
    return `有效期至 ${d.toLocaleString()}`
  } catch {
    return ''
  }
}

async function validateAndLoad() {
  const token = inputToken.value.trim()
  if (!token) {
    ElMessage.warning('请输入访问令牌')
    return
  }

  validating.value = true
  try {
    const validateRes = await validateDicomToken(token)
    tokenInfo.value = validateRes.data
    activeToken.value = token
    expireHint.value = formatExpireTime(validateRes.data.expireTime)

    const imagesRes = await getDicomImagesByToken(token)
    dicomImages.value = imagesRes.data || []

    tokenValidated.value = true
    ElMessage.success(`令牌验证成功，共 ${dicomImages.value.length} 张影像`)
  } catch {
    ElMessage.error('令牌无效或已过期，请检查后重试')
  } finally {
    validating.value = false
  }
}

function resetToken() {
  tokenValidated.value = false
  activeToken.value = ''
  tokenInfo.value = null
  dicomImages.value = []
  inputToken.value = ''
  expireHint.value = ''
}

onMounted(() => {
  const tokenParam = route.query.token as string
  if (tokenParam) {
    inputToken.value = tokenParam
    validateAndLoad()
  }
})
</script>

<style scoped>
.token-viewer-page {
  width: 100vw;
  height: 100vh;
  background: #0a0a1a;
  overflow: hidden;
}

.token-input-section {
  width: 100%;
  height: 100%;
  display: flex;
  align-items: center;
  justify-content: center;
  background: linear-gradient(135deg, #0a0a2a 0%, #1a1a3e 100%);
}

.token-card {
  width: 420px;
  background: #fff;
  border-radius: 16px;
  box-shadow: 0 20px 60px rgba(0, 0, 0, 0.5);
  overflow: hidden;
}

.card-header {
  background: linear-gradient(135deg, #409eff 0%, #337ecc 100%);
  color: #fff;
  padding: 32px 24px;
  text-align: center;
}

.card-header h2 {
  margin: 12px 0 4px;
  font-size: 20px;
  font-weight: 600;
}

.card-header p {
  font-size: 13px;
  opacity: 0.85;
  margin: 0;
}

.card-body {
  padding: 24px;
}

.token-viewer-section {
  display: flex;
  flex-direction: column;
  height: 100%;
}

.viewer-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 8px 16px;
  background: #1a1a2e;
  border-bottom: 1px solid #2a2a4a;
  flex-shrink: 0;
}

.header-left {
  display: flex;
  align-items: center;
  gap: 8px;
  color: #e0e0e0;
}

.header-title {
  font-size: 15px;
  font-weight: 600;
}

.header-right {
  display: flex;
  align-items: center;
  gap: 12px;
}

.expire-hint {
  display: flex;
  align-items: center;
  gap: 4px;
  font-size: 12px;
  color: #67c23a;
}

.viewer-body {
  flex: 1;
  min-height: 0;
}
</style>
