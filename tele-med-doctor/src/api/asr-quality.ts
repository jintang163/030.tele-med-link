import request from '@/utils/request'
import type { AsrQualityReport, AsrQualityIssue, DiagnosisSuggestion } from '@/types'

export function processAsrAndQuality(consultationId: number) {
  return request.post<never, { data: AsrQualityReport }>(`/asr-quality/process/${consultationId}`)
}

export function getAsrQualityReportByConsultation(consultationId: number) {
  return request.get<never, { data: AsrQualityReport }>(`/asr-quality/report/consultation/${consultationId}`)
}

export function getDoctorQualityReports(doctorId: number) {
  return request.get<never, { data: AsrQualityReport[] }>(`/asr-quality/report/doctor/${doctorId}`)
}

export function getPatientQualityReports(patientId: number) {
  return request.get<never, { data: AsrQualityReport[] }>(`/asr-quality/report/patient/${patientId}`)
}

export function getQualityReportIssues(reportId: number) {
  return request.get<never, { data: AsrQualityIssue[] }>(`/asr-quality/report/${reportId}/issues`)
}

export function resolveQualityIssue(issueId: number, operatorId: number) {
  return request.post<never, { data: AsrQualityIssue }>(
    `/asr-quality/issue/${issueId}/resolve?operatorId=${operatorId}`
  )
}

export interface GenerateSuggestionPayload {
  consultationId?: number
  patientId: number
  doctorId: number
  department?: string
  patientComplaint?: string
  medicalHistory?: string
  imagingFindings?: string
  vitalSigns?: string
  labResults?: string
  generateSuggestions?: boolean
}

export function generateDiagnosisSuggestion(payload: GenerateSuggestionPayload) {
  return request.post<never, { data: DiagnosisSuggestion }>(
    '/diagnosis-assist/suggestion/generate',
    payload
  )
}

export function getDiagnosisSuggestionByConsultation(consultationId: number) {
  return request.get<never, { data: DiagnosisSuggestion }>(
    `/diagnosis-assist/suggestion/consultation/${consultationId}`
  )
}

export function getPatientDiagnosisSuggestions(patientId: number) {
  return request.get<never, { data: DiagnosisSuggestion[] }>(
    `/diagnosis-assist/suggestion/patient/${patientId}`
  )
}

export function getDoctorDiagnosisSuggestions(doctorId: number) {
  return request.get<never, { data: DiagnosisSuggestion[] }>(
    `/diagnosis-assist/suggestion/doctor/${doctorId}`
  )
}
