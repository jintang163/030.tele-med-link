<template>
  <div class="dicom-uploader">
    <el-upload
      class="upload-area"
      :auto-upload="false"
      :multiple="true"
      :on-change="handleFileChange"
      :on-remove="handleFileRemove"
      :file-list="fileList"
      accept=".dcm,DCM"
      drag
    >
      <div class="upload-inner">
        <el-icon class="upload-icon"><UploadFilled /></el-icon>
        <div class="upload-text">
          将DICOM文件 (.dcm) 拖到此处，或<em>点击选择文件</em>
        </div>
        <div class="upload-hint">支持批量上传，建议单文件不超过200MB</div>
      </div>
      <template #tip>
        <div class="upload-tip">
          当前待上传: {{ fileList.length }} 个文件
        </div>
      </template>
    </el-upload>

    <el-form v-if="fileList.length > 0" label-position="top" class="upload-form" size="small">
      <el-divider content-position="left">可选：影像元信息</el-divider>
      <el-row :gutter="16">
        <el-col :span="12">
          <el-form-item label="患者姓名">
            <el-input v-model="meta.patientName" placeholder="请输入患者姓名" />
          </el-form-item>
        </el-col>
        <el-col :span="12">
          <el-form-item label="检查类型 (Modality)">
            <el-select v-model="meta.modality" placeholder="选择CT/MRI等" clearable>
              <el-option label="CT" value="CT" />
              <el-option label="MR" value="MR" />
              <el-option label="MRI" value="MRI" />
              <el-option label="X-Ray" value="XR" />
              <el-option label="超声" value="US" />
              <el-option label="其他" value="OT" />
            </el-select>
          </el-form-item>
        </el-col>
      </el-row>
      <el-form-item label="检查描述">
        <el-input v-model="meta.studyDescription" placeholder="例如：头颅CT平扫" />
      </el-form-item>
    </el-form>

    <div v-if="fileList.length > 0" class="upload-actions">
      <el-button type="primary" :loading="uploading" @click="startUpload">
        <el-icon><Upload /></el-icon>
        开始上传 ({{ fileList.length }})
      </el-button>
      <el-button @click="clearFiles">清空列表</el-button>
    </div>

    <el-progress
      v-if="uploading && progressVisible"
      :percentage="progress"
      :status="progressStatus"
      class="upload-progress"
    />

    <div v-if="uploadedCount > 0 || failedCount > 0" class="upload-result">
      <el-tag v-if="uploadedCount > 0" type="success">
        成功: {{ uploadedCount }}
      </el-tag>
      <el-tag v-if="failedCount > 0" type="danger" style="margin-left: 8px;">
        失败: {{ failedCount }}
      </el-tag>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, computed } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { UploadFilled, Upload } from '@element-plus/icons-vue'
import type { UploadFile } from 'element-plus'
import { uploadDicom, type DicomUploadParams } from '@/api/dicom'
import type { DicomImage, SignalingMessage } from '@/types'
import type { SignalingWebSocket } from '@/utils/websocket'

const props = defineProps<{
  consultationId: number
  userId: number
  userName?: string
  signaling?: SignalingWebSocket | null
}>()

const emit = defineEmits<{
  (e: 'uploaded', images: DicomImage[]): void
}>()

interface UploadMeta {
  patientName: string
  modality: string
  studyDescription: string
}

const fileList = ref<UploadFile[]>([])
const meta = reactive<UploadMeta>({
  patientName: '',
  modality: '',
  studyDescription: ''
})
const uploading = ref(false)
const currentIndex = ref(0)
const uploadedCount = ref(0)
const failedCount = ref(0)
const uploadedImages = ref<DicomImage[]>([])

const progress = computed(() => {
  if (fileList.value.length === 0) return 0
  return Math.round((currentIndex.value / fileList.value.length) * 100)
})

const progressVisible = computed(() => uploading.value)
const progressStatus = computed<'success' | 'exception' | 'warning' | ''>(() => {
  if (!uploading.value && failedCount.value > 0) return 'exception'
  return ''
})

function handleFileChange(file: UploadFile, files: UploadFile[]) {
  fileList.value = files.filter(f => {
    const name = f.name?.toLowerCase() || ''
    if (!name.endsWith('.dcm')) {
      ElMessage.warning(`跳过非DICOM文件: ${f.name}`)
      return false
    }
    return true
  })
}

function handleFileRemove(_file: UploadFile, files: UploadFile[]) {
  fileList.value = files
}

function clearFiles() {
  fileList.value = []
  uploadedCount.value = 0
  failedCount.value = 0
  uploadedImages.value = []
}

async function startUpload() {
  if (fileList.value.length === 0) {
    ElMessage.warning('请先选择文件')
    return
  }

  if (uploadedCount.value > 0 || failedCount.value > 0) {
    try {
      await ElMessageBox.confirm(
        '之前已有上传记录，是否继续上传当前列表中未完成的文件？',
        '确认上传',
        { confirmButtonText: '继续上传', cancelButtonText: '取消', type: 'info' }
      )
    } catch {
      return
    }
  }

  uploading.value = true
  currentIndex.value = 0
  const startCount = uploadedCount.value

  try {
    for (let i = 0; i < fileList.value.length; i++) {
      currentIndex.value = i + 1
      const f = fileList.value[i]
      if (!f.raw) continue

      try {
        const params: DicomUploadParams = {
          consultationId: props.consultationId,
          uploaderId: props.userId,
          uploaderName: props.userName,
          patientName: meta.patientName || undefined,
          modality: meta.modality || undefined,
          studyDescription: meta.studyDescription || undefined,
          sliceIndex: i + 1
        }

        const res = await uploadDicom(f.raw, params)
        uploadedImages.value.push(res.data)
        uploadedCount.value++

        if (props.signaling) {
          const msg: SignalingMessage = {
            type: 'dicom-image-added',
            from: String(props.userId),
            to: '',
            roomId: String(props.consultationId),
            payload: { image: res.data },
            timestamp: Date.now()
          }
          props.signaling.send(msg)
        }
      } catch (e) {
        console.error('上传失败:', f.name, e)
        failedCount.value++
      }
    }

    const totalNew = uploadedCount.value - startCount
    if (totalNew > 0) {
      ElMessage.success(`成功上传 ${totalNew} 张DICOM影像`)
      emit('uploaded', uploadedImages.value.slice(-totalNew))
    }
    if (failedCount.value > 0) {
      ElMessage.error(`${failedCount.value} 张上传失败，请检查文件格式或网络`)
    }
  } finally {
    uploading.value = false
  }
}
</script>

<style scoped>
.dicom-uploader {
  padding: 12px 0;
}

.upload-area :deep(.el-upload-dragger) {
  background: #fafafa;
  border: 2px dashed #d9d9d9;
  border-radius: 8px;
  padding: 20px;
  transition: all 0.2s;
}

.upload-area :deep(.el-upload-dragger:hover) {
  border-color: #409eff;
  background: #ecf5ff;
}

.upload-inner {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 8px;
}

.upload-icon {
  font-size: 42px;
  color: #c0c4cc;
}

.upload-text {
  font-size: 14px;
  color: #606266;
}

.upload-text em {
  font-style: normal;
  color: #409eff;
  font-weight: 500;
}

.upload-hint {
  font-size: 12px;
  color: #909399;
}

.upload-tip {
  font-size: 12px;
  color: #909399;
  text-align: center;
  margin-top: 8px;
}

.upload-form {
  margin-top: 12px;
}

.upload-actions {
  margin-top: 12px;
  display: flex;
  gap: 8px;
}

.upload-progress {
  margin-top: 12px;
}

.upload-result {
  margin-top: 12px;
}
</style>
