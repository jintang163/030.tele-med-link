import { SignalingWebSocket } from './websocket'
import type { SignalingMessage } from '@/types'

const JANUS_URL = 'http://localhost:8088/janus'
const JANUS_API_SECRET = 'janusrocks'

interface JanusResponse {
  janus: string
  session_id: number
  handle_id?: number
  transaction?: string
  plugindata?: { plugin: string; data: any }
  jsep?: RTCSessionDescriptionInit
  sender?: number
  data?: any
}

export class JanusVideoRoom {
  private roomId: string
  private roomNumericId: number
  private localVideoElement: HTMLVideoElement | null = null
  private remoteVideoElement: HTMLVideoElement | null = null
  private signaling: SignalingWebSocket | null = null
  private localStream: MediaStream | null = null
  private audioEnabled = true
  private videoEnabled = true
  private userId = ''

  private sessionId: number | null = null
  private publisherHandle: number | null = null
  private subscriberHandle: number | null = null

  private publisherPC: RTCPeerConnection | null = null
  private subscriberPC: RTCPeerConnection | null = null
  private remoteStream: MediaStream | null = null

  private pollingTimer: ReturnType<typeof setInterval> | null = null
  private janusTransaction = 0

  private readonly peerConfig: RTCConfiguration = {
    iceServers: [
      { urls: 'stun:stun.l.google.com:19302' },
      { urls: 'stun:stun1.l.google.com:19302' }
    ],
    bundlePolicy: 'max-bundle',
    rtcpMuxPolicy: 'require'
  }

  constructor(roomId: string, localVideoElement: HTMLVideoElement, remoteVideoElement: HTMLVideoElement) {
    this.roomId = roomId
    this.roomNumericId = this.hashRoomId(roomId)
    this.localVideoElement = localVideoElement
    this.remoteVideoElement = remoteVideoElement
  }

  setSignaling(signaling: SignalingWebSocket) {
    this.signaling = signaling
  }

  private hashRoomId(roomStr: string): number {
    let hash = 0
    for (let i = 0; i < roomStr.length; i++) {
      const char = roomStr.charCodeAt(i)
      hash = ((hash << 5) - hash) + char
      hash = hash & hash
    }
    return Math.abs(hash) % 2147483647 || 1234
  }

  private nextTransaction(): string {
    this.janusTransaction++
    return `tx_${Date.now()}_${this.janusTransaction}`
  }

  private async janusRequest(body: any): Promise<JanusResponse> {
    const tx = this.nextTransaction()
    const payload = { ...body, transaction: tx, apisecret: JANUS_API_SECRET }

    let url = JANUS_URL
    if (this.sessionId != null) {
      url += `/${this.sessionId}`
      if (payload.handle_id != null || this.publisherHandle != null) {
        const handleId = payload.handle_id || this.publisherHandle
        if (handleId) {
          url += `/${handleId}`
          delete payload.handle_id
        }
      }
    }

    try {
      const response = await fetch(url, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify(payload)
      })
      const data = await response.json() as JanusResponse
      if (this.sessionId == null && data.session_id) {
        this.sessionId = data.session_id
      }
      return data
    } catch (error) {
      console.error('Janus request failed:', error)
      throw error
    }
  }

  private startLongPolling() {
    if (this.pollingTimer || this.sessionId == null) return
    this.pollingTimer = setInterval(async () => {
      if (this.sessionId == null) return
      try {
        const url = `${JANUS_URL}/${this.sessionId}?apisecret=${JANUS_API_SECRET}&rid=${Date.now()}`
        const response = await fetch(url, { method: 'GET' })
        const result = await response.json() as JanusResponse | JanusResponse[]
        const events = Array.isArray(result) ? result : [result]
        for (const evt of events) {
          this.handleJanusEvent(evt)
        }
      } catch (e) {
        // ignore polling errors
      }
    }, 300)
  }

  private stopLongPolling() {
    if (this.pollingTimer) {
      clearInterval(this.pollingTimer)
      this.pollingTimer = null
    }
  }

  private handleJanusEvent(evt: JanusResponse) {
    if (!evt || !evt.janus) return
    const type = evt.janus

    if (type === 'event' && evt.plugindata?.data?.videoroom === 'event') {
      const roomEvent = evt.plugindata.data
      if (roomEvent.publishers && roomEvent.publishers.length > 0) {
        const firstPublisher = roomEvent.publishers[0]
        if (firstPublisher && this.subscriberHandle == null) {
          this.setupSubscriber(firstPublisher.id as number)
        }
      }
      if (roomEvent.leaving && this.subscriberHandle != null) {
        console.log('Publisher left, feed id:', roomEvent.leaving)
      }
    }

    if (type === 'event' && evt.jsep && evt.handle_id === this.subscriberHandle) {
      if (this.subscriberPC && evt.jsep.type === 'offer') {
        this.handleSubscriberOffer(evt.jsep)
      }
    }

    if (type === 'ack' && evt.jsep && this.publisherPC && this.publisherPC.signalingState === 'have-local-offer') {
      this.publisherPC.setRemoteDescription(
        new RTCSessionDescription({ type: 'answer', sdp: evt.jsep.sdp })
      ).catch(e => console.error('publisher setRemoteDescription failed:', e))
    }
  }

  async joinRoom(userId: string) {
    this.userId = userId

    try {
      this.localStream = await navigator.mediaDevices.getUserMedia({
        video: {
          width: { ideal: 640, max: 1280 },
          height: { ideal: 480, max: 720 },
          frameRate: { ideal: 30, max: 30 }
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

      await this.createJanusSession()
      await this.attachPublisher()
      await this.joinRoomAsPublisher()

      this.startLongPolling()
      this.notifySignalingJoin()

    } catch (error) {
      console.error('Failed to join Janus room, fallback to P2P mode:', error)
      this.fallbackToP2P(userId)
    }
  }

  private async createJanusSession() {
    const resp = await this.janusRequest({ janus: 'create' })
    if (!resp.session_id) {
      throw new Error('Failed to create Janus session')
    }
    console.log('Janus session created:', this.sessionId)
  }

  private async attachPublisher() {
    const resp = await this.janusRequest({
      janus: 'attach',
      plugin: 'janus.plugin.videoroom'
    })
    if (!resp.data || !resp.data.id) {
      throw new Error('Failed to attach publisher handle')
    }
    this.publisherHandle = resp.data.id as number
    console.log('Publisher handle attached:', this.publisherHandle)
  }

  private async joinRoomAsPublisher() {
    await this.janusRequest({
      handle_id: this.publisherHandle,
      janus: 'message',
      body: {
        request: 'join',
        ptype: 'publisher',
        room: this.roomNumericId,
        display: `doctor_${this.userId}`
      }
    })
    console.log('Joined room as publisher:', this.roomNumericId)

    this.publisherPC = new RTCPeerConnection(this.peerConfig)

    this.publisherPC.oniceconnectionstatechange = () => {
      console.log('Publisher ICE state:', this.publisherPC?.iceConnectionState)
    }

    this.localStream?.getTracks().forEach(track => {
      const sender = this.publisherPC?.addTrack(track, this.localStream!)
      if (sender && track.kind === 'video') {
        const params = sender.getParameters()
        if (params.encodings && params.encodings.length > 0) {
          params.encodings[0].maxBitrate = 800000
        } else {
          params.encodings = [{ maxBitrate: 800000 }]
        }
        params.degradationPreference = 'maintain-framerate'
        sender.setParameters(params).catch(() => {})
      }
    })

    const offer = await this.publisherPC.createOffer({
      offerToReceiveAudio: false,
      offerToReceiveVideo: false
    })
    const sdp = this.enhanceSdpForLowLatency(offer.sdp || '')
    await this.publisherPC.setLocalDescription({ type: 'offer', sdp })

    await this.janusRequest({
      handle_id: this.publisherHandle,
      janus: 'message',
      body: {
        request: 'configure',
        audio: true,
        video: true
      },
      jsep: { type: 'offer', sdp }
    })
  }

  private async setupSubscriber(feedId: number) {
    try {
      const resp = await this.janusRequest({
        janus: 'attach',
        plugin: 'janus.plugin.videoroom'
      })
      if (!resp.data || !resp.data.id) return
      this.subscriberHandle = resp.data.id as number
      console.log('Subscriber handle attached:', this.subscriberHandle)

      this.subscriberPC = new RTCPeerConnection(this.peerConfig)

      this.subscriberPC.ontrack = (event) => {
        if (!this.remoteStream) {
          this.remoteStream = new MediaStream()
          if (this.remoteVideoElement) {
            this.remoteVideoElement.srcObject = this.remoteStream
          }
        }
        event.streams[0]?.getTracks().forEach(track => {
          this.remoteStream?.addTrack(track)
        })
      }

      this.subscriberPC.oniceconnectionstatechange = () => {
        console.log('Subscriber ICE state:', this.subscriberPC?.iceConnectionState)
      }

      await this.janusRequest({
        handle_id: this.subscriberHandle,
        janus: 'message',
        body: {
          request: 'join',
          ptype: 'listener',
          room: this.roomNumericId,
          feed: feedId
        }
      })
      console.log('Joined as subscriber, feed:', feedId)

    } catch (e) {
      console.error('Setup subscriber failed:', e)
    }
  }

  private async handleSubscriberOffer(jsep: RTCSessionDescriptionInit) {
    if (!this.subscriberPC) return
    try {
      const sdp = this.enhanceSdpForLowLatency(jsep.sdp || '')
      await this.subscriberPC.setRemoteDescription(
        new RTCSessionDescription({ type: 'offer', sdp })
      )
      const answer = await this.subscriberPC.createAnswer()
      const answerSdp = this.enhanceSdpForLowLatency(answer.sdp || '')
      await this.subscriberPC.setLocalDescription({ type: 'answer', sdp: answerSdp })

      await this.janusRequest({
        handle_id: this.subscriberHandle,
        janus: 'message',
        body: { request: 'start' },
        jsep: { type: 'answer', sdp: answerSdp }
      })
    } catch (e) {
      console.error('Handle subscriber offer failed:', e)
    }
  }

  private enhanceSdpForLowLatency(sdp: string): string {
    let result = sdp
    result = result.replace(/useinbandfec=1/g, 'useinbandfec=1; usedtx=1')
    if (!result.includes('minptime=10')) {
      result = result.replace(
        /a=fmtp:111 /g,
        'a=fmtp:111 minptime=10; useinbandfec=1; usedtx=1; stereo=0; '
      )
    }
    return result
  }

  private fallbackToP2P(userId: string) {
    console.warn('Using P2P fallback mode for video')

    this.publisherPC = new RTCPeerConnection(this.peerConfig)
    const pc = this.publisherPC

    pc.onicecandidate = (event) => {
      if (event.candidate && this.signaling) {
        this.signaling.send({
          type: 'ice-candidate',
          from: userId,
          to: '',
          roomId: this.roomId,
          payload: { candidate: event.candidate.toJSON() },
          timestamp: Date.now()
        })
      }
    }

    pc.oniceconnectionstatechange = () => {
      console.log('P2P ICE state:', pc.iceConnectionState)
    }

    pc.ontrack = (event) => {
      if (this.remoteVideoElement && event.streams[0]) {
        this.remoteVideoElement.srcObject = event.streams[0]
      }
    }

    this.localStream?.getTracks().forEach(track => {
      const sender = pc.addTrack(track, this.localStream!)
      if (sender && track.kind === 'video') {
        const params = sender.getParameters()
        if (params.encodings && params.encodings.length > 0) {
          params.encodings[0].maxBitrate = 800000
        } else {
          params.encodings = [{ maxBitrate: 800000 }]
        }
        params.degradationPreference = 'maintain-framerate'
        sender.setParameters(params).catch(() => {})
      }
    })

    this.signaling?.send({
      type: 'join',
      from: userId,
      to: '',
      roomId: this.roomId,
      timestamp: Date.now()
    })
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

  async createOffer(remoteUserId: string) {
    if (this.sessionId != null && this.publisherHandle != null) {
      return
    }
    if (!this.publisherPC) return
    try {
      const offer = await this.publisherPC.createOffer({
        offerToReceiveAudio: true,
        offerToReceiveVideo: true
      })
      const sdp = this.enhanceSdpForLowLatency(offer.sdp || '')
      await this.publisherPC.setLocalDescription({ type: 'offer', sdp })
      this.signaling?.send({
        type: 'offer',
        from: this.userId,
        to: remoteUserId,
        roomId: this.roomId,
        payload: { sdp },
        timestamp: Date.now()
      })
    } catch (error) {
      console.error('Failed to create P2P offer', error)
    }
  }

  async handleAnswer(sdp: string) {
    const pc = this.subscriberPC || this.publisherPC
    if (!pc) return
    try {
      const enhancedSdp = this.enhanceSdpForLowLatency(sdp)
      await pc.setRemoteDescription(
        new RTCSessionDescription({ type: 'answer', sdp: enhancedSdp })
      )
    } catch (error) {
      console.error('Failed to handle answer', error)
    }
  }

  async handleOffer(sdp: string, fromUserId: string) {
    if (this.sessionId != null) {
      return
    }
    const pc = this.publisherPC
    if (!pc) return
    try {
      const enhancedSdp = this.enhanceSdpForLowLatency(sdp)
      await pc.setRemoteDescription(
        new RTCSessionDescription({ type: 'offer', sdp: enhancedSdp })
      )
      const answer = await pc.createAnswer()
      const answerSdp = this.enhanceSdpForLowLatency(answer.sdp || '')
      await pc.setLocalDescription({ type: 'answer', sdp: answerSdp })
      this.signaling?.send({
        type: 'answer',
        from: this.userId,
        to: fromUserId,
        roomId: this.roomId,
        payload: { sdp: answerSdp },
        timestamp: Date.now()
      })
    } catch (error) {
      console.error('Failed to handle offer', error)
    }
  }

  async handleIceCandidate(candidate: RTCIceCandidateInit) {
    const pc = this.subscriberPC || this.publisherPC
    if (!pc) return
    try {
      await pc.addIceCandidate(new RTCIceCandidate(candidate))
    } catch (error) {
      console.error('Failed to add ICE candidate', error)
    }
  }

  toggleAudio(): boolean {
    this.audioEnabled = !this.audioEnabled
    this.localStream?.getAudioTracks().forEach(track => {
      track.enabled = this.audioEnabled
    })
    if (this.publisherHandle != null && this.sessionId != null) {
      this.janusRequest({
        handle_id: this.publisherHandle,
        janus: 'message',
        body: {
          request: 'configure',
          audio: this.audioEnabled
        }
      }).catch(() => {})
    }
    return this.audioEnabled
  }

  toggleVideo(): boolean {
    this.videoEnabled = !this.videoEnabled
    this.localStream?.getVideoTracks().forEach(track => {
      track.enabled = this.videoEnabled
    })
    if (this.publisherHandle != null && this.sessionId != null) {
      this.janusRequest({
        handle_id: this.publisherHandle,
        janus: 'message',
        body: {
          request: 'configure',
          video: this.videoEnabled
        }
      }).catch(() => {})
    }
    return this.videoEnabled
  }

  getStats(): Promise<RTCStatsReport | null> {
    const pc = this.subscriberPC || this.publisherPC
    if (!pc) return Promise.resolve(null)
    return pc.getStats()
  }

  leaveRoom() {
    this.stopLongPolling()

    this.localStream?.getTracks().forEach(track => track.stop())
    this.localStream = null

    this.subscriberPC?.close()
    this.subscriberPC = null
    this.publisherPC?.close()
    this.publisherPC = null

    if (this.localVideoElement) this.localVideoElement.srcObject = null
    if (this.remoteVideoElement) this.remoteVideoElement.srcObject = null

    const cleanup = async () => {
      if (this.sessionId == null) return
      try {
        if (this.subscriberHandle != null) {
          await this.janusRequest({
            handle_id: this.subscriberHandle,
            janus: 'detach'
          }).catch(() => {})
        }
        if (this.publisherHandle != null) {
          await this.janusRequest({
            handle_id: this.publisherHandle,
            janus: 'detach'
          }).catch(() => {})
        }
        await this.janusRequest({
          janus: 'destroy'
        }).catch(() => {})
      } catch (e) {
        // ignore cleanup errors
      } finally {
        this.sessionId = null
        this.publisherHandle = null
        this.subscriberHandle = null
      }
    }
    cleanup()

    this.signaling?.send({
      type: 'leave',
      from: this.userId,
      to: '',
      roomId: this.roomId,
      timestamp: Date.now()
    })
    this.signaling?.disconnect()
    this.signaling = null
  }

  handleSignalingMessage(message: SignalingMessage, userId: string) {
    switch (message.type) {
      case 'offer':
        this.handleOffer(message.payload.sdp, message.from)
        break
      case 'answer':
        this.handleAnswer(message.payload.sdp)
        break
      case 'ice-candidate':
        this.handleIceCandidate(message.payload.candidate)
        break
      case 'user-joined':
        if (message.from !== userId && this.sessionId == null) {
          this.createOffer(message.from)
        }
        break
    }
  }
}
