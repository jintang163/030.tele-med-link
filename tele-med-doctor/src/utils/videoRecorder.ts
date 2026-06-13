import { uploadSegment, getEncryptionKey, stopRecording } from '@/api/video'

const DEFAULT_SEGMENT_INTERVAL = 5 * 60 * 1000
const WEBM_MIME_TYPE = 'video/webm;codecs=vp9,opus'

async function aesEncryptBlob(blob: Blob, rawKey: Uint8Array, iv: Uint8Array): Promise<Blob> {
  const buffer = await blob.arrayBuffer()
  const cryptoKey = await crypto.subtle.importKey(
    'raw',
    rawKey,
    { name: 'AES-CBC' },
    false,
    ['encrypt']
  )
  const encrypted = await crypto.subtle.encrypt(
    { name: 'AES-CBC', iv },
    cryptoKey,
    buffer
  )
  return new Blob([encrypted], { type: 'application/octet-stream' })
}

async function computeChecksum(blob: Blob): Promise<string> {
  const buffer = await blob.arrayBuffer()
  const hashBuffer = await crypto.subtle.digest('SHA-256', buffer)
  const hashArray = Array.from(new Uint8Array(hashBuffer))
  return hashArray.map(b => b.toString(16).padStart(2, '0')).join('')
}

export class VideoRecorder {
  private mediaRecorder: MediaRecorder | null = null
  private stream: MediaStream | null = null
  private segmentTimer: ReturnType<typeof setInterval> | null = null
  private segmentIndex = 0
  private recordingId: number | null = null
  private consultationId: number
  private encryptionKey: Uint8Array | null = null
  private encryptionIv: Uint8Array | null = null
  private encryptionKeyBase64: string | null = null
  private encryptionIvBase64: string | null = null
  private segmentDuration: number
  private segmentStartTime = 0
  private isRecording = false
  private watermarkText = ''
  private onSegmentUploaded?: (index: number) => void
  private onError?: (error: Error) => void
  private pendingChunks: Blob[] = []

  constructor(
    consultationId: number,
    options?: {
      segmentDuration?: number
      watermarkText?: string
      onSegmentUploaded?: (index: number) => void
      onError?: (error: Error) => void
    }
  ) {
    this.consultationId = consultationId
    this.segmentDuration = options?.segmentDuration ?? DEFAULT_SEGMENT_INTERVAL
    this.watermarkText = options?.watermarkText ?? '录制中'
    this.onSegmentUploaded = options?.onSegmentUploaded
    this.onError = options?.onError
  }

  getIsActive(): boolean {
    return this.isRecording
  }

  getSegmentIndex(): number {
    return this.segmentIndex
  }

  getRecordingId(): number | null {
    return this.recordingId
  }

  getWatermarkText(): string {
    return this.watermarkText
  }

  async start(recordingId: number, stream: MediaStream): Promise<void> {
    if (this.isRecording) {
      throw new Error('录制已在进行中')
    }

    this.recordingId = recordingId
    this.stream = stream
    this.segmentIndex = 0

    try {
      const keyRes = await getEncryptionKey(this.consultationId)
      this.encryptionKeyBase64 = keyRes.data.encryptionKey
      this.encryptionIvBase64 = keyRes.data.encryptionIv
      this.encryptionKey = Uint8Array.from(atob(this.encryptionKeyBase64), c => c.charCodeAt(0))
      this.encryptionIv = Uint8Array.from(atob(this.encryptionIvBase64), c => c.charCodeAt(0))
    } catch (e) {
      throw new Error('获取加密密钥失败')
    }

    const mimeType = MediaRecorder.isTypeSupported(WEBM_MIME_TYPE)
      ? WEBM_MIME_TYPE
      : 'video/webm'

    this.mediaRecorder = new MediaRecorder(stream, {
      mimeType,
      videoBitsPerSecond: 2500000
    })

    this.mediaRecorder.ondataavailable = (event) => {
      if (event.data && event.data.size > 0) {
        this.pendingChunks.push(event.data)
      }
    }

    this.mediaRecorder.start(1000)
    this.isRecording = true
    this.segmentStartTime = Date.now()

    this.segmentTimer = setInterval(() => {
      this.finalizeCurrentSegment()
    }, this.segmentDuration)
  }

  private async finalizeCurrentSegment(): Promise<void> {
    if (!this.mediaRecorder || !this.isRecording || !this.recordingId) return

    const currentIndex = this.segmentIndex
    const chunks = [...this.pendingChunks]
    this.pendingChunks = []

    if (chunks.length === 0) return

    const webmBlob = new Blob(chunks, { type: 'video/webm' })
    const duration = Math.round((Date.now() - this.segmentStartTime) / 1000)
    this.segmentStartTime = Date.now()
    this.segmentIndex++

    this.uploadSegmentAsync(webmBlob, currentIndex, duration)
  }

  private async uploadSegmentAsync(webmBlob: Blob, segmentIndex: number, duration: number): Promise<void> {
    if (!this.recordingId) return

    try {
      let uploadBlob: Blob
      let ivBase64: string

      if (this.encryptionKey && this.encryptionIv) {
        const iv = new Uint8Array(16)
        crypto.getRandomValues(iv)
        ivBase64 = btoa(String.fromCharCode(...iv))

        uploadBlob = await aesEncryptBlob(webmBlob, this.encryptionKey, iv)
      } else {
        ivBase64 = this.encryptionIvBase64 ?? ''
        uploadBlob = webmBlob
      }

      const checksum = await computeChecksum(webmBlob)
      const fileName = `segment_${segmentIndex}.webm`

      await uploadSegment(
        uploadBlob,
        this.recordingId,
        this.consultationId,
        segmentIndex,
        fileName,
        duration,
        ivBase64,
        checksum
      )

      this.onSegmentUploaded?.(segmentIndex)
    } catch (e) {
      this.onError?.(e instanceof Error ? e : new Error('片段上传失败'))
    }
  }

  async stop(): Promise<void> {
    if (!this.isRecording) return

    this.isRecording = false

    if (this.segmentTimer) {
      clearInterval(this.segmentTimer)
      this.segmentTimer = null
    }

    if (this.mediaRecorder && this.mediaRecorder.state !== 'inactive') {
      this.mediaRecorder.stop()
    }

    await new Promise<void>(resolve => {
      if (this.mediaRecorder) {
        const originalOnStop = this.mediaRecorder.onstop
        this.mediaRecorder.onstop = () => {
          originalOnStop?.call(this.mediaRecorder!, new Event('stop'))
          resolve()
        }
        setTimeout(resolve, 3000)
      } else {
        resolve()
      }
    })

    if (this.pendingChunks.length > 0 && this.recordingId) {
      const chunks = [...this.pendingChunks]
      this.pendingChunks = []
      const webmBlob = new Blob(chunks, { type: 'video/webm' })
      const duration = Math.round((Date.now() - this.segmentStartTime) / 1000)
      await this.uploadSegmentAsync(webmBlob, this.segmentIndex, duration)
    }

    try {
      await stopRecording(this.consultationId)
    } catch (e) {
      this.onError?.(e instanceof Error ? e : new Error('停止录制失败'))
    }

    this.mediaRecorder = null
    this.stream = null
    this.encryptionKey = null
    this.encryptionIv = null
  }

  destroy(): void {
    if (this.segmentTimer) {
      clearInterval(this.segmentTimer)
      this.segmentTimer = null
    }
    if (this.mediaRecorder && this.mediaRecorder.state !== 'inactive') {
      this.mediaRecorder.stop()
    }
    this.isRecording = false
    this.mediaRecorder = null
    this.stream = null
    this.pendingChunks = []
  }
}
