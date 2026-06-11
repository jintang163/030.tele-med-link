import request from '@/utils/request'
import type {
  MediasoupNodeVO,
  NearestNodeVO,
  TurnServerVO,
  TransportConnectVO,
  ProducerVO,
  ConsumerVO,
  QualityAdviceVO,
  QualityReportDTO
} from '@/types'

const BASE_URL = '/mediasoup'

export function getNearestNode(clientIp?: string, clientRegion?: string, preferredRegion?: string) {
  const params = { clientIp, clientRegion, preferredRegion }
  return request.get<never, { data: NearestNodeVO }>(`${BASE_URL}/nearest-node`, { params })
}

export function listMediasoupNodes() {
  return request.get<never, { data: MediasoupNodeVO[] }>(`${BASE_URL}/nodes`)
}

export function getTurnConfig() {
  return request.get<never, { data: TurnServerVO }>(`${BASE_URL}/turn-config`)
}

export function getRouterCapabilities(nodeId: string) {
  return request.get<never, { data: any }>(`${BASE_URL}/router-capabilities/${nodeId}`)
}

export function createTransport(
  nodeId: string,
  consultationId: string,
  userId: string,
  kind: 'send' | 'recv'
) {
  return request.post<never, { data: TransportConnectVO }>(`${BASE_URL}/transport`, {
    nodeId,
    consultationId,
    userId,
    kind
  })
}

export function createProducer(
  nodeId: string,
  consultationId: string,
  userId: string,
  transportId: string,
  kind: 'audio' | 'video',
  rtpParameters: any
) {
  return request.post<never, { data: ProducerVO }>(`${BASE_URL}/producer`, {
    nodeId,
    consultationId,
    userId,
    transportId,
    kind,
    rtpParameters
  })
}

export function createConsumer(
  nodeId: string,
  consultationId: string,
  userId: string,
  transportId: string,
  producerId: string,
  rtpCapabilities: any
) {
  return request.post<never, { data: ConsumerVO }>(`${BASE_URL}/consumer`, {
    nodeId,
    consultationId,
    userId,
    transportId,
    producerId,
    rtpCapabilities
  })
}

export function reportQuality(dto: QualityReportDTO) {
  return request.post<never, { data: QualityAdviceVO }>(`${BASE_URL}/quality-report`, dto)
}
