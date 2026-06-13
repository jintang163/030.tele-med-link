<template>
  <div class="signature-wrapper">
    <div class="signature-header">
      <span class="signature-title">{{ title }}</span>
      <div class="signature-actions">
        <el-button size="small" @click="clearCanvas">清除</el-button>
        <el-button size="small" type="primary" @click="confirmSignature" :disabled="!hasDrawn">确认签名</el-button>
      </div>
    </div>
    <div class="canvas-container" ref="containerRef">
      <canvas
        ref="canvasRef"
        class="signature-canvas"
        @mousedown="startDraw"
        @mousemove="drawing"
        @mouseup="endDraw"
        @mouseleave="endDraw"
        @touchstart.prevent="startDrawTouch"
        @touchmove.prevent="drawingTouch"
        @touchend.prevent="endDraw"
      />
    </div>
    <div class="signature-hint" v-if="!hasDrawn">请在上方区域手写签名</div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, onUnmounted, nextTick } from 'vue'
import { ElMessage } from 'element-plus'

const props = withDefaults(defineProps<{
  title?: string
  width?: number
  height?: number
  penColor?: string
  penWidth?: number
}>(), {
  title: '手写签名',
  width: 500,
  height: 200,
  penColor: '#000000',
  penWidth: 3
})

const emit = defineEmits<{
  (e: 'confirm', signatureBase64: string): void
  (e: 'clear'): void
}>()

const canvasRef = ref<HTMLCanvasElement>()
const containerRef = ref<HTMLDivElement>()
const hasDrawn = ref(false)

let ctx: CanvasRenderingContext2D | null = null
let isDrawing = false
let lastX = 0
let lastY = 0

const initCanvas = () => {
  if (!canvasRef.value || !containerRef.value) return
  const canvas = canvasRef.value
  const container = containerRef.value
  const dpr = window.devicePixelRatio || 1
  const displayWidth = container.clientWidth || props.width
  const displayHeight = props.height

  canvas.width = displayWidth * dpr
  canvas.height = displayHeight * dpr
  canvas.style.width = displayWidth + 'px'
  canvas.style.height = displayHeight + 'px'

  ctx = canvas.getContext('2d')
  if (ctx) {
    ctx.scale(dpr, dpr)
    ctx.strokeStyle = props.penColor
    ctx.lineWidth = props.penWidth
    ctx.lineCap = 'round'
    ctx.lineJoin = 'round'
  }
}

const getCanvasPos = (e: MouseEvent) => {
  if (!canvasRef.value) return { x: 0, y: 0 }
  const rect = canvasRef.value.getBoundingClientRect()
  return {
    x: e.clientX - rect.left,
    y: e.clientY - rect.top
  }
}

const getTouchPos = (e: TouchEvent) => {
  if (!canvasRef.value || !e.touches[0]) return { x: 0, y: 0 }
  const rect = canvasRef.value.getBoundingClientRect()
  return {
    x: e.touches[0].clientX - rect.left,
    y: e.touches[0].clientY - rect.top
  }
}

const startDraw = (e: MouseEvent) => {
  isDrawing = true
  const pos = getCanvasPos(e)
  lastX = pos.x
  lastY = pos.y
  if (ctx) {
    ctx.beginPath()
    ctx.moveTo(pos.x, pos.y)
  }
}

const startDrawTouch = (e: TouchEvent) => {
  isDrawing = true
  const pos = getTouchPos(e)
  lastX = pos.x
  lastY = pos.y
  if (ctx) {
    ctx.beginPath()
    ctx.moveTo(pos.x, pos.y)
  }
}

const drawing = (e: MouseEvent) => {
  if (!isDrawing || !ctx) return
  hasDrawn.value = true
  const pos = getCanvasPos(e)
  ctx.beginPath()
  ctx.moveTo(lastX, lastY)
  ctx.lineTo(pos.x, pos.y)
  ctx.stroke()
  lastX = pos.x
  lastY = pos.y
}

const drawingTouch = (e: TouchEvent) => {
  if (!isDrawing || !ctx) return
  hasDrawn.value = true
  const pos = getTouchPos(e)
  ctx.beginPath()
  ctx.moveTo(lastX, lastY)
  ctx.lineTo(pos.x, pos.y)
  ctx.stroke()
  lastX = pos.x
  lastY = pos.y
}

const endDraw = () => {
  isDrawing = false
}

const clearCanvas = () => {
  if (!canvasRef.value || !ctx) return
  const dpr = window.devicePixelRatio || 1
  ctx.clearRect(0, 0, canvasRef.value.width / dpr, canvasRef.value.height / dpr)
  hasDrawn.value = false
  emit('clear')
}

const confirmSignature = () => {
  if (!canvasRef.value || !hasDrawn.value) {
    ElMessage.warning('请先手写签名')
    return
  }
  const base64 = canvasRef.value.toDataURL('image/png')
  emit('confirm', base64)
}

const resizeObserver = ref<ResizeObserver>()

onMounted(() => {
  nextTick(() => {
    initCanvas()
    if (containerRef.value) {
      resizeObserver.value = new ResizeObserver(() => {
        const hadContent = hasDrawn.value
        initCanvas()
        hasDrawn.value = hadContent
      })
      resizeObserver.value.observe(containerRef.value)
    }
  })
})

onUnmounted(() => {
  resizeObserver.value?.disconnect()
})

defineExpose({
  clearCanvas,
  getSignatureBase64: () => canvasRef.value?.toDataURL('image/png') || '',
  hasDrawn
})
</script>

<style scoped>
.signature-wrapper {
  border: 1px solid #dcdfe6;
  border-radius: 8px;
  overflow: hidden;
  background: #fff;
}

.signature-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 8px 12px;
  background: #f5f7fa;
  border-bottom: 1px solid #e4e7ed;
}

.signature-title {
  font-size: 14px;
  font-weight: 600;
  color: #303133;
}

.signature-actions {
  display: flex;
  gap: 8px;
}

.canvas-container {
  position: relative;
  width: 100%;
}

.signature-canvas {
  display: block;
  cursor: crosshair;
  background: #fff;
}

.signature-hint {
  padding: 6px 12px;
  font-size: 12px;
  color: #c0c4cc;
  text-align: center;
  border-top: 1px dashed #e4e7ed;
}
</style>
