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
  type: 'offer' | 'answer' | 'ice-candidate' | 'chat' | 'join' | 'leave' | 'user-online' | 'user-offline' | 'user-joined' | 'user-left' | 'mediasoup-producer-added' | 'mediasoup-producer-removed' | 'dicom-annotation' | 'dicom-viewport' | 'dicom-image-added' | 'whiteboard-op' | 'whiteboard-clear' | 'video-recording-request' | 'video-recording-auth' | 'video-recording-started' | 'video-recording-stopped' | 'video-recording-status'
  from: string
  to: string
  roomId: string
  payload?: any
  timestamp: number
}

export type VideoRecordingStatusType = 'PENDING_AUTHORIZATION' | 'RECORDING' | 'UPLOADING' | 'PROCESSING' | 'COMPLETED' | 'FAILED' | 'CANCELLED' | 'EXPIRED'

export interface VideoRecording {
  id: number
  consultationId: number
  consultationNo?: string
  doctorId: number
  doctorName?: string
  patientId: number
  patientName?: string
  status: number
  statusText?: string
  totalSegments?: number
  uploadedSegments?: number
  totalDuration?: number
  hlsPlaylistUrl?: string
  doctorAuthorized: boolean
  patientAuthorized: boolean
  watermarkText?: string
  startTime?: string
  endTime?: string
  expireTime?: string
  createTime?: string
}

export interface VideoSegment {
  id: number
  recordingId: number
  consultationId: number
  segmentIndex: number
  fileName: string
  bucketName: string
  objectName: string
  fileSize: number
  duration: number
  encryptionIv: string
  uploadStatus: number
  uploadTime?: string
}

export interface VideoRecordingRequestPayload {
  recordingId: number
  consultationId: number
  consultationNo?: string
  doctorId: number
  doctorName?: string
  watermarkText?: string
  segmentDuration?: number
  timestamp: number
}

export interface VideoRecordingAuthPayload {
  recordingId: number
  consultationId: number
  userRole: 'DOCTOR' | 'PATIENT'
  userId: number
  userName?: string
  authorized: boolean
  timestamp: number
}

export interface VideoRecordingStatusPayload {
  recordingId: number
  consultationId: number
  status: number
  statusText?: string
  doctorAuthorized?: boolean
  patientAuthorized?: boolean
  startTime?: string
}

export interface VideoPlaybackAuth {
  recordingId: number
  authToken: string
  hlsPlaylistUrl: string
  mp4Url: string
  expireTime: string
}

export interface VideoRecordingKeyVO {
  recordingId: number
  encryptionKey: string
  encryptionIv: string
}

export interface AsrQualityIssue {
  id: number
  reportId: number
  issueType: string
  severity: string
  description: string
  relatedText: string
  suggestion: string
  timelineStart: number
  timelineEnd: number
  resolved: boolean
  createTime: string
}

export interface TranscriptUtterance {
  id?: number
  speakerRole: string
  speakerName?: string
  text: string
  duration?: number
  createTime?: string
}

export interface AsrQualityReport {
  id: number
  consultationId: number
  consultationNo?: string
  doctorId: number
  doctorName?: string
  patientId: number
  patientName?: string
  status: string
  fullTranscript?: string
  totalDuration?: number
  doctorTalkTime?: number
  patientTalkTime?: number
  keyIndicatorScore: number
  safetyScore: number
  overallScore: number
  summary?: string
  recommendations?: string
  safetyRisksDetected?: boolean
  asrProvider?: string
  mentionedKeyIndicators?: string[]
  missingKeyIndicators?: string[]
  issues?: AsrQualityIssue[]
  utterances?: TranscriptUtterance[]
  createTime: string
  updateTime?: string
}

export interface DiagnosisSuggestion {
  id: number
  consultationId?: number
  patientId: number
  doctorId: number
  department?: string
  patientComplaint?: string
  imagingFindings?: string
  primaryDisease?: string
  primaryConfidence?: number
  primaryEvidence?: string
  secondaryDisease1?: string
  secondaryConfidence1?: number
  secondaryDisease2?: string
  secondaryConfidence2?: number
  secondaryDisease3?: string
  secondaryConfidence3?: number
  relatedSymptoms?: string[]
  recommendedTests?: string[]
  differentialDiagnosis?: string
  status?: string
  disclaimer?: string
  createTime?: string
}

export interface PatientRecordingItem {
  recordingId: number
  consultationId: number
  consultationNo: string
  doctorName?: string
  patientName?: string
  status: number
  statusText: string
  totalDuration?: number
  startTime?: string
  endTime?: string
  expireTime?: string
  hlsPlaylistUrl?: string
  authToken?: string
}

export interface DicomImage {
  id: number
  consultationId: number
  objectName: string
  fileName: string
  patientName?: string
  studyUid?: string
  seriesUid?: string
  instanceUid?: string
  modality?: string
  studyDescription?: string
  seriesDescription?: string
  sliceIndex?: number
  uploaderId: number
  uploaderName?: string
  fileSize?: number
  width?: number
  height?: number
  windowCenter?: number
  windowWidth?: number
  uploadTime: string
}

export interface DicomToken {
  token: string
  imageId?: number
  consultationId: number
  expireTime: string
  expireMinutes: number
  imageInfo?: DicomImage
}

export interface DicomAnnotation {
  annotationId: string
  imageId: number
  annotationType: 'POINT' | 'LINE' | 'RECTANGLE' | 'ELLIPSE' | 'ANGLE' | 'TEXT'
  coordinates: Array<{ x: number; y: number }>
  properties?: Record<string, any>
  creatorId?: number
  creatorName?: string
  createTime?: number
  updateTime?: number
}

export interface DicomAnnotationSyncPayload {
  annotationId: string
  imageId: number
  token?: string
  annotationType: string
  coordinates: Array<{ x: number; y: number }>
  properties?: Record<string, any>
  operation: 'add' | 'update' | 'delete' | 'clear'
  operatorId: number
  operatorName: string
  timestamp: number
}

export interface DicomViewportState {
  imageId: number
  token?: string
  windowCenter?: number
  windowWidth?: number
  scale?: number
  translation?: { x: number; y: number }
  rotation?: number
  invert?: boolean
  operatorId: number
  operatorName: string
  timestamp: number
}

export interface DicomViewportSyncPayload extends DicomViewportState {
  consultationId: number
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

export type VideoResolution = '1280x720' | '960x540' | '640x360'

export interface MediasoupNodeVO {
  id: string
  nodeName: string
  nodeIp: string
  nodePort: number
  httpPort: number
  region: string
  weight: number
  status: number
  cpuUsage: number
  memoryUsage: number
  activeConsumers: number
  activeProducers: number
}

export interface NearestNodeVO {
  nodeId: string
  nodeUrl: string
  wsUrl: string
  region: string
  latencyMs: number
  rtpCapabilities: any
}

export interface TurnServerVO {
  urls: string[]
  username: string
  credential: string
  credentialType: string
}

export interface TransportConnectVO {
  id: string
  iceParameters: any
  iceCandidates: any[]
  dtlsParameters: any
}

export interface ProducerVO {
  id: string
  kind: 'audio' | 'video'
  userId: string
  consultationId: string
  paused: boolean
}

export interface ConsumerVO {
  id: string
  producerId: string
  kind: 'audio' | 'video'
  userId: string
  paused: boolean
  rtpParameters: any
}

export interface QualityAdviceVO {
  targetResolution: VideoResolution
  targetBitrate: number
  shouldDowngrade: boolean
  reason: string
}

export interface QualityReportDTO {
  userId: string
  consultationId: string
  transportId: string
  kind: 'audio' | 'video'
  packetLostRate: number
  jitter: number
  roundTripTime: number
  bitrate: number
  resolution: VideoResolution
}

export type WhiteboardTool = 'PEN' | 'ARROW' | 'LINE' | 'RECTANGLE' | 'ELLIPSE' | 'TEXT' | 'ERASER'

export type WhiteboardOperation = 'DRAW' | 'CLEAR' | 'UNDO' | 'REDO'

export type WhiteboardSource = 'BLANK' | 'DICOM'

export interface WhiteboardPoint {
  x: number
  y: number
}

export interface WhiteboardOp {
  opId: string
  roomId: string
  source: WhiteboardSource
  imageId?: number
  operation: WhiteboardOperation
  toolType: WhiteboardTool
  points: WhiteboardPoint[]
  properties?: Record<string, any>
  color: string
  strokeWidth: number
  text?: string
  operatorId: number
  operatorName: string
  timestamp: number
}

export interface WhiteboardHistory {
  roomId: string
  source: WhiteboardSource
  imageId?: number
  totalOps: number
  operations: WhiteboardOp[]
}

export interface WhiteboardSnapshotParams {
  roomId: string
  source: WhiteboardSource
  imageId?: number
  snapshotData: string
  format?: string
  fileName?: string
  operatorId: number
  operatorName: string
  consultationId?: number
  insertToRecord?: boolean
}

export type ScheduleSlotStatus = 'NORMAL' | 'SUSPENDED' | 'SHIFTED'

export interface ScheduleSlot {
  id: number
  doctorId: number
  doctorName?: string
  scheduleDate: string
  slotTime: string
  status: ScheduleSlotStatus
  maxPatients: number
  remaining: number
  suspendReason?: string
  shiftToDoctorId?: number
  shiftToDoctorName?: string
  shiftToDate?: string
  shiftToSlotTime?: string
  createTime?: string
}

export interface DailySchedule {
  date: string
  dayOfWeek: number
  slots: ScheduleSlot[]
}

export type WeeklySchedule = DailySchedule[]

export interface ScheduleCreateDTO {
  doctorId: number
  scheduleDate: string
  slotTimes: string[]
  maxPatientsPerSlot: number
  operatorId: number
}

export interface ScheduleBatchCopyDTO {
  doctorId: number
  sourceStartDate: string
  sourceEndDate: string
  targetDates: string[]
  operatorId: number
}

export interface ScheduleSuspendDTO {
  scheduleId: number
  suspendReason: string
  operatorId: number
}

export interface ScheduleShiftDTO {
  scheduleId: number
  shiftToDoctorId: number
  shiftToDate: string
  shiftToSlotTime: string
  operatorId: number
}

export interface ScheduleTemplate {
  id: number
  doctorId: number
  templateName: string
  dayOfWeek: number
  dayOfWeekLabel: string
  slotTimes: string[]
  maxPatientsPerSlot: number
  createTime: string
}

export interface ScheduleTemplateCreateDTO {
  doctorId: number
  templateName: string
  dayOfWeek: number
  slotTimes: string[]
  maxPatientsPerSlot: number
}

export interface AppointmentBookDTO {
  scheduleSlotId: number
  patientId: number
  description?: string
  patientName?: string
}

export interface AppointmentRescheduleDTO {
  appointmentId: number
  newScheduleSlotId: number
  reason?: string
  operatorId: number
}

export type NotificationType = 'APPOINTMENT_REMINDER' | 'SCHEDULE_SUSPENDED' | 'APPOINTMENT_RESCHEDULED'

export interface PatientNotification {
  id: number
  patientId: number
  patientName?: string
  type: NotificationType
  typeText: string
  title: string
  content: string
  status: number
  statusText: string
  appointmentId?: number
  scheduleSlotId?: number
  createTime: string
}
