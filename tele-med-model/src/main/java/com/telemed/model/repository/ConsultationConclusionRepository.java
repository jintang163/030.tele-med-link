package com.telemed.model.repository;

import com.telemed.model.entity.ConsultationConclusion;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ConsultationConclusionRepository extends JpaRepository<ConsultationConclusion, Long> {

    Optional<ConsultationConclusion> findByConsultationId(Long consultationId);

    List<ConsultationConclusion> findByPatientIdOrderByCreateTimeDesc(Long patientId);
}
