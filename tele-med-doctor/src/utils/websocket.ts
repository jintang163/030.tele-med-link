import type { SignalingMessage } from '@/types'

export class SignalingWebSocket {
  private ws: WebSocket | null = null
  private userId: string
  private url: string
  private callbacks: ((message: SignalingMessage) => void)[] = []
  private reconnectTimer: ReturnType<typeof setTimeout> | null = null
  private reconnectAttempts = 0
  private maxReconnectAttempts = 10
  private reconnectInterval = 3000

  constructor(userId: string) {
    this.userId = userId
    this.url = `ws://localhost:8080/ws/signaling?userId=${userId}`
  }

  connect() {
    if (this.ws?.readyState === WebSocket.OPEN) return

    this.ws = new WebSocket(this.url)

    this.ws.onopen = () => {
      this.reconnectAttempts = 0
      console.log('WebSocket connected')
    }

    this.ws.onmessage = (event) => {
      try {
        const message: SignalingMessage = JSON.parse(event.data)
        this.callbacks.forEach((cb) => cb(message))
      } catch (e) {
        console.error('Failed to parse WebSocket message', e)
      }
    }

    this.ws.onclose = () => {
      console.log('WebSocket closed')
      this.scheduleReconnect()
    }

    this.ws.onerror = (error) => {
      console.error('WebSocket error', error)
      this.ws?.close()
    }
  }

  private scheduleReconnect() {
    if (this.reconnectAttempts >= this.maxReconnectAttempts) return
    if (this.reconnectTimer) clearTimeout(this.reconnectTimer)
    this.reconnectTimer = setTimeout(() => {
      this.reconnectAttempts++
      console.log(`Reconnecting... attempt ${this.reconnectAttempts}`)
      this.connect()
    }, this.reconnectInterval)
  }

  disconnect() {
    if (this.reconnectTimer) {
      clearTimeout(this.reconnectTimer)
      this.reconnectTimer = null
    }
    this.reconnectAttempts = this.maxReconnectAttempts
    this.ws?.close()
    this.ws = null
  }

  send(message: SignalingMessage) {
    if (this.ws?.readyState === WebSocket.OPEN) {
      this.ws.send(JSON.stringify(message))
    }
  }

  onMessage(callback: (message: SignalingMessage) => void) {
    this.callbacks.push(callback)
  }
}
