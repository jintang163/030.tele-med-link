export interface User {
  id: number
  username: string
  realName?: string
  phone?: string
  role: string
  avatarUrl?: string
  hospitalId?: number
  department?: string
  status?: number
  name?: string
}

export interface Doctor {
  id: number
  userId: number
  title?: string
  specialty?: string
  department?: string
  hospitalId?: number
  campusId?: number
  name?: string
  hospitalName?: string
  campusName?: string
  status?: number
}

export interface Consultation {
  id: number
  consultationNo: string
  patientId: number
  patientName?: string
  doctorId?: number
  doctorName?: string
  hospitalId?: number
  campusId?: number
  status: number
  statusText?: string
  type: number
  appointmentId?: number
  roomId: string
  startTime?: string
  endTime?: string
  duration?: number
  createTime: string
  updateTime?: string
  conclusionContent?: string
  conclusionFileUrl?: string
}

export interface Appointment {
  id: number
  patientId: number
  patientName?: string
  doctorId: number
  doctorName?: string
  doctorTitle?: string
  doctorDepartment?: string
  hospitalId?: number
  hospitalName?: string
  appointmentDate: string
  timeSlot: number
  timeSlotDesc?: string
  status: number
  statusText?: string
  description?: string
  consultationId?: number
  createTime: string
}

export interface ChatMessage {
  id?: number
  consultationId: number
  senderId: number
  senderName: string
  senderType: 'DOCTOR' | 'PATIENT'
  senderRole?: string
  content: string
  contentType?: number
  createTime: string
}

export interface SignalingMessage {
  type: 'offer' | 'answer' | 'ice-candidate' | 'chat' | 'join' | 'leave' | 'user-online' | 'user-offline' | 'user-joined' | 'user-left'
  from: string
  to: string
  roomId: string
  payload?: any
  timestamp: number
}

export interface Hospital {
  id: number
  name: string
  address?: string
  phone?: string
  level?: string
  status?: number
}

export interface Campus {
  id: number
  hospitalId: number
  name: string
  address?: string
  phone?: string
  status?: number
}

export interface TimeSlotVO {
  code: number
  name: string
  startTime?: string
  endTime?: string
  available: boolean
  remainingCapacity: number
}

export interface DoctorScheduleVO {
  doctorId: number
  doctorName?: string
  title?: string
  department?: string
  campusId?: number
  campusName?: string
  hospitalId?: number
  hospitalName?: string
  scheduleDate: string
  timeSlots: TimeSlotVO[]
}

export interface ConsultationDoctorVO {
  id?: number
  doctorId: number
  doctorName?: string
  title?: string
  department?: string
  campusId?: number
  campusName?: string
  roleType: number
  roleTypeText?: string
  joinStatus: number
  joinStatusText?: string
  joinTime?: string
  leaveTime?: string
}

export interface CrossCampusConsultation {
  id: number
  consultationNo: string
  patientId: number
  patientName?: string
  doctorId?: number
  doctorName?: string
  hospitalId?: number
  campusId?: number
  status: number
  statusText?: string
  type: number
  appointmentId?: number
  roomId: string
  startTime?: string
  endTime?: string
  duration?: number
  createTime: string
  conclusionContent?: string
  conclusionFileUrl?: string
  crossCampus: boolean
  sourceCampusId?: number
  sourceCampusName?: string
  targetCampusId?: number
  targetCampusName?: string
  campusTag?: string
  assistantDoctors?: ConsultationDoctorVO[]
  primaryDoctorName?: string
  expireTime?: string
  confirmTime?: string
}

export interface CrossCampusConsultationCreateDTO {
  patientId: number
  sourceCampusId: number
  targetCampusId: number
  primaryDoctorId: number
  assistantDoctorIds?: number[]
  appointmentDate: string
  timeSlot: number
  description?: string
  patientSymptoms?: string
  consultationType?: number
}
