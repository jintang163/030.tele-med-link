package com.telemed.model.repository;

import com.telemed.model.entity.FaceVerifyLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface FaceVerifyLogRepository extends JpaRepository<FaceVerifyLog, Long> {

    List<FaceVerifyLog> findByPatientIdOrderByVerifyTimeDesc(Long patientId);

    Page<FaceVerifyLog> findByPatientIdOrderByVerifyTimeDesc(Long patientId, Pageable pageable);

    long countByPatientIdAndResultAndVerifyTimeAfter(Long patientId, Integer result, LocalDateTime afterTime);
}
