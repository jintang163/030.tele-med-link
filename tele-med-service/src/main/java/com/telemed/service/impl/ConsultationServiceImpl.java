package com.telemed.service.impl;

import com.telemed.common.constant.ConsultationStatus;
import com.telemed.common.exception.BusinessException;
import com.telemed.common.vo.ConsultationVO;
import com.telemed.model.entity.Consultation;
import com.telemed.model.entity.ConsultationConclusion;
import com.telemed.model.entity.Doctor;
import com.telemed.model.entity.Patient;
import com.telemed.model.entity.User;
import com.telemed.model.repository.ConsultationConclusionRepository;
import com.telemed.model.repository.ConsultationRepository;
import com.telemed.model.repository.DoctorRepository;
import com.telemed.model.repository.PatientRepository;
import com.telemed.model.repository.UserRepository;
import com.telemed.service.ConsultationService;
import com.telemed.service.MinioService;
import com.telemed.service.SignalingService;
import com.telemed.service.AppointmentService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ConsultationServiceImpl implements ConsultationService {

    private final ConsultationRepository consultationRepository;
    private final ConsultationConclusionRepository consultationConclusionRepository;
    private final MinioService minioService;
    private final SignalingService signalingService;
    private final DoctorRepository doctorRepository;
    private final PatientRepository patientRepository;
    private final UserRepository userRepository;
    private final AppointmentService appointmentService;

    @Override
    @Transactional
    public Consultation createConsultation(Long patientId, Long doctorId, Integer type) {
        Consultation consultation = new Consultation();
        consultation.setConsultationNo(UUID.randomUUID().toString().replace("-", ""));
        consultation.setPatientId(patientId);
        consultation.setDoctorId(doctorId);
        consultation.setType(type);
        consultation.setStatus(ConsultationStatus.WAITING.getCode());

        Patient patient = patientRepository.findById(patientId).orElse(null);
        if (patient != null) {
            consultation.setHospitalId(patient.getHospitalId());
            consultation.setCampusId(patient.getCampusId());
        }

        String roomId = signalingService.createJanusRoom(consultation.getConsultationNo());
        consultation.setRoomId(roomId);
        return consultationRepository.save(consultation);
    }

    @Override
    @Transactional
    public Consultation acceptConsultation(Long consultationId, Long doctorId) {
        Consultation consultation = consultationRepository.findById(consultationId)
                .orElseThrow(() -> new BusinessException("问诊不存在"));
        if (consultation.getStatus() != ConsultationStatus.WAITING.getCode()) {
            throw new BusinessException("问诊状态不是等待中，无法接诊");
        }
        consultation.setDoctorId(doctorId);
        consultation.setStatus(ConsultationStatus.ONGOING.getCode());
        consultation.setStartTime(LocalDateTime.now());
        return consultationRepository.save(consultation);
    }

    @Override
    @Transactional
    public Consultation finishConsultation(Long consultationId, String conclusionContent) {
        Consultation consultation = consultationRepository.findById(consultationId)
                .orElseThrow(() -> new BusinessException("问诊不存在"));
        if (consultation.getStatus() != ConsultationStatus.ONGOING.getCode()) {
            throw new BusinessException("问诊状态不是进行中，无法结束");
        }
        consultation.setStatus(ConsultationStatus.FINISHED.getCode());
        consultation.setEndTime(LocalDateTime.now());
        if (consultation.getStartTime() != null) {
            long durationSeconds = java.time.Duration.between(consultation.getStartTime(), consultation.getEndTime()).getSeconds();
            consultation.setDuration((int) durationSeconds);
        }
        consultation = consultationRepository.save(consultation);

        if (conclusionContent != null && !conclusionContent.isEmpty()) {
            String objectName = "conclusion/" + consultation.getConsultationNo() + ".txt";
            String fileUrl = minioService.uploadString(conclusionContent, objectName);

            ConsultationConclusion conclusion = new ConsultationConclusion();
            conclusion.setConsultationId(consultation.getId());
            conclusion.setDoctorId(consultation.getDoctorId());
            conclusion.setPatientId(consultation.getPatientId());
            conclusion.setContent(conclusionContent);
            conclusion.setFileUrl(fileUrl);
            consultationConclusionRepository.save(conclusion);
        }

        appointmentService.completeAppointmentByConsultationId(consultation.getId());

        return consultation;
    }

    @Override
    public List<Consultation> getWaitingConsultations() {
        return consultationRepository.findByStatus(ConsultationStatus.WAITING.getCode());
    }

    @Override
    public List<ConsultationVO> getWaitingConsultationVOList() {
        List<Consultation> consultations = getWaitingConsultations();
        return consultations.stream()
                .map(this::convertToVO)
                .collect(Collectors.toList());
    }

    @Override
    public List<Consultation> getDoctorConsultations(Long doctorId, Integer status) {
        return consultationRepository.findByDoctorIdAndStatus(doctorId, status);
    }

    @Override
    public List<ConsultationVO> getDoctorConsultationVOList(Long doctorId, Integer status) {
        List<Consultation> consultations = status != null
                ? consultationRepository.findByDoctorIdAndStatus(doctorId, status)
                : consultationRepository.findByDoctorId(doctorId);
        return consultations.stream()
                .map(this::convertToVO)
                .collect(Collectors.toList());
    }

    @Override
    public List<Consultation> getPatientConsultations(Long patientId) {
        return consultationRepository.findByPatientIdOrderByCreateTimeDesc(patientId);
    }

    @Override
    public List<ConsultationVO> getPatientConsultationVOList(Long patientId) {
        List<Consultation> consultations = getPatientConsultations(patientId);
        return consultations.stream()
                .map(this::convertToVO)
                .collect(Collectors.toList());
    }

    @Override
    public Consultation getByConsultationNo(String consultationNo) {
        return consultationRepository.findByConsultationNo(consultationNo)
                .orElseThrow(() -> new BusinessException("问诊不存在"));
    }

    @Override
    public ConsultationVO getConsultationVOById(Long id) {
        Consultation consultation = consultationRepository.findById(id)
                .orElseThrow(() -> new BusinessException("问诊不存在"));
        return convertToVO(consultation);
    }

    private ConsultationVO convertToVO(Consultation consultation) {
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

        Patient patient = patientRepository.findById(consultation.getPatientId()).orElse(null);
        if (patient != null) {
            vo.setPatientName(patient.getName());
        }

        if (consultation.getDoctorId() != null) {
            Doctor doctor = doctorRepository.findById(consultation.getDoctorId()).orElse(null);
            if (doctor != null) {
                User user = userRepository.findById(doctor.getUserId()).orElse(null);
                if (user != null) {
                    vo.setDoctorName(user.getRealName());
                }
            }
        }

        ConsultationConclusion conclusion = consultationConclusionRepository.findByConsultationId(consultation.getId()).orElse(null);
        if (conclusion != null) {
            vo.setConclusionContent(conclusion.getContent());
            vo.setConclusionFileUrl(conclusion.getFileUrl());
        }

        return vo;
    }

    private String getStatusText(Integer status) {
        if (status == null) return "未知";
        for (ConsultationStatus s : ConsultationStatus.values()) {
            if (s.getCode() == status) {
                return switch (s) {
                    case WAITING -> "等待中";
                    case ONGOING -> "进行中";
                    case FINISHED -> "已完成";
                    case CANCELLED -> "已取消";
                };
            }
        }
        return "未知";
    }
}
