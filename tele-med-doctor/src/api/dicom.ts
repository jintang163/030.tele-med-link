import request from '@/utils/request'
import type { DicomImage, DicomToken } from '@/types'

export interface DicomUploadParams {
  consultationId: number
  uploaderId: number
  uploaderName?: string
  patientName?: string
  studyUid?: string
  seriesUid?: string
  instanceUid?: string
  modality?: string
  studyDescription?: string
  seriesDescription?: string
  sliceIndex?: number
}

export function uploadDicom(file: File, params: DicomUploadParams) {
  const formData = new FormData()
  formData.append('file', file)
  Object.entries(params).forEach(([key, value]) => {
    if (value !== undefined && value !== null) {
      formData.append(key, String(value))
    }
  })
  return request.post<never, { data: DicomImage }>('/dicom/upload', formData, {
    headers: { 'Content-Type': 'multipart/form-data' }
  })
}

export function getConsultationDicomImages(consultationId: number) {
  return request.get<never, { data: DicomImage[] }>(`/dicom/consultation/${consultationId}/images`)
}

export function generateDicomToken(
  consultationId: number,
  imageId: number,
  userId: number,
  userName?: string
) {
  const params = new URLSearchParams()
  params.append('consultationId', String(consultationId))
  params.append('imageId', String(imageId))
  params.append('userId', String(userId))
  if (userName) params.append('userName', userName)
  return request.post<never, { data: DicomToken }>(`/dicom/token/generate?${params.toString()}`)
}

export function generateConsultationDicomToken(
  consultationId: number,
  userId: number,
  userName?: string
) {
  const params = new URLSearchParams()
  params.append('consultationId', String(consultationId))
  params.append('userId', String(userId))
  if (userName) params.append('userName', userName)
  return request.post<never, { data: DicomToken }>(`/dicom/token/generate-consultation?${params.toString()}`)
}

export function validateDicomToken(token: string) {
  return request.get<never, { data: DicomToken }>(`/dicom/token/validate?token=${token}`)
}

export function getDicomImagesByToken(token: string) {
  return request.get<never, { data: DicomImage[] }>(`/dicom/token/images?token=${token}`)
}

export function getDicomImageUrlByToken(token: string) {
  return request.get<never, { data: string }>(`/dicom/token/url?token=${token}`)
}

export function getDicomImageInfoByToken(token: string) {
  return request.get<never, { data: DicomImage }>(`/dicom/token/info?token=${token}`)
}

export function deleteDicomImage(imageId: number, operatorId: number) {
  return request.delete<never, { data: void }>(`/dicom/image/${imageId}?operatorId=${operatorId}`)
}
