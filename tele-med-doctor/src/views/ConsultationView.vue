<template>
  <div class="consultation-container">
    <div class="main-content">
      <div class="content-tabs">
        <div
          class="tab-item"
          :class="{ active: activeMainTab === 'video' }"
          @click="activeMainTab = 'video'"
        >
          <el-icon><VideoCamera /></el-icon>
          视频问诊
        </div>
        <div
          class="tab-item"
          :class="{ active: activeMainTab === 'dicom' }"
          @click="activeMainTab = 'dicom'"
        >
          <el-icon><Picture /></el-icon>
          DICOM 影像
          <el-badge v-if="dicomImages.length > 0" :value="dicomImages.length" class="tab-badge" />
        </div>
      </div>

      <div class="content-area">
        <div v-show="activeMainTab === 'video'" class="video-area">
          <video ref="remoteVideoRef" class="remote-video" autoplay playsinline />
          <video ref="localVideoRef" class="local-video" autoplay playsinline muted />
          <div class="control-bar">
            <el-button :type="audioEnabled ? 'primary' : 'danger'" circle @click="toggleAudio">
              <el-icon><Microphone v-if="audioEnabled" /><Mute v-else /></el-icon>
            </el-button>
            <el-button :type="videoEnabled ? 'primary' : 'danger'" circle @click="toggleVideo">
              <el-icon><VideoCamera v-if="videoEnabled" /><VideoPause v-else /></el-icon>
            </el-button>
            <el-button type="warning" @click="activeMainTab = 'dicom'" :disabled="dicomImages.length === 0">
              <el-icon><Picture /></el-icon>
              查看影像 ({{ dicomImages.length }})
            </el-button>
            <el-button type="danger" @click="handleEndConsultation" :disabled="consultationEnded">结束问诊</el-button>
          </div>
        </div>

        <div v-show="activeMainTab === 'dicom'" class="dicom-area">
          <DicomImageViewer
            v-if="dicomImages.length > 0 || accessToken"
            ref="dicomViewerRef"
            :images="dicomImages"
            :access-token="accessToken"
            :consultation-id="consultationId"
            :user-id="userIdNum"
            :user-name="userName"
            @annotation-sync="handleAnnotationSync"
            @viewport-sync="handleViewportSync"
            @image-loaded="handleImageLoaded"
          />
          <el-empty
            v-else
            description="暂无DICOM影像，请先上传"
            :image-size="120"
            class="dicom-empty"
          >
            <el-button type="primary" @click="activeSideTab = 'upload'">
              <el-icon><Upload /></el-icon>
              上传DICOM文件
            </el-button>
          </el-empty>
        </div>
      </div>
    </div>

    <div class="side-panel">
      <el-tabs v-model="activeSideTab" class="side-tabs">
        <el-tab-pane name="chat">
          <template #label>
            <el-icon><ChatDotRound /></el-icon>
            <span>聊天</span>
          </template>
          <div class="chat-area">
            <div class="chat-messages" ref="chatMessagesRef">
              <div
                v-for="msg in chatMessages"
                :key="msg.id || msg.createTime"
                :class="['chat-message', msg.senderType === 'DOCTOR' ? 'self' : 'other']"
              >
                <div class="message-sender">{{ msg.senderName }}</div>
                <div class="message-content">{{ msg.content }}</div>
                <div class="message-time">{{ formatTime(msg.createTime) }}</div>
              </div>
            </div>
            <div class="chat-input">
              <el-input
                v-model="chatInput"
                placeholder="输入消息..."
                @keyup.enter="sendChat"
                :disabled="consultationEnded"
              >
                <template #append>
                  <el-button @click="sendChat" :disabled="consultationEnded || !chatInput">发送</el-button>
                </template>
              </el-input>
            </div>
          </div>
        </el-tab-pane>

        <el-tab-pane name="upload">
          <template #label>
            <el-icon><Upload /></el-icon>
            <span>上传影像</span>
          </template>
          <div class="upload-area">
            <div class="upload-header">
              <div class="upload-title">上传DICOM医学影像</div>
              <div class="upload-desc">上传CT/MRI等DICOM文件 (.dcm) 供会诊查看</div>
            </div>
            <DicomUploader
              :consultation-id="consultationId"
              :user-id="userIdNum"
              :user-name="userName"
              :signaling="signaling"
              @uploaded="handleImagesUploaded"
            />
          </div>
        </el-tab-pane>

        <el-tab-pane name="images">
          <template #label>
            <el-icon><Picture /></el-icon>
            <span>影像列表</span>
            <el-badge v-if="dicomImages.length > 0" :value="dicomImages.length" class="pane-badge" />
          </template>
          <div class="images-list">
            <div v-if="dicomImages.length === 0" class="images-empty">
              <el-empty description="暂无影像" :image-size="80" />
            </div>
            <div
              v-for="(img, idx) in dicomImages"
              :key="img.id"
              class="image-item"
            >
              <div class="image-thumb">
                <el-icon :size="24"><Picture /></el-icon>
                <div class="image-idx">{{ idx + 1 }}</div>
              </div>
              <div class="image-info">
                <div class="image-name" :title="img.fileName">{{ img.fileName }}</div>
                <div class="image-meta">
                  <el-tag v-if="img.modality" size="small" type="primary">{{ img.modality }}</el-tag>
                  <span class="image-size" v-if="img.fileSize">
                    {{ formatFileSize(img.fileSize) }}
                  </span>
                </div>
                <div class="image-uploader">
                  上传: {{ img.uploaderName || '-' }} · {{ formatTime(img.uploadTime) }}
                </div>
              </div>
              <div class="image-actions">
                <el-tooltip content="在查看器中打开">
                  <el-button size="small" @click="openImageInViewer(img)">
                    <el-icon><View /></el-icon>
                  </el-button>
                </el-tooltip>
                <el-tooltip v-if="img.uploaderId === userIdNum" content="删除">
                  <el-button size="small" type="danger" @click="handleDeleteImage(img.id)">
                    <el-icon><Delete /></el-icon>
                  </el-button>
                </el-tooltip>
              </div>
            </div>

            <div v-if="dicomImages.length > 0 && !accessToken" class="token-section">
              <el-divider>会诊共享令牌</el-divider>
              <div class="token-desc">
                生成临时访问令牌（30分钟有效），分享给其他参会者查看影像
              </div>
              <el-button
                type="success"
                :loading="generatingToken"
                style="width: 100%"
                @click="generateShareToken"
              >
                <el-icon><Key /></el-icon>
                生成访问令牌
              </el-button>
              <div v-if="accessToken" class="token-display">
                <el-input v-model="accessToken" readonly size="small">
                  <template #append>
                    <el-button @click="copyToken">
                      <el-icon><CopyDocument /></el-icon>
                    </el-button>
                  </template>
                </el-input>
                <div class="token-hint" v-if="tokenExpireTime">
                  有效期至: {{ tokenExpireTime }}
                </div>
              </div>
            </div>
          </div>
        </el-tab-pane>

        <el-tab-pane name="conclusion" v-if="showConclusion || consultationEnded">
          <template #label>
            <el-icon><Document /></el-icon>
            <span>问诊结论</span>
          </template>
          <div class="conclusion-panel">
            <div class="conclusion-header">问诊结论</div>
            <el-input
              v-model="conclusionContent"
              type="textarea"
              :rows="8"
              placeholder="请输入问诊结论..."
            />
            <el-button
              type="primary"
              style="margin-top: 12px; width: 100%"
              @click="handleSubmitConclusion"
              :loading="submittingConclusion"
            >
              提交结论
            </el-button>
          </div>
        </el-tab-pane>
      </el-tabs>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, onUnmounted, nextTick, computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useUserStore } from '@/stores/user'
import { getChatMessages, finishConsultation } from '@/api/consultation'
import {
  getConsultationDicomImages,
  generateConsultationDicomToken,
  deleteDicomImage
} from '@/api/dicom'
import { SignalingWebSocket } from '@/utils/websocket'
import { JanusVideoRoom } from '@/utils/janus'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  Microphone, Mute, VideoCamera, VideoPause, Picture, Upload,
  ChatDotRound, View, Delete, Key, CopyDocument, Document
} from '@element-plus/icons-vue'
import type {
  ChatMessage, SignalingMessage, DicomImage,
  DicomAnnotationSyncPayload, DicomViewportSyncPayload
} from '@/types'
import DicomImageViewer from '@/components/DicomImageViewer.vue'
import DicomUploader from '@/components/DicomUploader.vue'

const route = useRoute()
const router = useRouter()
const userStore = useUserStore()

const consultationId = Number(route.params.id)
const remoteVideoRef = ref<HTMLVideoElement>()
const localVideoRef = ref<HTMLVideoElement>()
const chatMessagesRef = ref<HTMLDivElement>()
const dicomViewerRef = ref<InstanceType<typeof DicomImageViewer> | null>(null)

const activeMainTab = ref<'video' | 'dicom'>('video')
const activeSideTab = ref('chat')

const chatMessages = ref<ChatMessage[]>([])
const chatInput = ref('')
const audioEnabled = ref(true)
const videoEnabled = ref(true)
const consultationEnded = ref(false)
const showConclusion = ref(false)
const conclusionContent = ref('')
const submittingConclusion = ref(false)

const dicomImages = ref<DicomImage[]>([])
const accessToken = ref('')
const tokenExpireTime = ref('')
const generatingToken = ref(false)

let videoRoom: JanusVideoRoom | null = null
let signaling: SignalingWebSocket | null = null
let chatTimer: ReturnType<typeof setInterval> | null = null
let dicomRefreshTimer: ReturnType<typeof setInterval> | null = null

const userId = String(userStore.user?.id || '')
const userIdNum = Number(userId) || 0
const userName = computed(() => userStore.user?.name || userStore.user?.username || '医生')

const formatTime = (time: string) => {
  if (!time) return ''
  return new Date(time).toLocaleTimeString()
}

const formatFileSize = (bytes: number) => {
  if (bytes < 1024) return bytes + ' B'
  if (bytes < 1024 * 1024) return (bytes / 1024).toFixed(1) + ' KB'
  return (bytes / (1024 * 1024)).toFixed(2) + ' MB'
}

const scrollToBottom = () => {
  nextTick(() => {
    if (chatMessagesRef.value) {
      chatMessagesRef.value.scrollTop = chatMessagesRef.value.scrollHeight
    }
  })
}

const fetchChatMessages = async () => {
  try {
    const res = await getChatMessages(consultationId)
    chatMessages.value = res.data || []
    scrollToBottom()
  } catch {
    // ignore
  }
}

const fetchDicomImages = async () => {
  try {
    const res = await getConsultationDicomImages(consultationId)
    dicomImages.value = res.data || []
  } catch {
    // ignore
  }
}

const sendChat = () => {
  if (!chatInput.value.trim() || !signaling) return
  const message: SignalingMessage = {
    type: 'chat',
    from: userId,
    to: '',
    roomId: String(consultationId),
    payload: { content: chatInput.value, senderName: userName.value },
    timestamp: Date.now()
  }
  signaling.send(message)
  chatMessages.value.push({
    consultationId,
    senderId: userIdNum,
    senderName: userName.value,
    senderType: 'DOCTOR',
    content: chatInput.value,
    createTime: new Date().toISOString()
  })
  chatInput.value = ''
  scrollToBottom()
}

const toggleAudio = () => {
  audioEnabled.value = videoRoom?.toggleAudio() ?? !audioEnabled.value
}

const toggleVideo = () => {
  videoEnabled.value = videoRoom?.toggleVideo() ?? !videoEnabled.value
}

const handleEndConsultation = async () => {
  try {
    await finishConsultation(consultationId)
    consultationEnded.value = true
    showConclusion.value = true
    activeSideTab.value = 'conclusion'
    ElMessage.success('问诊已结束，请填写结论')
  } catch {
    ElMessage.error('结束问诊失败')
  }
}

const handleSubmitConclusion = async () => {
  if (!conclusionContent.value.trim()) {
    ElMessage.warning('请输入问诊结论')
    return
  }
  submittingConclusion.value = true
  try {
    await finishConsultation(consultationId, conclusionContent.value)
    ElMessage.success('结论已提交')
    showConclusion.value = false
    setTimeout(() => router.back(), 1500)
  } catch {
    ElMessage.error('提交结论失败')
  } finally {
    submittingConclusion.value = false
  }
}

const handleImagesUploaded = (newImages: DicomImage[]) => {
  dicomImages.value = [...dicomImages.value, ...newImages]
  if (activeMainTab.value !== 'dicom') {
    ElMessage({
      type: 'success',
      message: `已上传 ${newImages.length} 张影像，是否切换到影像查看？`,
      showClose: true,
      duration: 0,
      onClose: () => {},
    })
  }
}

const openImageInViewer = (_img: DicomImage) => {
  activeMainTab.value = 'dicom'
}

const handleDeleteImage = async (imageId: number) => {
  try {
    await ElMessageBox.confirm('确认删除该影像？删除后无法恢复。', '删除确认', {
      type: 'warning',
      confirmButtonText: '删除',
      cancelButtonText: '取消'
    })
    await deleteDicomImage(imageId, userIdNum)
    dicomImages.value = dicomImages.value.filter(i => i.id !== imageId)
    ElMessage.success('删除成功')
  } catch {
    // cancelled
  }
}

const generateShareToken = async () => {
  generatingToken.value = true
  try {
    const res = await generateConsultationDicomToken(consultationId, userIdNum, userName.value)
    accessToken.value = res.data.token
    tokenExpireTime.value = res.data.expireTime
    ElMessage.success('令牌生成成功，有效期30分钟')
  } catch {
    ElMessage.error('生成令牌失败')
  } finally {
    generatingToken.value = false
  }
}

const copyToken = async () => {
  try {
    await navigator.clipboard.writeText(accessToken.value)
    ElMessage.success('令牌已复制到剪贴板')
  } catch {
    ElMessage.error('复制失败，请手动复制')
  }
}

const handleAnnotationSync = (payload: DicomAnnotationSyncPayload) => {
  if (!signaling) return
  const msg: SignalingMessage = {
    type: 'dicom-annotation',
    from: userId,
    to: '',
    roomId: String(consultationId),
    payload,
    timestamp: Date.now()
  }
  signaling.send(msg)
}

const handleViewportSync = (payload: DicomViewportSyncPayload) => {
  if (!signaling) return
  const msg: SignalingMessage = {
    type: 'dicom-viewport',
    from: userId,
    to: '',
    roomId: String(consultationId),
    payload,
    timestamp: Date.now()
  }
  signaling.send(msg)
}

const handleImageLoaded = (img: DicomImage) => {
  console.log('影像已加载:', img.fileName)
}

const handleSignalingMessage = (message: SignalingMessage) => {
  if (message.type === 'chat') {
    const senderId = Number(message.from)
    if (senderId !== userIdNum) {
      chatMessages.value.push({
        consultationId,
        senderId,
        senderName: message.payload?.senderName || '对方',
        senderType: 'PATIENT',
        content: message.payload?.content || '',
        createTime: new Date().toISOString()
      })
      scrollToBottom()
    }
    return
  }

  if (message.type === 'dicom-image-added') {
    const image = message.payload?.image as DicomImage
    if (image && image.id && !dicomImages.value.find(i => i.id === image.id)) {
      dicomImages.value.push(image)
      if (Number(message.from) !== userIdNum) {
        ElMessage.info(`${image.uploaderName || '对方'} 上传了新的DICOM影像`)
      }
    }
    return
  }

  if (message.type === 'dicom-annotation') {
    const payload = message.payload as DicomAnnotationSyncPayload
    if (payload && payload.operatorId !== userIdNum) {
      dicomViewerRef.value?.handleRemoteAnnotationSync(payload)
    }
    return
  }

  if (message.type === 'dicom-viewport') {
    const payload = message.payload as DicomViewportSyncPayload
    if (payload && payload.operatorId !== userIdNum) {
      dicomViewerRef.value?.handleRemoteViewportSync(payload)
    }
    return
  }

  if (message.type === 'offer' || message.type === 'answer' || message.type === 'ice-candidate') {
    videoRoom?.handleSignalingMessage(message, userId)
  }
}

onMounted(async () => {
  if (!localVideoRef.value || !remoteVideoRef.value) return

  videoRoom = new JanusVideoRoom(
    String(consultationId),
    localVideoRef.value,
    remoteVideoRef.value
  )

  signaling = new SignalingWebSocket(userId)
  signaling.onMessage(handleSignalingMessage)
  signaling.connect()

  videoRoom.setSignaling(signaling)
  await videoRoom.joinRoom(userId)

  fetchChatMessages()
  fetchDicomImages()
  chatTimer = setInterval(fetchChatMessages, 5000)
  dicomRefreshTimer = setInterval(fetchDicomImages, 15000)
})

onUnmounted(() => {
  videoRoom?.leaveRoom()
  videoRoom = null
  signaling?.disconnect()
  signaling = null
  if (chatTimer) {
    clearInterval(chatTimer)
    chatTimer = null
  }
  if (dicomRefreshTimer) {
    clearInterval(dicomRefreshTimer)
    dicomRefreshTimer = null
  }
})
</script>

<style scoped>
.consultation-container {
  display: flex;
  height: 100vh;
  background: #1a1a2e;
  overflow: hidden;
}

.main-content {
  flex: 1;
  display: flex;
  flex-direction: column;
  min-width: 0;
}

.content-tabs {
  display: flex;
  background: #2a2a4a;
  border-bottom: 1px solid #3a3a5a;
  padding: 0 16px;
  gap: 8px;
  flex-shrink: 0;
}

.tab-item {
  display: flex;
  align-items: center;
  gap: 6px;
  padding: 10px 16px;
  color: #a0a0c0;
  cursor: pointer;
  border-bottom: 2px solid transparent;
  font-size: 14px;
  transition: all 0.2s;
  position: relative;
}

.tab-item:hover {
  color: #e0e0ff;
  background: rgba(64, 158, 255, 0.1);
}

.tab-item.active {
  color: #fff;
  border-bottom-color: #409eff;
  background: rgba(64, 158, 255, 0.15);
}

.tab-badge {
  margin-left: 4px;
}

.content-area {
  flex: 1;
  position: relative;
  min-height: 0;
  background: #0a0a1a;
}

.video-area {
  width: 100%;
  height: 100%;
  position: relative;
  display: flex;
  align-items: center;
  justify-content: center;
}

.remote-video {
  width: 100%;
  height: 100%;
  object-fit: cover;
  background: #000;
}

.local-video {
  position: absolute;
  bottom: 80px;
  right: 20px;
  width: 240px;
  height: 180px;
  border-radius: 8px;
  border: 2px solid #409eff;
  object-fit: cover;
  z-index: 10;
  background: #000;
}

.control-bar {
  position: absolute;
  bottom: 20px;
  left: 50%;
  transform: translateX(-50%);
  display: flex;
  gap: 12px;
  padding: 10px 20px;
  background: rgba(0, 0, 0, 0.75);
  border-radius: 12px;
  z-index: 20;
}

.dicom-area {
  width: 100%;
  height: 100%;
  position: relative;
}

.dicom-empty {
  position: absolute;
  inset: 0;
  display: flex;
  align-items: center;
  justify-content: center;
}

.side-panel {
  width: 400px;
  display: flex;
  flex-direction: column;
  background: #fff;
  border-left: 1px solid #e4e7ed;
  flex-shrink: 0;
}

.side-tabs {
  flex: 1;
  display: flex;
  flex-direction: column;
  min-height: 0;
}

.side-tabs :deep(.el-tabs__content) {
  flex: 1;
  min-height: 0;
  overflow: hidden;
}

.side-tabs :deep(.el-tab-pane) {
  height: 100%;
  display: flex;
  flex-direction: column;
}

.pane-badge {
  margin-left: 2px;
}

.chat-area {
  flex: 1;
  display: flex;
  flex-direction: column;
  min-height: 0;
}

.chat-messages {
  flex: 1;
  overflow-y: auto;
  padding: 12px 16px;
  background: #fafafa;
}

.chat-message {
  margin-bottom: 12px;
}

.chat-message.self {
  text-align: right;
}

.chat-message.other {
  text-align: left;
}

.message-sender {
  font-size: 12px;
  color: #909399;
  margin-bottom: 4px;
}

.message-content {
  display: inline-block;
  padding: 8px 12px;
  border-radius: 8px;
  font-size: 14px;
  max-width: 80%;
  word-break: break-all;
}

.chat-message.self .message-content {
  background: #409eff;
  color: #fff;
}

.chat-message.other .message-content {
  background: #f4f4f5;
  color: #303133;
}

.message-time {
  font-size: 11px;
  color: #c0c4cc;
  margin-top: 4px;
}

.chat-input {
  padding: 12px 16px;
  border-top: 1px solid #e4e7ed;
  background: #fff;
}

.upload-area {
  flex: 1;
  overflow-y: auto;
  padding: 16px;
}

.upload-header {
  margin-bottom: 8px;
}

.upload-title {
  font-size: 16px;
  font-weight: 600;
  color: #303133;
  margin-bottom: 4px;
}

.upload-desc {
  font-size: 12px;
  color: #909399;
}

.images-list {
  flex: 1;
  overflow-y: auto;
  padding: 12px;
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.images-empty {
  flex: 1;
  display: flex;
  align-items: center;
  justify-content: center;
}

.image-item {
  display: flex;
  gap: 10px;
  padding: 10px;
  border: 1px solid #ebeef5;
  border-radius: 8px;
  background: #fff;
  transition: all 0.2s;
}

.image-item:hover {
  border-color: #409eff;
  box-shadow: 0 2px 8px rgba(64, 158, 255, 0.1);
}

.image-thumb {
  width: 48px;
  height: 48px;
  background: #ecf5ff;
  border-radius: 6px;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #409eff;
  position: relative;
  flex-shrink: 0;
}

.image-idx {
  position: absolute;
  bottom: 0;
  right: 0;
  background: #409eff;
  color: #fff;
  font-size: 10px;
  padding: 0 4px;
  border-radius: 2px 0 6px 0;
  line-height: 14px;
}

.image-info {
  flex: 1;
  min-width: 0;
}

.image-name {
  font-size: 13px;
  color: #303133;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
  margin-bottom: 4px;
  font-weight: 500;
}

.image-meta {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 2px;
}

.image-size {
  font-size: 11px;
  color: #909399;
}

.image-uploader {
  font-size: 11px;
  color: #c0c4cc;
}

.image-actions {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.token-section {
  margin-top: 16px;
}

.token-desc {
  font-size: 12px;
  color: #909399;
  margin-bottom: 10px;
  line-height: 1.5;
}

.token-display {
  margin-top: 10px;
}

.token-hint {
  font-size: 11px;
  color: #67c23a;
  margin-top: 4px;
  text-align: right;
}

.conclusion-panel {
  padding: 16px;
  flex: 1;
}

.conclusion-header {
  font-weight: 600;
  font-size: 16px;
  margin-bottom: 12px;
  color: #303133;
}
</style>
