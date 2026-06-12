<template>
  <div class="whiteboard-container" :class="{ 'bg-transparent': transparentBg }" ref="containerRef">
    <div v-if="showToolbar" class="whiteboard-toolbar">
      <div class="tool-group">
        <el-tooltip content="画笔">
          <el-button :type="currentTool === 'PEN' ? 'primary' : 'default'" size="small" circle @click="selectTool('PEN')">
            <el-icon><Edit /></el-icon>
          </el-button>
        </el-tooltip>
        <el-tooltip content="箭头">
          <el-button :type="currentTool === 'ARROW' ? 'primary' : 'default'" size="small" circle @click="selectTool('ARROW')">
            <span class="tool-arrow">→</span>
          </el-button>
        </el-tooltip>
        <el-tooltip content="直线">
          <el-button :type="currentTool === 'LINE' ? 'primary' : 'default'" size="small" circle @click="selectTool('LINE')">
            <el-icon><Minus /></el-icon>
          </el-button>
        </el-tooltip>
        <el-tooltip content="矩形">
          <el-button :type="currentTool === 'RECTANGLE' ? 'primary' : 'default'" size="small" circle @click="selectTool('RECTANGLE')">
            <el-icon><Grid /></el-icon>
          </el-button>
        </el-tooltip>
        <el-tooltip content="椭圆">
          <el-button :type="currentTool === 'ELLIPSE' ? 'primary' : 'default'" size="small" circle @click="selectTool('ELLIPSE')">
            <el-icon><Circle /></el-icon>
          </el-button>
        </el-tooltip>
        <el-tooltip content="文字">
          <el-button :type="currentTool === 'TEXT' ? 'primary' : 'default'" size="small" circle @click="selectTool('TEXT')">
            <el-icon><Tickets /></el-icon>
          </el-button>
        </el-tooltip>
        <el-tooltip content="橡皮擦">
          <el-button :type="currentTool === 'ERASER' ? 'primary' : 'default'" size="small" circle @click="selectTool('ERASER')">
            <el-icon><Rubber /></el-icon>
          </el-button>
        </el-tooltip>
      </div>
      <el-divider direction="vertical" />
      <div class="tool-group">
        <el-tooltip content="颜色">
          <el-color-picker v-model="strokeColor" size="small" />
        </el-tooltip>
        <el-tooltip content="线宽">
          <el-select v-model="strokeWidth" size="small" style="width: 70px">
            <el-option :value="1" label="1px" />
            <el-option :value="2" label="2px" />
            <el-option :value="3" label="3px" />
            <el-option :value="5" label="5px" />
            <el-option :value="8" label="8px" />
          </el-select>
        </el-tooltip>
      </div>
      <el-divider direction="vertical" />
      <div class="tool-group">
        <el-tooltip content="撤销">
          <el-button size="small" circle @click="handleUndo" :disabled="!canUndo">
            <el-icon><RefreshLeft /></el-icon>
          </el-button>
        </el-tooltip>
        <el-tooltip content="重做">
          <el-button size="small" circle @click="handleRedo" :disabled="!canRedo">
            <el-icon><RefreshRight /></el-icon>
          </el-button>
        </el-tooltip>
        <el-tooltip content="清空">
          <el-button size="small" circle type="danger" @click="handleClear">
            <el-icon><Delete /></el-icon>
          </el-button>
        </el-tooltip>
      </div>
      <el-divider direction="vertical" />
      <div class="tool-group">
        <el-tooltip content="保存为图片">
          <el-button size="small" circle type="success" @click="handleSaveSnapshot">
            <el-icon><Picture /></el-icon>
          </el-button>
        </el-tooltip>
        <el-tooltip content="保存并插入病历">
          <el-button size="small" type="success" @click="handleSaveToRecord">
            <el-icon><DocumentAdd /></el-icon>
            插入病历
          </el-button>
        </el-tooltip>
      </div>
    </div>

    <div class="canvas-wrapper" ref="canvasWrapperRef">
      <canvas ref="canvasRef" class="whiteboard-canvas" />
      <input
        v-if="textInputVisible"
        ref="textInputRef"
        v-model="pendingText"
        class="text-input"
        :style="textInputStyle"
        @blur="submitText"
        @keyup.enter="submitText"
        @keyup.escape="cancelText"
      />
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted, onUnmounted, watch, nextTick } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  Edit, Minus, Grid, Circle, Tickets, Rubber,
  RefreshLeft, RefreshRight, Delete, Picture, DocumentAdd
} from '@element-plus/icons-vue'
import type { WhiteboardOp, WhiteboardTool, WhiteboardPoint, WhiteboardSource } from '@/types'
import { saveWhiteboardSnapshot, clearWhiteboard } from '@/api/whiteboard'

const props = defineProps<{
  roomId: string
  source?: WhiteboardSource
  imageId?: number
  userId: number
  userName: string
  showToolbar?: boolean
  readOnly?: boolean
  transparentBg?: boolean
  initialOps?: WhiteboardOp[]
}>()

const emit = defineEmits<{
  (e: 'draw', op: WhiteboardOp): void
  (e: 'clear', payload: any): void
  (e: 'undo', payload: any): void
  (e: 'redo', payload: any): void
  (e: 'save', snapshotData: string): void
  (e: 'save-to-record', snapshotData: string): void
}>()

const containerRef = ref<HTMLElement>()
const canvasWrapperRef = ref<HTMLElement>()
const canvasRef = ref<HTMLCanvasElement>()
const textInputRef = ref<HTMLInputElement>()

const showToolbar = computed(() => props.showToolbar !== false)
const readOnly = computed(() => props.readOnly === true)

const currentTool = ref<WhiteboardTool>('PEN')
const strokeColor = ref('#ff4d4f')
const strokeWidth = ref(2)

const isDrawing = ref(false)
const currentPoints = ref<WhiteboardPoint[]>([])
const ops = ref<WhiteboardOp[]>([])
const redoStack = ref<WhiteboardOp[]>([])

const textInputVisible = ref(false)
const pendingText = ref('')
const textInputPosition = ref({ x: 0, y: 0 })

const canUndo = computed(() => ops.value.length > 0)
const canRedo = computed(() => redoStack.value.length > 0)

const textInputStyle = computed(() => ({
  left: textInputPosition.value.x + 'px',
  top: textInputPosition.value.y + 'px',
  color: strokeColor.value,
  fontSize: (strokeWidth.value * 8) + 'px'
}))

let ctx: CanvasRenderingContext2D | null = null
let canvasWidth = 0
let canvasHeight = 0
let dpr = 1

function initCanvas() {
  if (!canvasRef.value || !canvasWrapperRef.value) return

  const canvas = canvasRef.value
  const wrapper = canvasWrapperRef.value
  dpr = window.devicePixelRatio || 1
  canvasWidth = wrapper.clientWidth
  canvasHeight = wrapper.clientHeight

  canvas.width = canvasWidth * dpr
  canvas.height = canvasHeight * dpr
  canvas.style.width = canvasWidth + 'px'
  canvas.style.height = canvasHeight + 'px'

  ctx = canvas.getContext('2d')
  if (ctx) {
    ctx.scale(dpr, dpr)
    ctx.lineCap = 'round'
    ctx.lineJoin = 'round'
  }

  redrawAll()
}

function getPointerPos(e: MouseEvent | TouchEvent): WhiteboardPoint {
  if (!canvasRef.value) return { x: 0, y: 0 }
  const rect = canvasRef.value.getBoundingClientRect()
  let clientX = 0, clientY = 0
  if ('touches' in e) {
    clientX = e.touches[0]?.clientX || 0
    clientY = e.touches[0]?.clientY || 0
  } else {
    clientX = e.clientX
    clientY = e.clientY
  }
  return {
    x: clientX - rect.left,
    y: clientY - rect.top
  }
}

function selectTool(tool: WhiteboardTool) {
  if (readOnly.value) return
  currentTool.value = tool
  if (ctx) {
    if (tool === 'ERASER') {
      ctx.globalCompositeOperation = 'destination-out'
    } else {
      ctx.globalCompositeOperation = 'source-over'
    }
  }
}

function startDrawing(e: MouseEvent | TouchEvent) {
  if (readOnly.value || !ctx) return

  if (currentTool.value === 'TEXT') {
    const pos = getPointerPos(e)
    textInputPosition.value = { x: pos.x, y: pos.y - 20 }
    pendingText.value = ''
    textInputVisible.value = true
    nextTick(() => {
      textInputRef.value?.focus()
    })
    return
  }

  isDrawing.value = true
  const pos = getPointerPos(e)
  currentPoints.value = [pos]
  redoStack.value = []

  ctx.beginPath()
  ctx.moveTo(pos.x, pos.y)
  ctx.strokeStyle = currentTool.value === 'ERASER' ? 'rgba(0,0,0,1)' : strokeColor.value
  ctx.lineWidth = currentTool.value === 'ERASER' ? strokeWidth.value * 5 : strokeWidth.value

  if (currentTool.value === 'PEN' || currentTool.value === 'ERASER') {
    // free draw starts
  }
}

function draw(e: MouseEvent | TouchEvent) {
  if (!isDrawing.value || !ctx) return

  const pos = getPointerPos(e)
  currentPoints.value.push(pos)

  if (currentTool.value === 'PEN' || currentTool.value === 'ERASER') {
    ctx.lineTo(pos.x, pos.y)
    ctx.stroke()
  } else {
    redrawAll()
    drawShape(ctx, currentTool.value, currentPoints.value, strokeColor.value, strokeWidth.value)
  }
}

function stopDrawing() {
  if (!isDrawing.value) return

  isDrawing.value = false

  if (currentPoints.value.length < 2 && currentTool.value !== 'TEXT') {
    currentPoints.value = []
    return
  }

  const op: WhiteboardOp = {
    opId: 'op_' + Date.now() + '_' + Math.random().toString(36).slice(2, 8),
    roomId: props.roomId,
    source: props.source || 'BLANK',
    imageId: props.imageId,
    operation: 'DRAW',
    toolType: currentTool.value,
    points: [...currentPoints.value],
    color: strokeColor.value,
    strokeWidth: currentTool.value === 'ERASER' ? strokeWidth.value * 5 : strokeWidth.value,
    operatorId: props.userId,
    operatorName: props.userName,
    timestamp: Date.now()
  }

  ops.value.push(op)
  currentPoints.value = []

  emit('draw', op)
  redrawAll()
}

function drawShape(c: CanvasRenderingContext2D, tool: WhiteboardTool, points: WhiteboardPoint[], color: string, width: number) {
  if (points.length < 2) return

  c.save()
  c.strokeStyle = color
  c.lineWidth = width
  c.lineCap = 'round'
  c.lineJoin = 'round'

  const start = points[0]
  const end = points[points.length - 1]

  switch (tool) {
    case 'LINE':
      c.beginPath()
      c.moveTo(start.x, start.y)
      c.lineTo(end.x, end.y)
      c.stroke()
      break
    case 'ARROW':
      c.beginPath()
      c.moveTo(start.x, start.y)
      c.lineTo(end.x, end.y)
      c.stroke()
      drawArrowhead(c, start.x, start.y, end.x, end.y, color, width)
      break
    case 'RECTANGLE':
      c.strokeRect(start.x, start.y, end.x - start.x, end.y - start.y)
      break
    case 'ELLIPSE':
      c.beginPath()
      const rx = Math.abs(end.x - start.x) / 2
      const ry = Math.abs(end.y - start.y) / 2
      const cx = start.x + (end.x - start.x) / 2
      const cy = start.y + (end.y - start.y) / 2
      c.ellipse(cx, cy, rx, ry, 0, 0, Math.PI * 2)
      c.stroke()
      break
    case 'PEN':
    case 'ERASER':
      if (points.length >= 2) {
        c.beginPath()
        c.moveTo(points[0].x, points[0].y)
        for (let i = 1; i < points.length; i++) {
          c.lineTo(points[i].x, points[i].y)
        }
        c.stroke()
      }
      break
  }

  c.restore()
}

function drawArrowhead(c: CanvasRenderingContext2D, x1: number, y1: number, x2: number, y2: number, color: string, width: number) {
  const headLen = 15 + width * 2
  const angle = Math.atan2(y2 - y1, x2 - x1)

  c.save()
  c.fillStyle = color
  c.beginPath()
  c.moveTo(x2, y2)
  c.lineTo(
    x2 - headLen * Math.cos(angle - Math.PI / 6),
    y2 - headLen * Math.sin(angle - Math.PI / 6)
  )
  c.lineTo(
    x2 - headLen * Math.cos(angle + Math.PI / 6),
    y2 - headLen * Math.sin(angle + Math.PI / 6)
  )
  c.closePath()
  c.fill()
  c.restore()
}

function redrawAll() {
  if (!ctx) return
  ctx.clearRect(0, 0, canvasWidth, canvasHeight)
  ctx.globalCompositeOperation = 'source-over'

  for (const op of ops.value) {
    if (op.operation === 'DRAW' && op.points && op.points.length > 0) {
      drawShape(ctx, op.toolType, op.points, op.color, op.strokeWidth)
    }
  }
}

function submitText() {
  if (!pendingText.value.trim() || !ctx) {
    cancelText()
    return
  }

  const pos = textInputPosition.value
  const fontSize = strokeWidth.value * 8

  ctx.save()
  ctx.fillStyle = strokeColor.value
  ctx.font = `${fontSize}px sans-serif`
  ctx.fillText(pendingText.value, pos.x, pos.y + fontSize)
  ctx.restore()

  const op: WhiteboardOp = {
    opId: 'op_' + Date.now() + '_' + Math.random().toString(36).slice(2, 8),
    roomId: props.roomId,
    source: props.source || 'BLANK',
    imageId: props.imageId,
    operation: 'DRAW',
    toolType: 'TEXT',
    points: [{ x: pos.x, y: pos.y }],
    color: strokeColor.value,
    strokeWidth: strokeWidth.value,
    text: pendingText.value,
    operatorId: props.userId,
    operatorName: props.userName,
    timestamp: Date.now()
  }

  ops.value.push(op)
  redoStack.value = []
  emit('draw', op)

  cancelText()
}

function cancelText() {
  textInputVisible.value = false
  pendingText.value = ''
}

function handleUndo() {
  if (readOnly.value || ops.value.length === 0) return
  const lastOp = ops.value.pop()!
  redoStack.value.push(lastOp)
  redrawAll()
  emit('undo', { opId: lastOp.opId })
}

function handleRedo() {
  if (readOnly.value || redoStack.value.length === 0) return
  const op = redoStack.value.pop()!
  ops.value.push(op)
  redrawAll()
  emit('redo', { opId: op.opId })
}

function handleClear() {
  if (readOnly.value || ops.value.length === 0) return
  ElMessageBox.confirm('确定要清空白板吗？此操作不可撤销。', '清空确认', {
    type: 'warning',
    confirmButtonText: '清空',
    cancelButtonText: '取消'
  }).then(() => {
    ops.value = []
    redoStack.value = []
    redrawAll()
    emit('clear', {
      roomId: props.roomId,
      source: props.source || 'BLANK',
      imageId: props.imageId,
      operatorId: props.userId,
      operatorName: props.userName
    })
    ElMessage.success('白板已清空')
  }).catch(() => {})
}

function exportCanvas(): string {
  if (!canvasRef.value) return ''
  return canvasRef.value.toDataURL('image/png')
}

async function handleSaveSnapshot() {
  const dataUrl = exportCanvas()
  if (!dataUrl) {
    ElMessage.warning('白板为空，无法保存')
    return
  }

  try {
    await saveWhiteboardSnapshot({
      roomId: props.roomId,
      source: props.source || 'BLANK',
      imageId: props.imageId,
      snapshotData: dataUrl,
      format: 'png',
      fileName: `whiteboard_${Date.now()}.png`,
      operatorId: props.userId,
      operatorName: props.userName,
      consultationId: Number(props.roomId),
      insertToRecord: false
    })
    emit('save', dataUrl)
    ElMessage.success('快照已保存至MinIO')
  } catch {
    emit('save', dataUrl)
    ElMessage.error('快照上传失败，已生成本地快照')
  }
}

async function handleSaveToRecord() {
  const dataUrl = exportCanvas()
  if (!dataUrl) {
    ElMessage.warning('白板为空，无法保存')
    return
  }

  try {
    await saveWhiteboardSnapshot({
      roomId: props.roomId,
      source: props.source || 'BLANK',
      imageId: props.imageId,
      snapshotData: dataUrl,
      format: 'png',
      fileName: `whiteboard_${Date.now()}.png`,
      operatorId: props.userId,
      operatorName: props.userName,
      consultationId: Number(props.roomId),
      insertToRecord: true
    })
    emit('save-to-record', dataUrl)
    ElMessage.success('已保存并插入电子病历')
  } catch {
    ElMessage.error('保存失败')
  }
}

function addRemoteOp(op: WhiteboardOp) {
  if (op.operation === 'DRAW') {
    ops.value.push(op)
    redrawAll()
  } else if (op.operation === 'CLEAR') {
    ops.value = []
    redoStack.value = []
    redrawAll()
  }
}

function applyOps(opList: WhiteboardOp[]) {
  ops.value = [...opList]
  redrawAll()
}

function handleClearRemote() {
  ops.value = []
  redoStack.value = []
  redrawAll()
}

function clearCanvas() {
  ops.value = []
  redoStack.value = []
  redrawAll()
}

function handleResize() {
  if (!canvasRef.value || !canvasWrapperRef.value) return
  const prevOps = [...ops.value]
  initCanvas()
  ops.value = prevOps
  redrawAll()
}

let resizeObserver: ResizeObserver | null = null

onMounted(() => {
  nextTick(() => {
    initCanvas()
    if (props.initialOps && props.initialOps.length > 0) {
      ops.value = [...props.initialOps]
      redrawAll()
    }
    if (canvasWrapperRef.value && typeof ResizeObserver !== 'undefined') {
      resizeObserver = new ResizeObserver(handleResize)
      resizeObserver.observe(canvasWrapperRef.value)
    }
  })
})

onUnmounted(() => {
  resizeObserver?.disconnect()
  resizeObserver = null
})

watch(() => props.initialOps, (newOps) => {
  if (newOps && newOps.length > 0 && ops.value.length === 0) {
    ops.value = [...newOps]
    redrawAll()
  }
})

watch(() => props.imageId, (newImageId, oldImageId) => {
  if (props.source === 'DICOM' && newImageId !== undefined && newImageId !== oldImageId) {
    clearCanvas()
    ops.value = []
    redoStack.value = []
  }
})

defineExpose({
  addRemoteOp,
  applyOps,
  handleClearRemote,
  exportCanvas,
  handleResize,
  clearCanvas
})
</script>

<style scoped>
.whiteboard-container {
  display: flex;
  flex-direction: column;
  width: 100%;
  height: 100%;
  background: #fff;
  overflow: hidden;
  position: relative;
}

.whiteboard-toolbar {
  display: flex;
  align-items: center;
  gap: 6px;
  padding: 6px 10px;
  background: #f5f7fa;
  border-bottom: 1px solid #e4e7ed;
  flex-shrink: 0;
  flex-wrap: wrap;
}

.tool-group {
  display: flex;
  align-items: center;
  gap: 4px;
}

.tool-arrow {
  font-size: 16px;
  font-weight: bold;
}

.canvas-wrapper {
  flex: 1;
  position: relative;
  overflow: hidden;
  cursor: crosshair;
  min-height: 0;
}

.whiteboard-canvas {
  position: absolute;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
  cursor: crosshair;
}

.text-input {
  position: absolute;
  border: 1px dashed #409eff;
  background: transparent;
  outline: none;
  padding: 2px 4px;
  min-width: 100px;
  z-index: 10;
  font-family: sans-serif;
}

.bg-transparent {
  background: transparent;
}

.bg-transparent .canvas-wrapper {
  background: transparent;
}

.bg-transparent .whiteboard-canvas {
  background: transparent;
}

.bg-transparent .whiteboard-toolbar {
  background: rgba(255, 255, 255, 0.92);
  backdrop-filter: blur(6px);
  border-bottom: 1px solid rgba(0, 0, 0, 0.08);
}
</style>
