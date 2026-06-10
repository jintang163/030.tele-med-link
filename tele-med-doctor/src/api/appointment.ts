import request from '@/utils/request'
import type { Appointment } from '@/types'

export function getDoctorAppointments(doctorId: number, status?: string) {
  const params = status ? { status } : {}
  return request.get<never, { data: Appointment[] }>(`/appointment/doctor/${doctorId}`, { params })
}

export function startAppointment(appointmentId: number, doctorId: number) {
  return request.post<never, { data: Appointment }>('/appointment/start', { appointmentId, doctorId })
}
