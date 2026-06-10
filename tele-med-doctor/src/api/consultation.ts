import request from '@/utils/request'
import type { Consultation, ChatMessage } from '@/types'

export function getWaitingList() {
  return request.get<never, { data: Consultation[] }>('/consultation/waiting')
}

export function getDoctorConsultations(doctorId: number, status?: string) {
  const params = status ? { status } : {}
  return request.get<never, { data: Consultation[] }>(`/consultation/doctor/${doctorId}`, { params })
}

export function acceptConsultation(consultationId: number, doctorId: number) {
  return request.post<never, { data: Consultation }>('/consultation/accept', { consultationId, doctorId })
}

export function finishConsultation(consultationId: number, conclusionContent?: string) {
  return request.post<never, { data: Consultation }>('/consultation/finish', { consultationId, conclusionContent })
}

export function getChatMessages(consultationId: number) {
  return request.get<never, { data: ChatMessage[] }>(`/consultation/chat/${consultationId}`)
}
