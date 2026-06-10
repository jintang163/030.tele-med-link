package com.telemed.service;

import com.telemed.common.vo.ConsultationVO;
import com.telemed.model.entity.Consultation;

import java.util.List;

public interface ConsultationService {

    Consultation createConsultation(Long patientId, Long doctorId, Integer type);

    Consultation acceptConsultation(Long consultationId, Long doctorId);

    Consultation finishConsultation(Long consultationId, String conclusionContent);

    List<Consultation> getWaitingConsultations();

    List<ConsultationVO> getWaitingConsultationVOList();

    List<Consultation> getDoctorConsultations(Long doctorId, Integer status);

    List<ConsultationVO> getDoctorConsultationVOList(Long doctorId, Integer status);

    List<Consultation> getPatientConsultations(Long patientId);

    List<ConsultationVO> getPatientConsultationVOList(Long patientId);

    Consultation getByConsultationNo(String consultationNo);

    ConsultationVO getConsultationVOById(Long id);
}
