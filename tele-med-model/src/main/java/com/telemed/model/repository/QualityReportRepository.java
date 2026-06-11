package com.telemed.model.repository;

import com.telemed.model.entity.QualityReport;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;

public interface QualityReportRepository extends JpaRepository<QualityReport, Long> {

    List<QualityReport> findByConsultationIdAndUserIdAndKindOrderByCreateTimeDesc(Long consultationId, Long userId, String kind, Pageable pageable);

    List<QualityReport> findByConsultationIdOrderByCreateTimeDesc(Long consultationId, Pageable pageable);

    void deleteByCreateTimeBefore(LocalDateTime before);
}
