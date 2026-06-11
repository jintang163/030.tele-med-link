import { Device, Transport, Producer, Consumer, types as MediasoupTypes } from 'mediasoup-client'
import { SignalingWebSocket } from './websocket'
import type { SignalingMessage, VideoResolution, NearestNodeVO, TurnServerVO, QualityAdviceVO, QualityReportDTO } from '@/types'
import {
  getNearestNode,
  getTurnConfig,
  getRouterCapabilities,
  createTransport,
  createProducer,
  createConsumer,
  reportQuality
} from '@/api/mediasoup'

const RESOLUTION_MAP: Record<VideoResolution, { width: number; height: number; bitrate: number; fps: number }> = {
  '1280x720': { width: 1280, height: 720, bitrate: 2500000, fps: 30 },
  '960x540': { width: 960, height: 540, bitrate: 1500000, fps: 25 },
  '640x360': { width: 640, height: 360, bitrate: 800000, fps: 20 }
}

export class MediasoupVideoRoom {
  private device: Device | null = null
  private sendTransport: Transport | null = null
  private recvTransport: Transport | null = null
  private producers: Map<string, Producer> = new Map()
  private consumers: Map<string, Consumer> = new Map()
  private localStream: MediaStream | null = null
  private remoteStream: MediaStream | null = null
  private localVideoElement: HTMLVideoElement | null = null
  private remoteVideoElement: HTMLVideoElement | null = null
  private nodeId: string = ''
  private roomId: string
  private userId: string = ''
  private nearestNode: NearestNodeVO | null = null
  private turnConfig: TurnServerVO | null = null
  private currentResolution: VideoResolution = '1280x720'
  private qualityMonitorTimer: ReturnType<typeof setInterval> | null = null
  private signaling: SignalingWebSocket | null = null
  private audioEnabled: boolean = true
  private videoEnabled: boolean = true

  constructor(roomId: string, localVideoElement: HTMLVideoElement, remoteVideoElement: HTMLVideoElement) {
    this.roomId = roomId
    this.localVideoElement = localVideoElement
    this.remoteVideoElement = remoteVideoElement
  }

  setSignaling(signaling: SignalingWebSocket) {
    this.signaling = signaling
  }

  async joinRoom(userId: string) {
    this.userId = userId

    try {
      const nearestNodeRes = await getNearestNode()
      this.nearestNode = nearestNodeRes.data
      this.nodeId = this.nearestNode.nodeId
      console.log('[Mediasoup] Got nearest node:', this.nearestNode)

      const turnConfigRes = await getTurnConfig()
      this.turnConfig = turnConfigRes.data
      console.log('[Mediasoup] Got TURN config:', this.turnConfig)

      const routerCapsRes = await getRouterCapabilities(this.nodeId)
      const routerRtpCapabilities = routerCapsRes.data
      console.log('[Mediasoup] Got router RTP capabilities')

      this.device = new Device()
      await this.device.load({ routerRtpCapabilities })
      console.log('[Mediasoup] Device loaded')

      await this.setupSendTransport()
      await this.setupRecvTransport()

      await this.startLocalMedia()

      await this.subscribeExistingProducers()

      this.startQualityMonitor()

      this.notifySignalingJoin()

      console.log('[Mediasoup] Joined room successfully')
    } catch (error) {
      console.error('[Mediasoup] Failed to join room:', error)
      throw error
    }
  }

  private async setupSendTransport() {
    const transportRes = await createTransport(this.nodeId, this.roomId, this.userId, 'send')
    const transportInfo = transportRes.data
    console.log('[Mediasoup] Created send transport:', transportInfo.id)

    this.sendTransport = this.device!.createSendTransport({
      id: transportInfo.id,
      iceParameters: transportInfo.iceParameters,
      iceCandidates: transportInfo.iceCandidates,
      dtlsParameters: transportInfo.dtlsParameters,
      iceServers: this.getRtcConfiguration().iceServers
    })

    this.sendTransport.on('connect', async ({ dtlsParameters }, callback, errback) => {
      try {
        callback()
      } catch (error) {
        errback(error as Error)
      }
    })

    this.sendTransport.on('connectionstatechange', (state) => {
      console.log('[Mediasoup] Send transport state:', state)
    })
  }

  private async setupRecvTransport() {
    const transportRes = await createTransport(this.nodeId, this.roomId, this.userId, 'recv')
    const transportInfo = transportRes.data
    console.log('[Mediasoup] Created recv transport:', transportInfo.id)

    this.recvTransport = this.device!.createRecvTransport({
      id: transportInfo.id,
      iceParameters: transportInfo.iceParameters,
      iceCandidates: transportInfo.iceCandidates,
      dtlsParameters: transportInfo.dtlsParameters,
      iceServers: this.getRtcConfiguration().iceServers
    })

    this.recvTransport.on('connect', async ({ dtlsParameters }, callback, errback) => {
      try {
        callback()
      } catch (error) {
        errback(error as Error)
      }
    })

    this.recvTransport.on('connectionstatechange', (state) => {
      console.log('[Mediasoup] Recv transport state:', state)
    })
  }

  private async startLocalMedia() {
    const resInfo = RESOLUTION_MAP[this.currentResolution]

    this.localStream = await navigator.mediaDevices.getUserMedia({
      video: {
        width: { ideal: resInfo.width, max: resInfo.width },
        height: { ideal: resInfo.height, max: resInfo.height },
        frameRate: { ideal: resInfo.fps, max: resInfo.fps }
      },
      audio: {
        echoCancellation: true,
        noiseSuppression: true,
        autoGainControl: true
      }
    })

    if (this.localVideoElement) {
      this.localVideoElement.srcObject = this.localStream
    }

    for (const track of this.localStream.getTracks()) {
      const kind = track.kind as 'audio' | 'video'
      const producer = await this.sendTransport!.produce({
        track,
        encodings: kind === 'video'
          ? [{ maxBitrate: resInfo.bitrate, scalabilityMode: 'S1T3' }]
          : undefined,
        codecOptions: kind === 'video'
          ? { videoGoogleStartBitrate: 1000 }
          : undefined
      })

      this.producers.set(kind, producer)
      console.log(`[Mediasoup] Created ${kind} producer:`, producer.id)

      await createProducer(
        this.nodeId,
        this.roomId,
        this.userId,
        this.sendTransport!.id,
        kind,
        producer.rtpParameters
      )
    }
  }

  private async subscribeExistingProducers() {
    console.log('[Mediasoup] Checking for existing producers...')
  }

  private async consumeProducer(producerId: string, kind: 'audio' | 'video', producerUserId: string) {
    if (!this.device || !this.recvTransport) return
    if (!this.device.canConsume({ producerId, rtpCapabilities: this.device.rtpCapabilities })) {
      console.warn('[Mediasoup] Cannot consume producer:', producerId)
      return
    }

    const consumerRes = await createConsumer(
      this.nodeId,
      this.roomId,
      this.userId,
      this.recvTransport.id,
      producerId,
      this.device.rtpCapabilities
    )
    const consumerInfo = consumerRes.data

    const consumer = await this.recvTransport.consume({
      id: consumerInfo.id,
      producerId: consumerInfo.producerId,
      kind: consumerInfo.kind,
      rtpParameters: consumerInfo.rtpParameters
    })

    this.consumers.set(producerId, consumer)
    console.log(`[Mediasoup] Created ${kind} consumer for producer ${producerId}, user ${producerUserId}`)

    if (!this.remoteStream) {
      this.remoteStream = new MediaStream()
      if (this.remoteVideoElement) {
        this.remoteVideoElement.srcObject = this.remoteStream
      }
    }

    this.remoteStream.addTrack(consumer.track)
  }

  private startQualityMonitor() {
    if (this.qualityMonitorTimer) return

    this.qualityMonitorTimer = setInterval(async () => {
      try {
        await this.monitorQuality()
      } catch (error) {
        console.error('[Mediasoup] Quality monitor error:', error)
      }
    }, 3000)
  }

  private stopQualityMonitor() {
    if (this.qualityMonitorTimer) {
      clearInterval(this.qualityMonitorTimer)
      this.qualityMonitorTimer = null
    }
  }

  private async monitorQuality() {
    const stats = await this.getStats()
    if (!stats) return

    let packetLostRate = 0
    let jitter = 0
    let roundTripTime = 0
    let bitrate = 0

    for (const report of stats.values()) {
      if (report.type === 'outbound-rtp') {
        if (report.packetsLost != null && report.packetsSent != null) {
          packetLostRate = report.packetsSent > 0
            ? report.packetsLost / report.packetsSent
            : 0
        }
        if (report.jitter != null) {
          jitter = report.jitter
        }
        if (report.roundTripTime != null) {
          roundTripTime = report.roundTripTime
        }
        if (report.bitrate != null) {
          bitrate = report.bitrate
        }
      }
    }

    const dto: QualityReportDTO = {
      userId: this.userId,
      consultationId: this.roomId,
      transportId: this.sendTransport?.id || '',
      kind: 'video',
      packetLostRate,
      jitter,
      roundTripTime,
      bitrate,
      resolution: this.currentResolution
    }

    const adviceRes = await reportQuality(dto)
    const advice = adviceRes.data

    if (advice.shouldDowngrade) {
      console.log('[Mediasoup] Applying quality advice:', advice)
      await this.applyQualityAdvice(advice)
    }
  }

  async applyQualityAdvice(advice: QualityAdviceVO) {
    const targetResolution = advice.targetResolution
    if (!RESOLUTION_MAP[targetResolution]) return

    this.currentResolution = targetResolution
    const resInfo = RESOLUTION_MAP[targetResolution]

    const videoProducer = this.producers.get('video')
    if (videoProducer) {
      const track = videoProducer.track
      if (track) {
        const constraints: MediaTrackConstraints = {
          width: { ideal: resInfo.width, max: resInfo.width },
          height: { ideal: resInfo.height, max: resInfo.height },
          frameRate: { ideal: resInfo.fps, max: resInfo.fps }
        }
        try {
          await track.applyConstraints(constraints)
          console.log(`[Mediasoup] Applied constraints: ${resInfo.width}x${resInfo.height}`)
        } catch (error) {
          console.error('[Mediasoup] Failed to apply constraints:', error)
        }
      }

      try {
        await videoProducer.setMaxSpatialLayer(0)
      } catch (e) {
        // ignore
      }
    }

    if (this.localStream) {
      for (const track of this.localStream.getVideoTracks()) {
        const sender = this.sendTransport?.handler?.rtpSenderByKind?.get('video')
        if (sender) {
          const params = sender.getParameters()
          if (params.encodings && params.encodings.length > 0) {
            params.encodings[0].maxBitrate = advice.targetBitrate || resInfo.bitrate
            try {
              await sender.setParameters(params)
            } catch (e) {
              // ignore
            }
          }
        }
      }
    }
  }

  getRtcConfiguration(): RTCConfiguration {
    const iceServers: RTCIceServer[] = [
      { urls: 'stun:stun.l.google.com:19302' },
      { urls: 'stun:stun1.l.google.com:19302' }
    ]

    if (this.turnConfig) {
      iceServers.push({
        urls: this.turnConfig.urls,
        username: this.turnConfig.username,
        credential: this.turnConfig.credential
      })
    }

    return {
      iceServers,
      bundlePolicy: 'max-bundle',
      rtcpMuxPolicy: 'require'
    }
  }

  toggleAudio(): boolean {
    this.audioEnabled = !this.audioEnabled
    this.localStream?.getAudioTracks().forEach((track) => {
      track.enabled = this.audioEnabled
    })

    const audioProducer = this.producers.get('audio')
    if (audioProducer) {
      if (this.audioEnabled) {
        audioProducer.resume()
      } else {
        audioProducer.pause()
      }
    }

    return this.audioEnabled
  }

  toggleVideo(): boolean {
    this.videoEnabled = !this.videoEnabled
    this.localStream?.getVideoTracks().forEach((track) => {
      track.enabled = this.videoEnabled
    })

    const videoProducer = this.producers.get('video')
    if (videoProducer) {
      if (this.videoEnabled) {
        videoProducer.resume()
      } else {
        videoProducer.pause()
      }
    }

    return this.videoEnabled
  }

  getStats(): Promise<RTCStatsReport | null> {
    const producer = this.producers.get('video')
    if (!producer) return Promise.resolve(null)
    return producer.getStats() as Promise<RTCStatsReport>
  }

  leaveRoom() {
    this.stopQualityMonitor()

    this.localStream?.getTracks().forEach((track) => track.stop())
    this.localStream = null

    for (const consumer of this.consumers.values()) {
      try {
        consumer.close()
      } catch (e) {
        // ignore
      }
    }
    this.consumers.clear()
    this.remoteStream = null

    for (const producer of this.producers.values()) {
      try {
        producer.close()
      } catch (e) {
        // ignore
      }
    }
    this.producers.clear()

    try {
      this.sendTransport?.close()
    } catch (e) {
      // ignore
    }
    this.sendTransport = null

    try {
      this.recvTransport?.close()
    } catch (e) {
      // ignore
    }
    this.recvTransport = null

    this.device = null

    if (this.localVideoElement) this.localVideoElement.srcObject = null
    if (this.remoteVideoElement) this.remoteVideoElement.srcObject = null

    this.signaling?.send({
      type: 'leave',
      from: this.userId,
      to: '',
      roomId: this.roomId,
      timestamp: Date.now()
    })
    this.signaling?.disconnect()
    this.signaling = null

    console.log('[Mediasoup] Left room')
  }

  handleSignalingMessage(message: SignalingMessage) {
    switch (message.type) {
      case 'mediasoup-producer-added': {
        const { producerId, kind, userId: producerUserId } = message.payload
        if (producerUserId !== this.userId) {
          this.consumeProducer(producerId, kind as 'audio' | 'video', producerUserId)
        }
        break
      }
      case 'mediasoup-producer-removed': {
        const { producerId } = message.payload
        const consumer = this.consumers.get(producerId)
        if (consumer) {
          consumer.close()
          this.consumers.delete(producerId)
          console.log('[Mediasoup] Removed consumer for producer:', producerId)
        }
        break
      }
    }
  }

  private notifySignalingJoin() {
    this.signaling?.send({
      type: 'join',
      from: this.userId,
      to: '',
      roomId: this.roomId,
      timestamp: Date.now()
    })
  }
}
