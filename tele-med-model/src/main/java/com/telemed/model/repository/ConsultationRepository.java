package com.telemed.model.repository;

import com.telemed.model.entity.Consultation;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ConsultationRepository extends JpaRepository<Consultation, Long> {

    List<Consultation> findByStatus(Integer status);

    List<Consultation> findByDoctorIdAndStatus(Long doctorId, Integer status);

    List<Consultation> findByDoctorId(Long doctorId);

    List<Consultation> findByPatientIdOrderByCreateTimeDesc(Long patientId);

    Optional<Consultation> findByConsultationNo(String consultationNo);
}
