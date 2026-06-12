<template>
  <div class="dicom-viewer" ref="viewerContainerRef">
    <div class="dicom-toolbar">
      <div class="tool-group">
        <el-tooltip content="调窗 (W/L)">
          <el-button :type="currentTool === 'wwwc' ? 'primary' : 'default'" size="small" circle @click="selectTool('wwwc')">
            <span class="tool-icon">WL</span>
          </el-button>
        </el-tooltip>
        <el-tooltip content="平移">
          <el-button :type="currentTool === 'pan' ? 'primary' : 'default'" size="small" circle @click="selectTool('pan')">
            <el-icon><Aim /></el-icon>
          </el-button>
        </el-tooltip>
        <el-tooltip content="缩放">
          <el-button :type="currentTool === 'zoom' ? 'primary' : 'default'" size="small" circle @click="selectTool('zoom')">
            <el-icon><ZoomIn /></el-icon>
          </el-button>
        </el-tooltip>
        <el-divider direction="vertical" />
        <el-tooltip content="长度测量">
          <el-button :type="currentTool === 'length' ? 'primary' : 'default'" size="small" circle @click="selectTool('length')">
            <span class="tool-icon">L</span>
          </el-button>
        </el-tooltip>
        <el-tooltip content="矩形测量">
          <el-button :type="currentTool === 'rectangle' ? 'primary' : 'default'" size="small" circle @click="selectTool('rectangle')">
            <span class="tool-icon">R</span>
          </el-button>
        </el-tooltip>
        <el-tooltip content="椭圆测量">
          <el-button :type="currentTool === 'ellipse' ? 'primary' : 'default'" size="small" circle @click="selectTool('ellipse')">
            <span class="tool-icon">E</span>
          </el-button>
        </el-tooltip>
        <el-tooltip content="角度测量">
          <el-button :type="currentTool === 'angle' ? 'primary' : 'default'" size="small" circle @click="selectTool('angle')">
            <span class="tool-icon">A</span>
          </el-button>
        </el-tooltip>
        <el-tooltip content="箭头标注">
          <el-button :type="currentTool === 'annotate' ? 'primary' : 'default'" size="small" circle @click="selectTool('annotate')">
            <el-icon><EditPen /></el-icon>
          </el-button>
        </el-tooltip>
        <el-divider direction="vertical" />
        <el-tooltip content="重置视口">
          <el-button size="small" circle @click="resetCurrentViewport">
            <el-icon><Refresh /></el-icon>
          </el-button>
        </el-tooltip>
        <el-tooltip content="反转">
          <el-button size="small" circle @click="toggleInvert">
            <el-icon><Sunny /></el-icon>
          </el-button>
        </el-tooltip>
        <el-tooltip content="逆时针旋转">
          <el-button size="small" circle @click="rotateViewport(-90)">
            <el-icon><RefreshLeft /></el-icon>
          </el-button>
        </el-tooltip>
        <el-tooltip content="顺时针旋转">
          <el-button size="small" circle @click="rotateViewport(90)">
            <el-icon><RefreshRight /></el-icon>
          </el-button>
        </el-tooltip>
        <el-divider direction="vertical" />
        <el-tooltip :content="syncViewports ? '关闭视口同步' : '开启视口同步'">
          <el-button size="small" :type="syncViewports ? 'success' : 'default'" circle @click="syncViewports = !syncViewports">
            <el-icon><Link /></el-icon>
          </el-button>
        </el-tooltip>
      </div>
      <div class="layout-group">
        <span class="layout-label">布局:</span>
        <el-radio-group v-model="layout" size="small">
          <el-radio-button label="1x1">1</el-radio-button>
          <el-radio-button label="2x1">2H</el-radio-button>
          <el-radio-button label="1x2">2V</el-radio-button>
          <el-radio-button label="2x2">4</el-radio-button>
        </el-radio-group>
      </div>
    </div>

    <div class="dicom-viewport-container" :class="layout">
      <div
        v-for="(vp, idx) in viewports"
        :key="idx"
        class="dicom-viewport"
        :class="{ active: activeViewportIndex === idx, 'has-image': vp.loaded }"
        @click="setActiveViewport(idx)"
      >
        <div
          class="cornerstone-element"
          :ref="(el: any) => viewportRefs[idx] = el"
          @contextmenu.prevent
        ></div>
        <div class="viewport-overlay">
          <div class="viewport-index">{{ idx + 1 }}</div>
          <div class="viewport-info" v-if="vp.imageInfo">
            <div class="info-line">{{ vp.imageInfo.modality || 'DICOM' }} | {{ vp.imageInfo.fileName }}</div>
            <div class="info-line">W: {{ vp.ww || '-' }}  L: {{ vp.wc || '-' }}</div>
            <div class="info-line">缩放: {{ ((vp.scale || 1) * 100).toFixed(0) }}%</div>
          </div>
          <div v-if="!vp.loaded" class="empty-placeholder">
            <el-select
              v-if="availableImages.length > 0"
              v-model="vp.selectedImageId"
              placeholder="选择DICOM影像"
              size="small"
              style="width: 80%"
              @change="(val: number) => onSelectImage(idx, val)"
            >
              <el-option
                v-for="img in availableImages"
                :key="img.id"
                :label="img.fileName + (img.modality ? ` (${img.modality})` : '')"
                :value="img.id"
              />
            </el-select>
            <el-empty v-else description="暂无可用影像" :image-size="60" />
          </div>
        </div>
        <el-dropdown
          v-if="vp.loaded"
          @command="(cmd: string) => onViewportCommand(cmd, idx)"
          trigger="contextmenu"
          @click.native.stop
        >
          <span class="dropdown-trigger"></span>
          <template #dropdown>
            <el-dropdown-menu>
              <el-dropdown-item command="reset">重置视口</el-dropdown-item>
              <el-dropdown-item command="clearAnnotations">清除标注</el-dropdown-item>
              <el-dropdown-item command="unload">移除影像</el-dropdown-item>
              <el-dropdown-item command="changeImage">切换影像...</el-dropdown-item>
            </el-dropdown-menu>
          </template>
        </el-dropdown>
      </div>
    </div>

    <div class="dicom-statusbar">
      <span v-if="activeViewport && activeViewport.imageInfo">
        患者: {{ activeViewport.imageInfo.patientName || '-' }} |
        序列: {{ activeViewport.imageInfo.seriesDescription || '-' }} |
        上传者: {{ activeViewport.imageInfo.uploaderName || '-' }}
      </span>
      <span v-else>当前会诊共有 {{ availableImages.length }} 张影像</span>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, watch, onMounted, onUnmounted, nextTick } from 'vue'
import { ElMessage } from 'element-plus'
import {
  Aim, ZoomIn, EditPen, Refresh, Sunny, RefreshLeft, RefreshRight, Link
} from '@element-plus/icons-vue'
import type { DicomImage, DicomAnnotationSyncPayload, DicomViewportState, DicomViewportSyncPayload, SignalingMessage } from '@/types'
import {
  initCornerstone, enableElement, disableElement, loadDicomImage,
  setToolActive, getViewport, setViewport, resetViewport,
  resize, type DicomTool
} from '@/utils/cornerstone'
import * as cornerstone from 'cornerstone-core'
import * as cornerstoneTools from 'cornerstone-tools'
import { getDicomImageUrlByToken, getDicomImageUrlByTokenAndImageId } from '@/api/dicom'

const props = defineProps<{
  images: DicomImage[]
  accessToken?: string
  consultationId: number
  userId: number
  userName?: string
}>()

const emit = defineEmits<{
  (e: 'annotation-sync', payload: DicomAnnotationSyncPayload): void
  (e: 'viewport-sync', payload: DicomViewportSyncPayload): void
  (e: 'image-loaded', image: DicomImage): void
}>()

interface ViewportState {
  loaded: boolean
  loading: boolean
  imageId?: number
  imageInfo?: DicomImage
  imageUrl?: string
  selectedImageId?: number
  wc?: number
  ww?: number
  scale?: number
}

const viewerContainerRef = ref<HTMLElement>()
const viewportRefs = ref<(HTMLElement | null)[]>([])
const activeViewportIndex = ref(0)
const currentTool = ref<DicomTool>('wwwc')
const layout = ref('2x2')
const syncViewports = ref(false)

const viewports = ref<ViewportState[]>([
  { loaded: false, loading: false },
  { loaded: false, loading: false },
  { loaded: false, loading: false },
  { loaded: false, loading: false }
])

const availableImages = computed(() => props.images)
const activeViewport = computed(() => viewports.value[activeViewportIndex.value])

const layoutCells = computed(() => {
  const [r, c] = layout.value.split('x').map(Number)
  return r * c
})

watch(layoutCells, (newCount) => {
  while (viewports.value.length < newCount) {
    viewports.value.push({ loaded: false, loading: false })
  }
  while (viewports.value.length > newCount) {
    const last = viewports.value.pop()
    const idx = viewports.value.length
    if (last && last.loaded && viewportRefs.value[idx]) {
      disableElement(viewportRefs.value[idx]!)
    }
  }
  if (activeViewportIndex.value >= newCount) {
    activeViewportIndex.value = 0
  }
  nextTick(initAllViewports)
})

let cornerstoneInited = false

function selectTool(tool: DicomTool) {
  currentTool.value = tool
  applyToolToAll(tool)
}

function applyToolToAll(tool: DicomTool) {
  const count = layoutCells.value
  for (let i = 0; i < count; i++) {
    const el = viewportRefs.value[i]
    if (el && viewports.value[i].loaded) {
      if (tool === 'none') {
        setToolActive(el, 'wwwc', { mouseButtonMask: 2 })
      } else {
        setToolActive(el, tool, { mouseButtonMask: 1 })
        setToolActive(el, 'wwwc', { mouseButtonMask: 2 })
        setToolActive(el, 'zoom', { mouseButtonMask: 4, syncViewports: syncViewports.value })
      }
    }
  }
}

function setActiveViewport(idx: number) {
  activeViewportIndex.value = idx
}

function resetCurrentViewport() {
  const el = viewportRefs.value[activeViewportIndex.value]
  if (el) {
    resetViewport(el)
    updateViewportInfo(activeViewportIndex.value)
  }
}

function toggleInvert() {
  const el = viewportRefs.value[activeViewportIndex.value]
  if (el) {
    const vp = getViewport(el)
    vp.invert = !vp.invert
    setViewport(el, vp)
  }
}

function rotateViewport(deg: number) {
  const el = viewportRefs.value[activeViewportIndex.value]
  if (el) {
    const vp = getViewport(el)
    vp.rotation = (vp.rotation || 0) + deg
    setViewport(el, vp)
    syncViewportState(activeViewportIndex.value)
  }
}

async function onSelectImage(vpIdx: number, imageId: number) {
  const img = availableImages.value.find(i => i.id === imageId)
  if (!img) return

  const vp = viewports.value[vpIdx]
  if (vp.loading) return
  vp.loading = true

  try {
    let imageUrl = ''
    if (props.accessToken) {
      try {
        const urlRes = await getDicomImageUrlByTokenAndImageId(props.accessToken, imageId)
        imageUrl = urlRes.data
      } catch {
        try {
          const urlRes = await getDicomImageUrlByToken(props.accessToken)
          imageUrl = urlRes.data
        } catch {
          imageUrl = await fetchImageUrlDirect(imageId)
        }
      }
    } else {
      imageUrl = await fetchImageUrlDirect(imageId)
    }

    vp.imageUrl = imageUrl
    await nextTick()
    const el = viewportRefs.value[vpIdx]
    if (!el) {
      vp.loading = false
      return
    }

    if (!cornerstoneInited) {
      initCornerstone()
      cornerstoneInited = true
    }

    enableElement(el)
    await loadDicomImage(imageUrl, el)

    vp.loaded = true
    vp.imageId = imageId
    vp.imageInfo = img

    applyToolToAll(currentTool.value)
    updateViewportInfo(vpIdx)
    bindViewportEvents(el, vpIdx)

    emit('image-loaded', img)
    ElMessage.success(`影像加载成功: ${img.fileName}`)
  } catch (e) {
    console.error('加载DICOM影像失败', e)
    ElMessage.error('影像加载失败')
  } finally {
    vp.loading = false
  }
}

async function fetchImageUrlDirect(imageId: number): Promise<string> {
  try {
    const { generateDicomToken, getDicomImageUrlByToken } = await import('@/api/dicom')
    const tokenRes = await generateDicomToken(props.consultationId, imageId, props.userId, props.userName)
    const urlRes = await getDicomImageUrlByToken(tokenRes.data.token)
    return urlRes.data
  } catch (e) {
    throw e
  }
}

function updateViewportInfo(idx: number) {
  const el = viewportRefs.value[idx]
  if (!el) return
  const vp = getViewport(el)
  const viewport = viewports.value[idx]
  viewport.wc = vp.voi?.windowCenter
  viewport.ww = vp.voi?.windowWidth
  viewport.scale = vp.scale
}

function bindViewportEvents(el: HTMLElement, vpIdx: number) {
  el.addEventListener('cornerstoneimagerendered', () => {
    updateViewportInfo(vpIdx)
    if (vpIdx === activeViewportIndex.value) {
      syncViewportState(vpIdx)
    }
  })

  el.addEventListener('cornerstonetoolmeasurementadded', (evt: any) => {
    const detail = evt.detail || {}
    emitAnnotationSync(vpIdx, 'add', detail)
  })

  el.addEventListener('cornerstonetoolmeasurementmodified', (evt: any) => {
    const detail = evt.detail || {}
    emitAnnotationSync(vpIdx, 'update', detail)
  })

  el.addEventListener('cornerstonetooldataimagedrawn', () => {
    // ignore for now
  })
}

function emitAnnotationSync(vpIdx: number, operation: 'add' | 'update' | 'delete' | 'clear', detail: any) {
  const vp = viewports.value[vpIdx]
  if (!vp.imageId) return
  const payload: DicomAnnotationSyncPayload = {
    annotationId: detail.measurementData?.uuid || `anno_${Date.now()}`,
    imageId: vp.imageId,
    annotationType: detail.toolName || '',
    coordinates: extractCoordinates(detail),
    properties: detail.measurementData || {},
    operation,
    operatorId: props.userId,
    operatorName: props.userName || String(props.userId),
    timestamp: Date.now()
  }
  emit('annotation-sync', payload)
}

function extractCoordinates(detail: any): Array<{ x: number; y: number }> {
  const data = detail.measurementData
  if (!data) return []
  const coords: Array<{ x: number; y: number }> = []
  if (data.handles) {
    Object.values(data.handles).forEach((h: any) => {
      if (h && typeof h.x === 'number' && typeof h.y === 'number') {
        coords.push({ x: h.x, y: h.y })
      }
    })
  }
  return coords
}

function syncViewportState(vpIdx: number) {
  if (!syncViewports.value) return
  const el = viewportRefs.value[vpIdx]
  if (!el) return
  const vpState = getViewport(el)
  const vp = viewports.value[vpIdx]
  if (!vp.imageId) return

  const count = layoutCells.value
  for (let i = 0; i < count; i++) {
    if (i !== vpIdx && viewports.value[i].loaded) {
      const otherEl = viewportRefs.value[i]
      if (otherEl) {
        const otherVp = getViewport(otherEl)
        if (vpState.voi) {
          otherVp.voi = { ...vpState.voi }
        }
        otherVp.scale = vpState.scale
        otherVp.rotation = vpState.rotation
        otherVp.invert = vpState.invert
        setViewport(otherEl, otherVp)
      }
    }
  }

  const payload: DicomViewportSyncPayload = {
    consultationId: props.consultationId,
    imageId: vp.imageId,
    windowCenter: vpState.voi?.windowCenter,
    windowWidth: vpState.voi?.windowWidth,
    scale: vpState.scale,
    translation: vpState.translation,
    rotation: vpState.rotation,
    invert: vpState.invert,
    operatorId: props.userId,
    operatorName: props.userName || String(props.userId),
    timestamp: Date.now()
  }
  emit('viewport-sync', payload)
}

function onViewportCommand(cmd: string, idx: number) {
  const el = viewportRefs.value[idx]
  const vp = viewports.value[idx]
  switch (cmd) {
    case 'reset':
      if (el) resetViewport(el)
      break
    case 'clearAnnotations':
      if (el) clearAnnotationsForElement(el)
      emitAnnotationSync(idx, 'clear', {})
      break
    case 'unload':
      if (el && vp.loaded) {
        disableElement(el)
      }
      vp.loaded = false
      vp.imageId = undefined
      vp.imageInfo = undefined
      vp.imageUrl = undefined
      vp.selectedImageId = undefined
      break
    case 'changeImage':
      if (el && vp.loaded) {
        disableElement(el)
      }
      vp.loaded = false
      vp.imageId = undefined
      vp.imageInfo = undefined
      vp.selectedImageId = undefined
      break
  }
}

function initAllViewports() {
  const count = layoutCells.value
  for (let i = 0; i < count; i++) {
    const el = viewportRefs.value[i]
    if (el && viewports.value[i].loaded && !el.dataset.cornerstoneEnabled) {
      enableElement(el)
      el.dataset.cornerstoneEnabled = 'true'
    }
  }
}

function handleRemoteViewportSync(payload: DicomViewportSyncPayload) {
  if (payload.operatorId === props.userId) return
  const count = layoutCells.value
  for (let i = 0; i < count; i++) {
    const vp = viewports.value[i]
    if (vp.imageId === payload.imageId && vp.loaded) {
      const el = viewportRefs.value[i]
      if (el) {
        const vpState = getViewport(el)
        if (payload.windowCenter !== undefined && payload.windowWidth !== undefined) {
          vpState.voi = { windowCenter: payload.windowCenter, windowWidth: payload.windowWidth }
        }
        if (payload.scale !== undefined) vpState.scale = payload.scale
        if (payload.rotation !== undefined) vpState.rotation = payload.rotation
        if (payload.invert !== undefined) vpState.invert = payload.invert
        if (payload.translation) vpState.translation = payload.translation
        setViewport(el, vpState)
        updateViewportInfo(i)
      }
    }
  }
}

function handleRemoteAnnotationSync(payload: DicomAnnotationSyncPayload) {
  if (payload.operatorId === props.userId) return

  const toolNameMap: Record<string, string> = {
    POINT: 'ArrowAnnotate',
    LINE: 'Length',
    RECTANGLE: 'RectangleRoi',
    ELLIPSE: 'EllipticalRoi',
    ANGLE: 'Angle',
    TEXT: 'ArrowAnnotate',
    Length: 'Length',
    RectangleRoi: 'RectangleRoi',
    EllipticalRoi: 'EllipticalRoi',
    Angle: 'Angle',
    ArrowAnnotate: 'ArrowAnnotate'
  }

  if (payload.operation === 'clear') {
    const count = layoutCells.value
    for (let i = 0; i < count; i++) {
      const vp = viewports.value[i]
      if (vp.imageId === payload.imageId && vp.loaded) {
        const el = viewportRefs.value[i]
        if (el) {
          clearAnnotationsForElement(el)
        }
      }
    }
    return
  }

  if (payload.operation === 'delete') {
    const count = layoutCells.value
    for (let i = 0; i < count; i++) {
      const vp = viewports.value[i]
      if (vp.imageId === payload.imageId && vp.loaded) {
        const el = viewportRefs.value[i]
        if (el) {
          removeAnnotationById(el, payload.annotationId, toolNameMap[payload.annotationType])
        }
      }
    }
    return
  }

  if (payload.operation === 'add' || payload.operation === 'update') {
    const toolName = toolNameMap[payload.annotationType]
    if (!toolName || !payload.coordinates || payload.coordinates.length === 0) return

    const count = layoutCells.value
    for (let i = 0; i < count; i++) {
      const vp = viewports.value[i]
      if (vp.imageId === payload.imageId && vp.loaded) {
        const el = viewportRefs.value[i]
        if (el) {
          addRemoteAnnotation(el, toolName, payload)
        }
      }
    }
  }
}

function clearAnnotationsForElement(el: HTMLElement) {
  try {
    const toolNames = ['Length', 'RectangleRoi', 'EllipticalRoi', 'Angle', 'ArrowAnnotate']
    toolNames.forEach(name => {
      try {
        cornerstoneTools.clearToolState(el, name)
      } catch { /* ignore */ }
    })
    cornerstone.updateImage(el)
  } catch (e) {
    console.warn('清除标注失败', e)
  }
}

function removeAnnotationById(el: HTMLElement, annotationId: string, toolName?: string) {
  try {
    if (toolName) {
      const toolState = cornerstoneTools.getToolState(el, toolName)
      if (toolState && toolState.data) {
        toolState.data = toolState.data.filter((d: any) => d.uuid !== annotationId)
        cornerstone.updateImage(el)
      }
    } else {
      const toolNames = ['Length', 'RectangleRoi', 'EllipticalRoi', 'Angle', 'ArrowAnnotate']
      toolNames.forEach(name => {
        try {
          const toolState = cornerstoneTools.getToolState(el, name)
          if (toolState && toolState.data) {
            const before = toolState.data.length
            toolState.data = toolState.data.filter((d: any) => d.uuid !== annotationId)
            if (toolState.data.length < before) {
              cornerstone.updateImage(el)
            }
          }
        } catch { /* ignore */ }
      })
    }
  } catch (e) {
    console.warn('删除标注失败', e)
  }
}

function addRemoteAnnotation(el: HTMLElement, toolName: string, payload: DicomAnnotationSyncPayload) {
  try {
    const coords = payload.coordinates
    if (!coords || coords.length === 0) return

    const measurementData: any = {
      uuid: payload.annotationId,
      visible: true,
      active: false,
      color: payload.properties?.color || '#00ff00',
      handles: {}
    }

    if (toolName === 'Length') {
      measurementData.handles = {
        start: { x: coords[0]?.x || 0, y: coords[0]?.y || 0 },
        end: { x: coords[1]?.x || 0, y: coords[1]?.y || 0 }
      }
    } else if (toolName === 'RectangleRoi') {
      measurementData.handles = {
        start: { x: coords[0]?.x || 0, y: coords[0]?.y || 0 },
        end: { x: coords[1]?.x || 0, y: coords[1]?.y || 0 }
      }
    } else if (toolName === 'EllipticalRoi') {
      measurementData.handles = {
        start: { x: coords[0]?.x || 0, y: coords[0]?.y || 0 },
        end: { x: coords[1]?.x || 0, y: coords[1]?.y || 0 }
      }
    } else if (toolName === 'Angle') {
      measurementData.handles = {
        start: { x: coords[0]?.x || 0, y: coords[0]?.y || 0 },
        middle: { x: coords[1]?.x || 0, y: coords[1]?.y || 0 },
        end: { x: coords[2]?.x || 0, y: coords[2]?.y || 0 }
      }
    } else if (toolName === 'ArrowAnnotate') {
      measurementData.handles = {
        start: { x: coords[0]?.x || 0, y: coords[0]?.y || 0 },
        end: { x: coords[1]?.x || 0, y: coords[1]?.y || 0 },
        textBox: {}
      }
      measurementData.text = payload.properties?.text || ''
    }

    const existingState = cornerstoneTools.getToolState(el, toolName)
    if (existingState && existingState.data) {
      const existingIdx = existingState.data.findIndex((d: any) => d.uuid === payload.annotationId)
      if (existingIdx >= 0) {
        existingState.data[existingIdx] = measurementData
        cornerstone.updateImage(el)
        return
      }
    }

    cornerstoneTools.addToolState(el, toolName, measurementData)
    cornerstone.updateImage(el)
  } catch (e) {
    console.warn('添加远程标注失败', e)
  }
}

function handleResize() {
  const count = layoutCells.value
  for (let i = 0; i < count; i++) {
    const el = viewportRefs.value[i]
    if (el && viewports.value[i].loaded) {
      resize(el)
    }
  }
}

let resizeObserver: ResizeObserver | null = null

onMounted(() => {
  if (!cornerstoneInited) {
    initCornerstone()
    cornerstoneInited = true
  }
  nextTick(() => {
    initAllViewports()
    if (viewerContainerRef.value && typeof ResizeObserver !== 'undefined') {
      resizeObserver = new ResizeObserver(handleResize)
      resizeObserver.observe(viewerContainerRef.value)
    }
  })
})

onUnmounted(() => {
  resizeObserver?.disconnect()
  resizeObserver = null
  const count = viewports.value.length
  for (let i = 0; i < count; i++) {
    if (viewports.value[i].loaded) {
      const el = viewportRefs.value[i]
      if (el) disableElement(el)
    }
  }
})

defineExpose({
  handleRemoteViewportSync,
  handleRemoteAnnotationSync,
  handleResize
})
</script>

<style scoped>
.dicom-viewer {
  display: flex;
  flex-direction: column;
  height: 100%;
  background: #1a1a1a;
  color: #e0e0e0;
  overflow: hidden;
}

.dicom-toolbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 8px 12px;
  background: #2a2a2a;
  border-bottom: 1px solid #3a3a3a;
  flex-shrink: 0;
}

.tool-group {
  display: flex;
  align-items: center;
  gap: 4px;
}

.layout-group {
  display: flex;
  align-items: center;
  gap: 8px;
}

.layout-label {
  font-size: 13px;
  color: #a0a0a0;
}

.tool-icon {
  font-size: 12px;
  font-weight: bold;
  font-family: monospace;
}

.dicom-viewport-container {
  flex: 1;
  display: grid;
  gap: 2px;
  background: #0a0a0a;
  padding: 2px;
  min-height: 0;
}

.dicom-viewport-container.\31 x\31 {
  grid-template-columns: 1fr;
  grid-template-rows: 1fr;
}

.dicom-viewport-container.\32 x\31 {
  grid-template-columns: repeat(2, 1fr);
  grid-template-rows: 1fr;
}

.dicom-viewport-container.\31 x\32 {
  grid-template-columns: 1fr;
  grid-template-rows: repeat(2, 1fr);
}

.dicom-viewport-container.\32 x\32 {
  grid-template-columns: repeat(2, 1fr);
  grid-template-rows: repeat(2, 1fr);
}

.dicom-viewport {
  position: relative;
  background: #000;
  overflow: hidden;
  cursor: crosshair;
}

.dicom-viewport.active {
  outline: 2px solid #409eff;
  outline-offset: -2px;
  z-index: 1;
}

.cornerstone-element {
  position: absolute;
  inset: 0;
  width: 100%;
  height: 100%;
}

.viewport-overlay {
  position: absolute;
  inset: 0;
  pointer-events: none;
}

.dicom-viewport:not(.has-image) .viewport-overlay {
  pointer-events: auto;
  display: flex;
  align-items: center;
  justify-content: center;
}

.viewport-index {
  position: absolute;
  top: 4px;
  left: 6px;
  font-size: 12px;
  color: #888;
  font-family: monospace;
  background: rgba(0, 0, 0, 0.6);
  padding: 2px 6px;
  border-radius: 3px;
}

.viewport-info {
  position: absolute;
  bottom: 6px;
  left: 6px;
  right: 6px;
  font-size: 11px;
  color: #aaa;
  font-family: monospace;
  background: rgba(0, 0, 0, 0.5);
  padding: 6px 8px;
  border-radius: 4px;
  line-height: 1.6;
}

.info-line {
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.empty-placeholder {
  width: 100%;
  height: 100%;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-direction: column;
  background: rgba(20, 20, 20, 0.9);
}

.dropdown-trigger {
  position: absolute;
  inset: 0;
}

.dicom-statusbar {
  padding: 4px 12px;
  background: #2a2a2a;
  border-top: 1px solid #3a3a3a;
  font-size: 12px;
  color: #909399;
  flex-shrink: 0;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}
</style>
