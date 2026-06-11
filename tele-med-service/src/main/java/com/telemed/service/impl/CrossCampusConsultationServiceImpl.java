package com.telemed.service.impl;

import com.telemed.common.constant.AppointmentStatus;
import com.telemed.common.constant.ConsultationStatus;
import com.telemed.common.constant.CrossCampusConstants;
import com.telemed.common.constant.TimeSlot;
import com.telemed.common.dto.CrossCampusConsultationCreateDTO;
import com.telemed.common.dto.CrossCampusNotifyDTO;
import com.telemed.common.exception.BusinessException;
import com.telemed.common.vo.ConsultationDoctorVO;
import com.telemed.common.vo.ConsultationVO;
import com.telemed.common.vo.DoctorScheduleVO;
import com.telemed.common.vo.TimeSlotVO;
import com.telemed.model.entity.Appointment;
import com.telemed.model.entity.Campus;
import com.telemed.model.entity.Consultation;
import com.telemed.model.entity.ConsultationDoctor;
import com.telemed.model.entity.Doctor;
import com.telemed.model.entity.Hospital;
import com.telemed.model.entity.Patient;
import com.telemed.model.entity.User;
import com.telemed.model.repository.AppointmentRepository;
import com.telemed.model.repository.CampusRepository;
import com.telemed.model.repository.ConsultationDoctorRepository;
import com.telemed.model.repository.ConsultationRepository;
import com.telemed.model.repository.DoctorRepository;
import com.telemed.model.repository.HospitalRepository;
import com.telemed.model.repository.PatientRepository;
import com.telemed.model.repository.UserRepository;
import com.telemed.service.CrossCampusConsultationService;
import com.telemed.service.SignalingService;
import com.telemed.service.WechatNotifyService;
import com.telemed.web.mq.CrossCampusNotifyProducer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class CrossCampusConsultationServiceImpl implements CrossCampusConsultationService {

    private final ConsultationRepository consultationRepository;
    private final ConsultationDoctorRepository consultationDoctorRepository;
    private final AppointmentRepository appointmentRepository;
    private final DoctorRepository doctorRepository;
    private final PatientRepository patientRepository;
    private final UserRepository userRepository;
    private final CampusRepository campusRepository;
    private final HospitalRepository hospitalRepository;
    private final SignalingService signalingService;
    private final WechatNotifyService wechatNotifyService;
    private final CrossCampusNotifyProducer notifyProducer;

    @Value("${cross-campus.consultation.timeout-minutes:30}")
    private int timeoutMinutes;

    @Value("${cross-campus.consultation.room-id-prefix:CROSS-CAMPUS}")
    private String roomIdPrefix;

    @Override
    @Transactional
    public Consultation createCrossCampusConsultation(CrossCampusConsultationCreateDTO dto) {
        if (dto.getSourceCampusId().equals(dto.getTargetCampusId())) {
            throw new BusinessException("源院区和目标院区不能相同");
        }

        if (checkTimeConflict(dto.getPrimaryDoctorId(), dto.getAppointmentDate(), dto.getTimeSlot())) {
            throw new BusinessException("主诊医生在该时间段已有预约");
        }

        if (dto.getAssistantDoctorIds() != null) {
            for (Long assistantId : dto.getAssistantDoctorIds()) {
                if (checkTimeConflict(assistantId, dto.getAppointmentDate(), dto.getTimeSlot())) {
                    Doctor doctor = doctorRepository.findById(assistantId).orElse(null);
                    String doctorName = doctor != null ? getDoctorName(doctor) : "副诊医生";
                    throw new BusinessException(doctorName + "在该时间段已有预约");
                }
            }
        }

        Patient patient = patientRepository.findById(dto.getPatientId())
                .orElseThrow(() -> new BusinessException("患者不存在"));

        Doctor primaryDoctor = doctorRepository.findById(dto.getPrimaryDoctorId())
                .orElseThrow(() -> new BusinessException("主诊医生不存在"));

        String consultationNo = UUID.randomUUID().toString().replace("-", "").substring(0, 32).toUpperCase();

        Consultation consultation = new Consultation();
        consultation.setConsultationNo(consultationNo);
        consultation.setPatientId(dto.getPatientId());
        consultation.setDoctorId(dto.getPrimaryDoctorId());
        consultation.setHospitalId(primaryDoctor.getHospitalId());
        consultation.setCampusId(dto.getSourceCampusId());
        consultation.setType(dto.getConsultationType() != null ? dto.getConsultationType() : CrossCampusConstants.CONSULTATION_TYPE_CROSS_CAMPUS);
        consultation.setStatus(ConsultationStatus.WAITING.getCode());
        consultation.setCrossCampus(true);
        consultation.setSourceCampusId(dto.getSourceCampusId());
        consultation.setTargetCampusId(dto.getTargetCampusId());
        consultation.setCampusTag(generateCampusTag(dto.getSourceCampusId(), dto.getTargetCampusId()));
        consultation.setExpireTime(LocalDateTime.now().plusMinutes(timeoutMinutes));
        consultation = consultationRepository.save(consultation);

        Appointment appointment = new Appointment();
        appointment.setPatientId(dto.getPatientId());
        appointment.setDoctorId(dto.getPrimaryDoctorId());
        appointment.setHospitalId(primaryDoctor.getHospitalId());
        appointment.setCampusId(dto.getSourceCampusId());
        appointment.setTargetCampusId(dto.getTargetCampusId());
        appointment.setCrossCampus(true);
        appointment.setCampusTag(consultation.getCampusTag());
        appointment.setAppointmentDate(dto.getAppointmentDate());
        appointment.setTimeSlot(dto.getTimeSlot());
        appointment.setDescription(dto.getDescription());
        appointment.setStatus(AppointmentStatus.PENDING.getCode());
        appointment.setConsultationId(consultation.getId());
        appointment.setExpireTime(consultation.getExpireTime());
        appointment = appointmentRepository.save(appointment);

        consultation.setAppointmentId(appointment.getId());

        String roomId = generateRoomId(consultationNo);
        consultation.setRoomId(roomId);
        consultation = consultationRepository.save(consultation);

        saveConsultationDoctors(consultation.getId(), primaryDoctor, dto.getAssistantDoctorIds(), dto.getTargetCampusId());

        sendCrossCampusNotification(consultation, appointment, patient, primaryDoctor, dto);

        log.info("跨院区会诊申请创建成功: consultationNo={}, campusTag={}", consultationNo, consultation.getCampusTag());
        return consultation;
    }

    @Override
    @Transactional
    public Consultation confirmCrossCampusConsultation(Long consultationId, Long doctorId) {
        Consultation consultation = consultationRepository.findById(consultationId)
                .orElseThrow(() -> new BusinessException("会诊不存在"));

        if (!consultation.getCrossCampus()) {
            throw new BusinessException("该会诊不是跨院区会诊");
        }

        if (consultation.getExpireTime() != null && consultation.getExpireTime().isBefore(LocalDateTime.now())) {
            throw new BusinessException("该会诊申请已超时");
        }

        if (!consultation.getDoctorId().equals(doctorId)) {
            ConsultationDoctor cd = consultationDoctorRepository
                    .findByConsultationIdAndDoctorId(consultationId, doctorId).orElse(null);
            if (cd == null) {
                throw new BusinessException("您不是该会诊的医生");
            }
            cd.setJoinStatus(CrossCampusConstants.JOIN_STATUS_ACCEPTED);
            cd.setJoinTime(LocalDateTime.now());
            consultationDoctorRepository.save(cd);
            return consultation;
        }

        consultation.setStatus(ConsultationStatus.CONFIRMED.getCode());
        consultation.setConfirmTime(LocalDateTime.now());
        consultation = consultationRepository.save(consultation);

        List<Appointment> appointments = appointmentRepository.findByConsultationId(consultationId);
        for (Appointment appt : appointments) {
            if (appt.getStatus() == AppointmentStatus.PENDING.getCode()) {
                appt.setStatus(AppointmentStatus.CONFIRMED.getCode());
                appointmentRepository.save(appt);
            }
        }

        wechatNotifyService.notifyCrossCampusConsultationResult(
                consultation.getPatientId(),
                getDoctorName(doctorId),
                true,
                null
        );

        log.info("跨院区会诊已确认: consultationNo={}, doctorId={}", consultation.getConsultationNo(), doctorId);
        return consultation;
    }

    @Override
    @Transactional
    public Consultation rejectCrossCampusConsultation(Long consultationId, Long doctorId, String reason) {
        Consultation consultation = consultationRepository.findById(consultationId)
                .orElseThrow(() -> new BusinessException("会诊不存在"));

        if (!consultation.getCrossCampus()) {
            throw new BusinessException("该会诊不是跨院区会诊");
        }

        if (!consultation.getDoctorId().equals(doctorId)) {
            ConsultationDoctor cd = consultationDoctorRepository
                    .findByConsultationIdAndDoctorId(consultationId, doctorId).orElse(null);
            if (cd != null) {
                cd.setJoinStatus(CrossCampusConstants.JOIN_STATUS_REJECTED);
                consultationDoctorRepository.save(cd);
            }
            return consultation;
        }

        consultation.setStatus(ConsultationStatus.CANCELLED.getCode());
        consultation = consultationRepository.save(consultation);

        List<Appointment> appointments = appointmentRepository.findByConsultationId(consultationId);
        for (Appointment appt : appointments) {
            if (appt.getStatus() == AppointmentStatus.PENDING.getCode()
                    || appt.getStatus() == AppointmentStatus.CONFIRMED.getCode()) {
                appt.setStatus(AppointmentStatus.CANCELLED.getCode());
                appointmentRepository.save(appt);
            }
        }

        wechatNotifyService.notifyCrossCampusConsultationResult(
                consultation.getPatientId(),
                getDoctorName(doctorId),
                false,
                reason
        );

        log.info("跨院区会诊已拒绝: consultationNo={}, doctorId={}, reason={}",
                consultation.getConsultationNo(), doctorId, reason);
        return consultation;
    }

    @Override
    @Transactional
    public Consultation cancelCrossCampusConsultation(Long consultationId, Long patientId) {
        Consultation consultation = consultationRepository.findById(consultationId)
                .orElseThrow(() -> new BusinessException("会诊不存在"));

        if (!consultation.getPatientId().equals(patientId)) {
            throw new BusinessException("无权取消此会诊");
        }

        if (!consultation.getCrossCampus()) {
            throw new BusinessException("该会诊不是跨院区会诊");
        }

        if (consultation.getStatus() == ConsultationStatus.ONGOING.getCode()
                || consultation.getStatus() == ConsultationStatus.FINISHED.getCode()) {
            throw new BusinessException("当前状态不允许取消");
        }

        consultation.setStatus(ConsultationStatus.CANCELLED.getCode());
        consultation = consultationRepository.save(consultation);

        List<Appointment> appointments = appointmentRepository.findByConsultationId(consultationId);
        for (Appointment appt : appointments) {
            if (appt.getStatus() != AppointmentStatus.CANCELLED.getCode()
                    && appt.getStatus() != AppointmentStatus.COMPLETED.getCode()) {
                appt.setStatus(AppointmentStatus.CANCELLED.getCode());
                appointmentRepository.save(appt);
            }
        }

        log.info("患者取消跨院区会诊: consultationNo={}, patientId={}", consultation.getConsultationNo(), patientId);
        return consultation;
    }

    @Override
    public DoctorScheduleVO getDoctorSchedule(Long doctorId, LocalDate date) {
        Doctor doctor = doctorRepository.findById(doctorId)
                .orElseThrow(() -> new BusinessException("医生不存在"));

        DoctorScheduleVO vo = new DoctorScheduleVO();
        vo.setDoctorId(doctor.getId());
        vo.setTitle(doctor.getTitle());
        vo.setDepartment(doctor.getDepartment());
        vo.setCampusId(doctor.getCampusId());
        vo.setHospitalId(doctor.getHospitalId());
        vo.setScheduleDate(date);

        User user = userRepository.findById(doctor.getUserId()).orElse(null);
        if (user != null) {
            vo.setDoctorName(user.getRealName());
        }

        Campus campus = campusRepository.findById(doctor.getCampusId()).orElse(null);
        if (campus != null) {
            vo.setCampusName(campus.getName());
            Hospital hospital = hospitalRepository.findById(campus.getHospitalId()).orElse(null);
            if (hospital != null) {
                vo.setHospitalName(hospital.getName());
            }
        }

        vo.setTimeSlots(getDoctorAvailableTimeSlots(doctorId, date));
        return vo;
    }

    @Override
    public List<DoctorScheduleVO> getCampusDoctorSchedules(Long campusId, String department, LocalDate date) {
        List<Doctor> doctors;
        if (department != null && !department.isEmpty()) {
            doctors = doctorRepository.findByDepartmentAndCampusIdAndStatus(department, campusId, 1);
        } else {
            doctors = doctorRepository.findByCampusIdAndStatus(campusId, 1);
        }

        return doctors.stream()
                .map(d -> getDoctorSchedule(d.getId(), date))
                .collect(Collectors.toList());
    }

    @Override
    public List<TimeSlotVO> getDoctorAvailableTimeSlots(Long doctorId, LocalDate date) {
        List<TimeSlotVO> slots = new ArrayList<>();

        TimeSlotVO morningSlot = new TimeSlotVO();
        morningSlot.setCode(TimeSlot.MORNING.getCode());
        morningSlot.setName(TimeSlot.MORNING.getDesc());
        morningSlot.setStartTime(LocalTime.of(8, 30));
        morningSlot.setEndTime(LocalTime.of(12, 0));

        TimeSlotVO afternoonSlot = new TimeSlotVO();
        afternoonSlot.setCode(TimeSlot.AFTERNOON.getCode());
        afternoonSlot.setName(TimeSlot.AFTERNOON.getDesc());
        afternoonSlot.setStartTime(LocalTime.of(14, 0));
        afternoonSlot.setEndTime(LocalTime.of(17, 30));

        boolean morningAvailable = !checkTimeConflict(doctorId, date, TimeSlot.MORNING.getCode());
        morningSlot.setAvailable(morningAvailable);
        morningSlot.setRemainingCapacity(morningAvailable ? 1 : 0);

        boolean afternoonAvailable = !checkTimeConflict(doctorId, date, TimeSlot.AFTERNOON.getCode());
        afternoonSlot.setAvailable(afternoonAvailable);
        afternoonSlot.setRemainingCapacity(afternoonAvailable ? 1 : 0);

        slots.add(morningSlot);
        slots.add(afternoonSlot);
        return slots;
    }

    @Override
    public List<ConsultationVO> getTargetCampusConsultations(Long targetCampusId, Integer status) {
        List<Consultation> consultations;
        if (status != null) {
            consultations = consultationRepository.findByTargetCampusIdAndStatus(targetCampusId, status);
        } else {
            consultations = consultationRepository.findByTargetCampusId(targetCampusId);
        }
        return consultations.stream().map(this::convertToCrossCampusVO).collect(Collectors.toList());
    }

    @Override
    public List<ConsultationVO> getSourceCampusConsultations(Long sourceCampusId, Integer status) {
        List<Consultation> consultations = consultationRepository.findBySourceCampusId(sourceCampusId);
        if (status != null) {
            consultations = consultations.stream()
                    .filter(c -> c.getStatus().equals(status))
                    .collect(Collectors.toList());
        }
        return consultations.stream().map(this::convertToCrossCampusVO).collect(Collectors.toList());
    }

    @Override
    public List<ConsultationVO> getCrossCampusByDoctorId(Long doctorId, Integer status) {
        List<Integer> allStatuses = List.of(
                ConsultationStatus.WAITING.getCode(),
                ConsultationStatus.CONFIRMED.getCode(),
                ConsultationStatus.ONGOING.getCode(),
                ConsultationStatus.FINISHED.getCode(),
                ConsultationStatus.CANCELLED.getCode()
        );
        List<Consultation> consultations = consultationRepository
                .findCrossCampusByDoctorIdAndStatuses(doctorId, allStatuses);
        if (status != null) {
            consultations = consultations.stream()
                    .filter(c -> c.getStatus().equals(status))
                    .collect(Collectors.toList());
        }
        return consultations.stream().map(this::convertToCrossCampusVO).collect(Collectors.toList());
    }

    @Override
    public List<ConsultationVO> getPatientCrossCampusConsultations(Long patientId, Integer status) {
        List<Consultation> consultations = consultationRepository
                .findByPatientIdAndCrossCampusTrueOrderByCreateTimeDesc(patientId);
        if (status != null) {
            consultations = consultations.stream()
                    .filter(c -> c.getStatus().equals(status))
                    .collect(Collectors.toList());
        }
        return consultations.stream().map(this::convertToCrossCampusVO).collect(Collectors.toList());
    }

    @Override
    public ConsultationVO getCrossCampusDetail(Long consultationId) {
        Consultation consultation = consultationRepository.findById(consultationId)
                .orElseThrow(() -> new BusinessException("会诊不存在"));
        return convertToCrossCampusVO(consultation);
    }

    @Override
    @Transactional
    public void cleanExpiredConsultations() {
        List<Consultation> expired = consultationRepository.findExpiredCrossCampusConsultations(
                ConsultationStatus.WAITING.getCode(),
                LocalDateTime.now()
        );

        for (Consultation consultation : expired) {
            if (consultation.getConfirmTime() != null) {
                continue;
            }
            boolean hasConfirmedAppointment = appointmentRepository
                    .findByConsultationId(consultation.getId()).stream()
                    .anyMatch(a -> AppointmentStatus.CONFIRMED.getCode() == a.getStatus());
            if (hasConfirmedAppointment) {
                continue;
            }

            consultation.setStatus(ConsultationStatus.CANCELLED.getCode());
            consultationRepository.save(consultation);

            List<Appointment> appointments = appointmentRepository.findByConsultationId(consultation.getId());
            for (Appointment appt : appointments) {
                if (appt.getStatus() == AppointmentStatus.PENDING.getCode()) {
                    appt.setStatus(AppointmentStatus.CANCELLED.getCode());
                    appointmentRepository.save(appt);
                }
            }

            log.info("清理超时未确认跨院区会诊: consultationNo={}", consultation.getConsultationNo());
        }

        List<Appointment> expiredAppointments = appointmentRepository.findExpiredCrossCampusAppointments(
                AppointmentStatus.PENDING.getCode(),
                LocalDateTime.now()
        );
        for (Appointment appt : expiredAppointments) {
            if (appt.getStatus() != AppointmentStatus.CANCELLED.getCode()
                    && appt.getStatus() != AppointmentStatus.CONFIRMED.getCode()) {
                appt.setStatus(AppointmentStatus.CANCELLED.getCode());
                appointmentRepository.save(appt);
                log.info("清理超时未确认跨院区预约: appointmentId={}", appt.getId());
            }
        }
    }

    @Override
    public boolean checkTimeConflict(Long doctorId, LocalDate date, Integer timeSlot) {
        List<Appointment> existing = appointmentRepository
                .findByDoctorIdAndAppointmentDateAndTimeSlot(doctorId, date, timeSlot);

        return existing.stream()
                .anyMatch(a -> a.getStatus() != AppointmentStatus.CANCELLED.getCode());
    }

    @Override
    public String generateCampusTag(Long sourceCampusId, Long targetCampusId) {
        return "CAMPUS_" + sourceCampusId + CrossCampusConstants.CAMPUS_TAG_SEPARATOR + "CAMPUS_" + targetCampusId;
    }

    @Override
    public String generateRoomId(String consultationNo) {
        try {
            return signalingService.createJanusRoom(roomIdPrefix + "-" + consultationNo);
        } catch (Exception e) {
            log.warn("创建Janus房间失败，使用默认房间ID", e);
            return roomIdPrefix + "-" + consultationNo;
        }
    }

    private void saveConsultationDoctors(Long consultationId, Doctor primaryDoctor,
                                         List<Long> assistantDoctorIds, Long targetCampusId) {
        ConsultationDoctor primaryCd = new ConsultationDoctor();
        primaryCd.setConsultationId(consultationId);
        primaryCd.setDoctorId(primaryDoctor.getId());
        primaryCd.setCampusId(targetCampusId);
        primaryCd.setRoleType(CrossCampusConstants.DOCTOR_ROLE_PRIMARY);
        primaryCd.setJoinStatus(CrossCampusConstants.JOIN_STATUS_PENDING);
        consultationDoctorRepository.save(primaryCd);

        if (assistantDoctorIds != null) {
            for (Long assistantId : assistantDoctorIds) {
                Doctor assistant = doctorRepository.findById(assistantId).orElse(null);
                if (assistant != null) {
                    ConsultationDoctor assistantCd = new ConsultationDoctor();
                    assistantCd.setConsultationId(consultationId);
                    assistantCd.setDoctorId(assistantId);
                    assistantCd.setCampusId(assistant.getCampusId());
                    assistantCd.setRoleType(CrossCampusConstants.DOCTOR_ROLE_ASSISTANT);
                    assistantCd.setJoinStatus(CrossCampusConstants.JOIN_STATUS_PENDING);
                    consultationDoctorRepository.save(assistantCd);
                }
            }
        }
    }

    private void sendCrossCampusNotification(Consultation consultation, Appointment appointment,
                                             Patient patient, Doctor primaryDoctor,
                                             CrossCampusConsultationCreateDTO dto) {
        Campus sourceCampus = campusRepository.findById(dto.getSourceCampusId()).orElse(null);
        Campus targetCampus = campusRepository.findById(dto.getTargetCampusId()).orElse(null);

        CrossCampusNotifyDTO notifyDTO = CrossCampusNotifyDTO.builder()
                .consultationId(consultation.getId())
                .consultationNo(consultation.getConsultationNo())
                .patientId(patient.getId())
                .patientName(patient.getName())
                .primaryDoctorId(primaryDoctor.getId())
                .primaryDoctorName(getDoctorName(primaryDoctor))
                .sourceCampusId(dto.getSourceCampusId())
                .sourceCampusName(sourceCampus != null ? sourceCampus.getName() : "未知院区")
                .targetCampusId(dto.getTargetCampusId())
                .targetCampusName(targetCampus != null ? targetCampus.getName() : "未知院区")
                .campusTag(consultation.getCampusTag())
                .appointmentDate(dto.getAppointmentDate())
                .timeSlot(dto.getTimeSlot())
                .timeSlotDesc(dto.getTimeSlot() == 0 ? TimeSlot.MORNING.getDesc() : TimeSlot.AFTERNOON.getDesc())
                .description(dto.getDescription())
                .roomId(consultation.getRoomId())
                .expireTime(consultation.getExpireTime())
                .createTime(LocalDateTime.now())
                .build();

        notifyProducer.sendAsyncCrossCampusNotify(notifyDTO);
    }

    private ConsultationVO convertToCrossCampusVO(Consultation consultation) {
        ConsultationVO vo = new ConsultationVO();
        vo.setId(consultation.getId());
        vo.setConsultationNo(consultation.getConsultationNo());
        vo.setPatientId(consultation.getPatientId());
        vo.setDoctorId(consultation.getDoctorId());
        vo.setHospitalId(consultation.getHospitalId());
        vo.setCampusId(consultation.getCampusId());
        vo.setStatus(consultation.getStatus());
        vo.setStatusText(getStatusText(consultation.getStatus()));
        vo.setType(consultation.getType());
        vo.setAppointmentId(consultation.getAppointmentId());
        vo.setRoomId(consultation.getRoomId());
        vo.setStartTime(consultation.getStartTime());
        vo.setEndTime(consultation.getEndTime());
        vo.setDuration(consultation.getDuration());
        vo.setCreateTime(consultation.getCreateTime());
        vo.setCrossCampus(consultation.getCrossCampus());
        vo.setSourceCampusId(consultation.getSourceCampusId());
        vo.setTargetCampusId(consultation.getTargetCampusId());
        vo.setCampusTag(consultation.getCampusTag());
        vo.setExpireTime(consultation.getExpireTime());
        vo.setConfirmTime(consultation.getConfirmTime());

        Patient patient = patientRepository.findById(consultation.getPatientId()).orElse(null);
        if (patient != null) {
            vo.setPatientName(patient.getName());
        }

        if (consultation.getDoctorId() != null) {
            vo.setPrimaryDoctorName(getDoctorName(consultation.getDoctorId()));
        }

        if (consultation.getSourceCampusId() != null) {
            Campus source = campusRepository.findById(consultation.getSourceCampusId()).orElse(null);
            if (source != null) {
                vo.setSourceCampusName(source.getName());
            }
        }

        if (consultation.getTargetCampusId() != null) {
            Campus target = campusRepository.findById(consultation.getTargetCampusId()).orElse(null);
            if (target != null) {
                vo.setTargetCampusName(target.getName());
            }
        }

        List<ConsultationDoctor> doctors = consultationDoctorRepository.findByConsultationId(consultation.getId());
        List<ConsultationDoctorVO> doctorVOs = doctors.stream()
                .filter(d -> !d.getRoleType().equals(CrossCampusConstants.DOCTOR_ROLE_PRIMARY))
                .map(this::convertDoctorToVO)
                .collect(Collectors.toList());
        vo.setAssistantDoctors(doctorVOs);

        return vo;
    }

    private ConsultationDoctorVO convertDoctorToVO(ConsultationDoctor cd) {
        ConsultationDoctorVO vo = new ConsultationDoctorVO();
        vo.setId(cd.getId());
        vo.setDoctorId(cd.getDoctorId());
        vo.setCampusId(cd.getCampusId());
        vo.setRoleType(cd.getRoleType());
        vo.setRoleTypeText(cd.getRoleType() == CrossCampusConstants.DOCTOR_ROLE_PRIMARY ? "主诊" : "副诊");
        vo.setJoinStatus(cd.getJoinStatus());
        vo.setJoinStatusText(getJoinStatusText(cd.getJoinStatus()));
        vo.setJoinTime(cd.getJoinTime());
        vo.setLeaveTime(cd.getLeaveTime());

        Doctor doctor = doctorRepository.findById(cd.getDoctorId()).orElse(null);
        if (doctor != null) {
            vo.setTitle(doctor.getTitle());
            vo.setDepartment(doctor.getDepartment());
            vo.setDoctorName(getDoctorName(doctor));
            Campus campus = campusRepository.findById(doctor.getCampusId()).orElse(null);
            if (campus != null) {
                vo.setCampusName(campus.getName());
            }
        }
        return vo;
    }

    private String getJoinStatusText(Integer status) {
        if (status == null) return "未知";
        return switch (status) {
            case 0 -> "待确认";
            case 1 -> "已接受";
            case 2 -> "已拒绝";
            case 3 -> "已加入";
            case 4 -> "已离开";
            default -> "未知";
        };
    }

    private String getStatusText(Integer status) {
        if (status == null) return "未知";
        for (ConsultationStatus s : ConsultationStatus.values()) {
            if (s.getCode() == status) {
                return switch (s) {
                    case WAITING -> "待确认";
                    case CONFIRMED -> "已确认";
                    case ONGOING -> "进行中";
                    case FINISHED -> "已完成";
                    case CANCELLED -> "已取消";
                };
            }
        }
        return "未知";
    }

    private String getDoctorName(Long doctorId) {
        Doctor doctor = doctorRepository.findById(doctorId).orElse(null);
        return doctor != null ? getDoctorName(doctor) : "未知医生";
    }

    private String getDoctorName(Doctor doctor) {
        User user = userRepository.findById(doctor.getUserId()).orElse(null);
        return user != null ? user.getRealName() : "未知医生";
    }
}
