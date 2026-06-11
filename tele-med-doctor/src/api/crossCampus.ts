import request from '@/utils/request'
import type {
  CrossCampusConsultation,
  DoctorScheduleVO,
  TimeSlotVO,
  Campus,
  Hospital,
  Doctor,
  CrossCampusConsultationCreateDTO
} from '@/types'

export function createCrossCampusConsultation(data: CrossCampusConsultationCreateDTO) {
  return request.post<never, { data: CrossCampusConsultation }>('/cross-campus/consultation/create', data)
}

export function confirmCrossCampusConsultation(consultationId: number, doctorId: number) {
  return request.post<never, { data: CrossCampusConsultation }>(
    `/cross-campus/consultation/confirm?consultationId=${consultationId}&doctorId=${doctorId}`
  )
}

export function rejectCrossCampusConsultation(consultationId: number, doctorId: number, reason?: string) {
  const url = reason
    ? `/cross-campus/consultation/reject?consultationId=${consultationId}&doctorId=${doctorId}&reason=${encodeURIComponent(reason)}`
    : `/cross-campus/consultation/reject?consultationId=${consultationId}&doctorId=${doctorId}`
  return request.post<never, { data: CrossCampusConsultation }>(url)
}

export function cancelCrossCampusConsultation(consultationId: number, patientId: number) {
  return request.post<never, { data: CrossCampusConsultation }>(
    `/cross-campus/consultation/cancel?consultationId=${consultationId}&patientId=${patientId}`
  )
}

export function getCrossCampusConsultationDetail(id: number) {
  return request.get<never, { data: CrossCampusConsultation }>(`/cross-campus/consultation/${id}`)
}

export function getTargetCampusConsultations(campusId: number, status?: number) {
  const params = status !== undefined ? { status } : {}
  return request.get<never, { data: CrossCampusConsultation[] }>(
    `/cross-campus/consultation/target-campus/${campusId}`,
    { params }
  )
}

export function getSourceCampusConsultations(campusId: number, status?: number) {
  const params = status !== undefined ? { status } : {}
  return request.get<never, { data: CrossCampusConsultation[] }>(
    `/cross-campus/consultation/source-campus/${campusId}`,
    { params }
  )
}

export function getDoctorCrossCampusConsultations(doctorId: number, status?: number) {
  const params = status !== undefined ? { status } : {}
  return request.get<never, { data: CrossCampusConsultation[] }>(
    `/cross-campus/consultation/doctor/${doctorId}`,
    { params }
  )
}

export function getDoctorSchedule(doctorId: number, date: string) {
  return request.get<never, { data: DoctorScheduleVO }>(
    `/cross-campus/schedule/doctor/${doctorId}?date=${date}`
  )
}

export function getCampusSchedules(campusId: number, date: string, department?: string) {
  const params: Record<string, string> = { date }
  if (department) params.department = department
  return request.get<never, { data: DoctorScheduleVO[] }>(
    `/cross-campus/schedule/campus/${campusId}`,
    { params }
  )
}

export function getDoctorTimeSlots(doctorId: number, date: string) {
  return request.get<never, { data: TimeSlotVO[] }>(
    `/cross-campus/schedule/doctor/${doctorId}/time-slots?date=${date}`
  )
}

export function getHospitalList() {
  return request.get<never, { data: Hospital[] }>('/cross-campus/hospitals')
}

export function getCampusList(hospitalId?: number) {
  const params = hospitalId !== undefined ? { hospitalId } : {}
  return request.get<never, { data: Campus[] }>('/cross-campus/campuses', { params })
}

export function getOtherCampusDoctors(campusId: number, hospitalId: number, department?: string) {
  const params: Record<string, string | number> = { hospitalId }
  if (department) params.department = department
  return request.get<never, { data: Doctor[] }>(
    `/cross-campus/campus/${campusId}/other-doctors`,
    { params }
  )
}

export function getCampusDoctors(campusId: number, department?: string) {
  const params = department ? { department } : {}
  return request.get<never, { data: Doctor[] }>(
    `/cross-campus/campus/${campusId}/doctors`,
    { params }
  )
}
