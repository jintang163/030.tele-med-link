package com.telemed.model.repository;

import com.telemed.model.entity.AsrQualityIssue;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface AsrQualityIssueRepository extends JpaRepository<AsrQualityIssue, Long> {

    List<AsrQualityIssue> findByReportIdOrderBySeverityDesc(Long reportId);

    List<AsrQualityIssue> findByConsultationIdOrderBySeverityDesc(Long consultationId);

    List<AsrQualityIssue> findByReportIdAndIssueType(Long reportId, String issueType);

    Long countByReportIdAndResolvedTrue(Long reportId);

    void deleteByReportId(Long reportId);
}
