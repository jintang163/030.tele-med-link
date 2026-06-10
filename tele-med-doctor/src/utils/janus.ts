import { SignalingWebSocket } from './websocket'
import type { SignalingMessage } from '@/types'

export class JanusVideoRoom {
  private roomId: string
  private localVideoElement: HTMLVideoElement | null = null
  private remoteVideoElement: HTMLVideoElement | null = null
  private peerConnection: RTCPeerConnection | null = null
  private signaling: SignalingWebSocket | null = null
  private localStream: MediaStream | null = null
  private audioEnabled = true
  private videoEnabled = true
  private userId = ''

  private readonly config: RTCConfiguration = {
    iceServers: [
      { urls: 'stun:stun.l.google.com:19302' },
      { urls: 'stun:stun1.l.google.com:19302' },
      { urls: 'stun:stun2.l.google.com:19302' }
    ],
    iceTransportPolicy: 'all',
    bundlePolicy: 'max-bundle',
    rtcpMuxPolicy: 'require'
  }

  private readonly videoConstraints = {
    width: { ideal: 640, max: 1280 },
    height: { ideal: 480, max: 720 },
    frameRate: { ideal: 30, max: 30 }
  }

  constructor(roomId: string, localVideoElement: HTMLVideoElement, remoteVideoElement: HTMLVideoElement) {
    this.roomId = roomId
    this.localVideoElement = localVideoElement
    this.remoteVideoElement = remoteVideoElement
  }

  setSignaling(signaling: SignalingWebSocket) {
    this.signaling = signaling
  }

  private applyLowLatencyConfig(pc: RTCPeerConnection) {
    const transceiver = pc.getTransceivers().find(t => t.receiver.track.kind === 'video')
    if (transceiver && transceiver.receiver) {
      try {
        const params = transceiver.receiver.getParameters()
        if (params.headerExtensions) {
          const hasPlayoutDelay = params.headerExtensions.some(
            ext => ext.uri === 'http://www.webrtc.org/experiments/rtp-hdrext/playout-delay'
          )
          if (!hasPlayoutDelay) {
            params.headerExtensions.push({
              uri: 'http://www.webrtc.org/experiments/rtp-hdrext/playout-delay',
              id: 12
            })
          }
        }
      } catch (e) {
        console.log('playout-delay extension not supported')
      }
    }
  }

  async joinRoom(userId: string) {
    this.userId = userId
    this.peerConnection = new RTCPeerConnection(this.config)

    this.peerConnection.onicecandidate = (event) => {
      if (event.candidate) {
        this.signaling?.send({
          type: 'ice-candidate',
          from: userId,
          to: '',
          roomId: this.roomId,
          payload: { candidate: event.candidate.toJSON() },
          timestamp: Date.now()
        })
      }
    }

    this.peerConnection.oniceconnectionstatechange = () => {
      console.log('ICE connection state:', this.peerConnection?.iceConnectionState)
    }

    this.peerConnection.ontrack = (event) => {
      if (this.remoteVideoElement && event.streams[0]) {
        this.remoteVideoElement.srcObject = event.streams[0]
      }
    }

    try {
      this.localStream = await navigator.mediaDevices.getUserMedia({
        video: this.videoConstraints,
        audio: {
          echoCancellation: true,
          noiseSuppression: true,
          autoGainControl: true
        }
      })
      if (this.localVideoElement) {
        this.localVideoElement.srcObject = this.localStream
      }
      this.localStream.getTracks().forEach((track) => {
        const sender = this.peerConnection?.addTrack(track, this.localStream!)
        if (sender && track.kind === 'video') {
          const parameters = sender.getParameters()
          if (!parameters.encodings || parameters.encodings.length === 0) {
            parameters.encodings = [{ maxBitrate: 800000 }]
          } else {
            parameters.encodings[0].maxBitrate = 800000
          }
          parameters.degradationPreference = 'maintain-framerate'
          sender.setParameters(parameters).catch(() => {})
        }
      })
    } catch (error) {
      console.error('Failed to get media devices', error)
    }

    this.signaling?.send({
      type: 'join',
      from: userId,
      to: '',
      roomId: this.roomId,
      timestamp: Date.now()
    })
  }

  async createOffer(remoteUserId: string) {
    if (!this.peerConnection) return
    try {
      this.applyLowLatencyConfig(this.peerConnection)
      const offer = await this.peerConnection.createOffer({
        offerToReceiveAudio: true,
        offerToReceiveVideo: true,
        voiceActivityDetection: true,
        iceRestart: false
      })
      const sdp = this.enhanceSdpForLowLatency(offer.sdp || '')
      await this.peerConnection.setLocalDescription({ type: 'offer', sdp })
      this.signaling?.send({
        type: 'offer',
        from: this.userId,
        to: remoteUserId,
        roomId: this.roomId,
        payload: { sdp },
        timestamp: Date.now()
      })
    } catch (error) {
      console.error('Failed to create offer', error)
    }
  }

  async handleAnswer(sdp: string) {
    if (!this.peerConnection) return
    try {
      const enhancedSdp = this.enhanceSdpForLowLatency(sdp)
      await this.peerConnection.setRemoteDescription(
        new RTCSessionDescription({ type: 'answer', sdp: enhancedSdp })
      )
    } catch (error) {
      console.error('Failed to handle answer', error)
    }
  }

  async handleOffer(sdp: string, userId: string) {
    if (!this.peerConnection) return
    try {
      const enhancedSdp = this.enhanceSdpForLowLatency(sdp)
      await this.peerConnection.setRemoteDescription(
        new RTCSessionDescription({ type: 'offer', sdp: enhancedSdp })
      )
      this.applyLowLatencyConfig(this.peerConnection)
      const answer = await this.peerConnection.createAnswer()
      const answerSdp = this.enhanceSdpForLowLatency(answer.sdp || '')
      await this.peerConnection.setLocalDescription({ type: 'answer', sdp: answerSdp })
      this.signaling?.send({
        type: 'answer',
        from: this.userId,
        to: userId,
        roomId: this.roomId,
        payload: { sdp: answerSdp },
        timestamp: Date.now()
      })
    } catch (error) {
      console.error('Failed to handle offer', error)
    }
  }

  private enhanceSdpForLowLatency(sdp: string): string {
    let result = sdp
    result = result.replace(/useinbandfec=1/g, 'useinbandfec=1; usedtx=1')
    if (!result.includes('stereo=0')) {
      result = result.replace(
        /a=fmtp:111 /g,
        'a=fmtp:111 minptime=10; useinbandfec=1; usedtx=1; stereo=0; '
      )
    }
    return result
  }

  async handleIceCandidate(candidate: RTCIceCandidateInit) {
    if (!this.peerConnection) return
    try {
      await this.peerConnection.addIceCandidate(new RTCIceCandidate(candidate))
    } catch (error) {
      console.error('Failed to add ICE candidate', error)
    }
  }

  toggleAudio(): boolean {
    this.audioEnabled = !this.audioEnabled
    this.localStream?.getAudioTracks().forEach((track) => {
      track.enabled = this.audioEnabled
    })
    return this.audioEnabled
  }

  toggleVideo(): boolean {
    this.videoEnabled = !this.videoEnabled
    this.localStream?.getVideoTracks().forEach((track) => {
      track.enabled = this.videoEnabled
    })
    return this.videoEnabled
  }

  getStats(): Promise<RTCStatsReport | null> {
    if (!this.peerConnection) return Promise.resolve(null)
    return this.peerConnection.getStats()
  }

  leaveRoom() {
    this.localStream?.getTracks().forEach((track) => track.stop())
    this.localStream = null
    this.peerConnection?.close()
    this.peerConnection = null
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
        if (message.from !== userId) {
          this.createOffer(message.from)
        }
        break
    }
  }
}
