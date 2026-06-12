import request from '@/utils/request'
import type {
  WeeklySchedule,
  ScheduleSlot,
  ScheduleCreateDTO,
  ScheduleBatchCopyDTO,
  ScheduleSuspendDTO,
  ScheduleShiftDTO,
  Appointment,
  AppointmentRescheduleDTO,
  ScheduleTemplate,
  ScheduleTemplateCreateDTO,
  PatientNotification,
  NotificationType
} from '@/types'

export function getWeeklySchedule(doctorId: number, weekStart: string) {
  return request.get<never, { data: WeeklySchedule }>('/schedule/weekly', {
    params: { doctorId, weekStart }
  })
}

export function getSlotTimes() {
  return request.get<never, { data: string[] }>('/schedule/slot-times')
}

export function getDoctorDaySchedule(doctorId: number, date: string) {
  return request.get<never, { data: ScheduleSlot[] }>('/schedule/day', {
    params: { doctorId, date }
  })
}

export function createSchedule(data: ScheduleCreateDTO) {
  return request.post<never, { data: ScheduleSlot[] }>('/schedule', data)
}

export function batchCopySchedule(data: ScheduleBatchCopyDTO) {
  return request.post<never, { data: ScheduleSlot[] }>('/schedule/batch-copy', data)
}

export function suspendSchedule(data: ScheduleSuspendDTO) {
  return request.post<never, { data: ScheduleSlot }>('/schedule/suspend', data)
}

export function resumeSchedule(scheduleId: number, operatorId: number) {
  return request.post<never, { data: ScheduleSlot }>('/schedule/resume', { scheduleId, operatorId })
}

export function shiftSchedule(data: ScheduleShiftDTO) {
  return request.post<never, { data: ScheduleSlot }>('/schedule/shift', data)
}

export function deleteSchedule(scheduleId: number, operatorId: number) {
  return request.delete<never, { data: void }>(`/schedule/${scheduleId}`, {
    data: { operatorId }
  })
}

export function deleteDaySchedule(doctorId: number, date: string, operatorId: number) {
  return request.delete<never, { data: void }>('/schedule/day', {
    data: { doctorId, date, operatorId }
  })
}

export function getScheduleTemplates(doctorId: number) {
  return request.get<never, { data: ScheduleTemplate[] }>('/schedule/templates', {
    params: { doctorId }
  })
}

export function createScheduleTemplate(data: ScheduleTemplateCreateDTO) {
  return request.post<never, { data: ScheduleTemplate }>('/schedule/template', data)
}

export function applyScheduleTemplate(templateId: number, doctorId: number, targetDates: string[], operatorId: number) {
  return request.post<never, { data: ScheduleSlot[] }>(`/schedule/template/${templateId}/apply`, {
    doctorId, targetDates, operatorId
  })
}

export function deleteScheduleTemplate(templateId: number) {
  return request.delete<never, { data: void }>(`/schedule/template/${templateId}`)
}

export function bookAppointment(scheduleSlotId: number, patientId: number, description?: string) {
  return request.post<never, { data: Appointment }>('/appointment/book', {
    scheduleSlotId, patientId, description
  })
}

export function rescheduleAppointment(data: AppointmentRescheduleDTO) {
  return request.post<never, { data: Appointment }>('/appointment/reschedule', data)
}

export function getPatientAppointments(patientId: number) {
  return request.get<never, { data: Appointment[] }>(`/appointment/patient/${patientId}`)
}

export function getAppointmentDetail(appointmentId: number) {
  return request.get<never, { data: Appointment }>(`/appointment/${appointmentId}`)
}

export function getNotifications(patientId: number, type?: NotificationType, status?: number) {
  const params: Record<string, any> = { patientId }
  if (type) params.type = type
  if (status !== undefined) params.status = status
  return request.get<never, { data: PatientNotification[] }>('/notifications', { params })
}

export function getUnreadNotificationCount(patientId: number) {
  return request.get<never, { data: number }>('/notifications/unread-count', {
    params: { patientId }
  })
}

export function markNotificationRead(notificationId: number) {
  return request.post<never, { data: PatientNotification }>(`/notifications/${notificationId}/read`)
}

export function markAllNotificationsRead(patientId: number) {
  return request.post<never, { data: void }>('/notifications/read-all', { patientId })
}

export function getNotificationDetail(notificationId: number) {
  return request.get<never, { data: PatientNotification }>(`/notifications/${notificationId}`)
}
