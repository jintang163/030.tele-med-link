package com.telemed.model.repository;

import com.telemed.model.entity.AsrQualityReport;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface AsrQualityReportRepository extends JpaRepository<AsrQualityReport, Long> {

    Optional<AsrQualityReport> findByConsultationId(Long consultationId);

    List<AsrQualityReport> findByDoctorIdOrderByCreateTimeDesc(Long doctorId);

    List<AsrQualityReport> findByPatientIdOrderByCreateTimeDesc(Long patientId);

    @Query("SELECT q FROM AsrQualityReport q WHERE q.overallScore IS NOT NULL AND q.doctorId = :doctorId ORDER BY q.createTime DESC")
    List<AsrQualityReport> findScoredByDoctorId(@Param("doctorId") Long doctorId);

    List<AsrQualityReport> findByStatusAndCreateTimeBetween(String status, LocalDateTime start, LocalDateTime end);

    void deleteByCreateTimeBefore(LocalDateTime before);
}
