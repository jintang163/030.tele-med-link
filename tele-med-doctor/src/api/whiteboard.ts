import request from '@/utils/request'
import type { WhiteboardHistory, WhiteboardOp, WhiteboardSnapshotParams } from '@/types'

export function getWhiteboardHistory(
  roomId: string,
  source: 'BLANK' | 'DICOM' = 'BLANK',
  imageId?: number,
  limit = 200
) {
  let url = `/whiteboard/history/${roomId}?source=${source}&limit=${limit}`
  if (imageId) url += `&imageId=${imageId}`
  return request.get<never, { data: WhiteboardHistory }>(url)
}

export function getWhiteboardOpsByRange(
  roomId: string,
  startScore: number,
  endScore: number,
  source: 'BLANK' | 'DICOM' = 'BLANK',
  imageId?: number
) {
  let url = `/whiteboard/ops/${roomId}/range?source=${source}&startScore=${startScore}&endScore=${endScore}`
  if (imageId) url += `&imageId=${imageId}`
  return request.get<never, { data: WhiteboardOp[] }>(url)
}

export function recordWhiteboardOp(op: WhiteboardOp) {
  return request.post<never, { data: void }>('/whiteboard/ops', op)
}

export function clearWhiteboard(
  roomId: string,
  operatorId: number,
  source: 'BLANK' | 'DICOM' = 'BLANK',
  imageId?: number
) {
  let url = `/whiteboard/history/${roomId}?source=${source}&operatorId=${operatorId}`
  if (imageId) url += `&imageId=${imageId}`
  return request.delete<never, { data: void }>(url)
}

export function saveWhiteboardSnapshot(params: WhiteboardSnapshotParams) {
  return request.post<never, { data: string }>('/whiteboard/snapshot', params)
}

export function undoWhiteboard(
  roomId: string,
  operatorId: number,
  source: 'BLANK' | 'DICOM' = 'BLANK',
  imageId?: number
) {
  let url = `/whiteboard/undo/${roomId}?source=${source}&operatorId=${operatorId}`
  if (imageId) url += `&imageId=${imageId}`
  return request.post<never, { data: void }>(url)
}

export function redoWhiteboard(
  roomId: string,
  operatorId: number,
  source: 'BLANK' | 'DICOM' = 'BLANK',
  imageId?: number
) {
  let url = `/whiteboard/redo/${roomId}?source=${source}&operatorId=${operatorId}`
  if (imageId) url += `&imageId=${imageId}`
  return request.post<never, { data: void }>(url)
}
