import request from '@/utils/request'
import type { VideoRecording, VideoSegment, VideoPlaybackAuth, VideoRecordingKeyVO } from '@/types'

export function startRecording(consultationId: number, doctorId: number, watermarkText?: string, segmentDuration?: number) {
  return request.post<never, { data: VideoRecording }>('/video/recording/start', {
    consultationId,
    doctorId,
    watermarkText,
    segmentDuration
  })
}

export function authorizeRecording(consultationId: number, userId: number, userRole: string, authorized: boolean) {
  return request.post<never, { data: VideoRecording }>('/video/recording/authorize', {
    consultationId,
    userId,
    userRole,
    authorized
  })
}

export function uploadSegment(
  file: Blob,
  recordingId: number,
  consultationId: number,
  segmentIndex: number,
  fileName: string,
  duration?: number,
  encryptionIv?: string,
  checksum?: string
) {
  const formData = new FormData()
  formData.append('file', file)
  formData.append('recordingId', String(recordingId))
  formData.append('consultationId', String(consultationId))
  formData.append('segmentIndex', String(segmentIndex))
  formData.append('fileName', fileName)
  if (duration !== undefined) formData.append('duration', String(duration))
  if (encryptionIv) formData.append('encryptionIv', encryptionIv)
  if (checksum) formData.append('checksum', checksum)

  return request.post<never, { data: VideoSegment }>('/video/recording/segment', formData, {
    headers: { 'Content-Type': 'multipart/form-data' }
  })
}

export function stopRecording(consultationId: number) {
  return request.post<never, { data: VideoRecording }>(`/video/recording/stop?consultationId=${consultationId}`)
}

export function getEncryptionKey(consultationId: number) {
  return request.get<never, { data: VideoRecordingKeyVO }>(`/video/recording/encryption-key?consultationId=${consultationId}`)
}

export function getDoctorRecordings(doctorId: number, status?: number) {
  const params = status !== undefined ? { status } : {}
  return request.get<never, { data: VideoRecording[] }>(`/video/recording/doctor/${doctorId}`, { params })
}

export function getPatientRecordings(patientId: number) {
  return request.get<never, { data: VideoRecording[] }>(`/video/recording/patient/${patientId}`)
}

export function getRecordingDetail(id: number) {
  return request.get<never, { data: VideoRecording }>(`/video/recording/${id}`)
}

export function getRecordingSegments(recordingId: number) {
  return request.get<never, { data: VideoSegment[] }>(`/video/recording/${recordingId}/segments`)
}

export function generatePlaybackAuth(recordingId: number, userId: number, userRole: string, expireMinutes?: number) {
  return request.post<never, { data: VideoPlaybackAuth }>('/video/playback/auth', {
    recordingId,
    userId,
    userRole,
    expireMinutes
  })
}

export function validatePlaybackToken(token: string) {
  return request.get<never, { data: VideoPlaybackAuth }>('/video/playback/validate', {
    headers: { 'X-Playback-Token': token }
  })
}
