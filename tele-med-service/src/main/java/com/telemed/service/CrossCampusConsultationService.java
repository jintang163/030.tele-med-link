package com.telemed.service;

import com.telemed.common.dto.CrossCampusConsultationCreateDTO;
import com.telemed.common.vo.ConsultationVO;
import com.telemed.common.vo.DoctorScheduleVO;
import com.telemed.common.vo.TimeSlotVO;
import com.telemed.model.entity.Consultation;

import java.time.LocalDate;
import java.util.List;

public interface CrossCampusConsultationService {

    Consultation createCrossCampusConsultation(CrossCampusConsultationCreateDTO dto);

    Consultation confirmCrossCampusConsultation(Long consultationId, Long doctorId);

    Consultation rejectCrossCampusConsultation(Long consultationId, Long doctorId, String reason);

    Consultation cancelCrossCampusConsultation(Long consultationId, Long patientId);

    DoctorScheduleVO getDoctorSchedule(Long doctorId, LocalDate date);

    List<DoctorScheduleVO> getCampusDoctorSchedules(Long campusId, String department, LocalDate date);

    List<TimeSlotVO> getDoctorAvailableTimeSlots(Long doctorId, LocalDate date);

    List<ConsultationVO> getTargetCampusConsultations(Long targetCampusId, Integer status);

    List<ConsultationVO> getSourceCampusConsultations(Long sourceCampusId, Integer status);

    List<ConsultationVO> getCrossCampusByDoctorId(Long doctorId, Integer status);

    ConsultationVO getCrossCampusDetail(Long consultationId);

    void cleanExpiredConsultations();

    boolean checkTimeConflict(Long doctorId, LocalDate date, Integer timeSlot);

    String generateCampusTag(Long sourceCampusId, Long targetCampusId);

    String generateRoomId(String consultationNo);
}
