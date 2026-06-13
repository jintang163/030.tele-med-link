package com.telemed.model.repository;

import com.telemed.model.entity.DiagnosisSuggestion;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface DiagnosisSuggestionRepository extends JpaRepository<DiagnosisSuggestion, Long> {

    Optional<DiagnosisSuggestion> findTopByConsultationIdOrderByCreateTimeDesc(Long consultationId);

    List<DiagnosisSuggestion> findByPatientIdOrderByCreateTimeDesc(Long patientId);

    List<DiagnosisSuggestion> findByDoctorIdOrderByCreateTimeDesc(Long doctorId);

    List<DiagnosisSuggestion> findByDepartmentOrderByCreateTimeDesc(String department);

    void deleteByCreateTimeBefore(LocalDateTime before);
}
