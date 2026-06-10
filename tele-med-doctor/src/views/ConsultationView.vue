<template>
  <div class="consultation-container">
    <div class="video-area">
      <video ref="remoteVideoRef" class="remote-video" autoplay playsinline />
      <video ref="localVideoRef" class="local-video" autoplay playsinline muted />
      <div class="control-bar">
        <el-button :type="audioEnabled ? 'primary' : 'danger'" circle @click="toggleAudio">
          <el-icon><Microphone v-if="audioEnabled" /><Mute v-else /></el-icon>
        </el-button>
        <el-button :type="videoEnabled ? 'primary' : 'danger'" circle @click="toggleVideo">
          <el-icon><VideoCamera v-if="videoEnabled" /><VideoPause v-else /></el-icon>
        </el-button>
        <el-button type="danger" @click="handleEndConsultation" :disabled="consultationEnded">结束问诊</el-button>
      </div>
    </div>
    <div class="side-panel">
      <div class="chat-area">
        <div class="chat-header">文字聊天</div>
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
      <div class="conclusion-panel" v-if="showConclusion">
        <div class="conclusion-header">问诊结论</div>
        <el-input
          v-model="conclusionContent"
          type="textarea"
          :rows="4"
          placeholder="请输入问诊结论..."
        />
        <el-button type="primary" style="margin-top: 12px; width: 100%" @click="handleSubmitConclusion" :loading="submittingConclusion">提交结论</el-button>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, onUnmounted, nextTick } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useUserStore } from '@/stores/user'
import { getChatMessages, finishConsultation } from '@/api/consultation'
import { SignalingWebSocket } from '@/utils/websocket'
import { JanusVideoRoom } from '@/utils/janus'
import { ElMessage } from 'element-plus'
import { Microphone, Mute, VideoCamera, VideoPause } from '@element-plus/icons-vue'
import type { ChatMessage, SignalingMessage } from '@/types'

const route = useRoute()
const router = useRouter()
const userStore = useUserStore()

const consultationId = Number(route.params.id)
const remoteVideoRef = ref<HTMLVideoElement>()
const localVideoRef = ref<HTMLVideoElement>()
const chatMessagesRef = ref<HTMLDivElement>()

const chatMessages = ref<ChatMessage[]>([])
const chatInput = ref('')
const audioEnabled = ref(true)
const videoEnabled = ref(true)
const consultationEnded = ref(false)
const showConclusion = ref(false)
const conclusionContent = ref('')
const submittingConclusion = ref(false)

let videoRoom: JanusVideoRoom | null = null
let signaling: SignalingWebSocket | null = null
let chatTimer: ReturnType<typeof setInterval> | null = null

const userId = String(userStore.user?.id || '')

const formatTime = (time: string) => {
  if (!time) return ''
  return new Date(time).toLocaleTimeString()
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

const sendChat = () => {
  if (!chatInput.value.trim() || !signaling) return
  const message: SignalingMessage = {
    type: 'chat',
    from: userId,
    to: '',
    roomId: String(consultationId),
    payload: { content: chatInput.value },
    timestamp: Date.now()
  }
  signaling.send(message)
  chatMessages.value.push({
    consultationId,
    senderId: Number(userId),
    senderName: userStore.user?.name || userStore.user?.username || '医生',
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
    ElMessage.success('问诊已结束')
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
  } catch {
    ElMessage.error('提交结论失败')
  } finally {
    submittingConclusion.value = false
  }
}

const handleSignalingMessage = (message: SignalingMessage) => {
  if (message.type === 'chat') {
    chatMessages.value.push({
      consultationId,
      senderId: Number(message.from),
      senderName: message.payload?.senderName || '患者',
      senderType: 'PATIENT',
      content: message.payload?.content || '',
      createTime: new Date().toISOString()
    })
    scrollToBottom()
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
  chatTimer = setInterval(fetchChatMessages, 5000)
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
})
</script>

<style scoped>
.consultation-container {
  display: flex;
  height: 100vh;
  background: #1a1a2e;
  overflow: hidden;
}

.video-area {
  flex: 1;
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
  gap: 16px;
  padding: 12px 24px;
  background: rgba(0, 0, 0, 0.7);
  border-radius: 12px;
  z-index: 20;
}

.side-panel {
  width: 380px;
  display: flex;
  flex-direction: column;
  background: #fff;
  border-left: 1px solid #e4e7ed;
}

.chat-area {
  flex: 1;
  display: flex;
  flex-direction: column;
  min-height: 0;
}

.chat-header {
  padding: 16px;
  font-weight: 600;
  font-size: 16px;
  border-bottom: 1px solid #e4e7ed;
  color: #303133;
}

.chat-messages {
  flex: 1;
  overflow-y: auto;
  padding: 12px 16px;
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
}

.conclusion-panel {
  padding: 16px;
  border-top: 1px solid #e4e7ed;
}

.conclusion-header {
  font-weight: 600;
  font-size: 16px;
  margin-bottom: 12px;
  color: #303133;
}
</style>
