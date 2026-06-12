package com.telemed.model.repository;

import com.telemed.model.entity.DicomImage;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface DicomImageRepository extends JpaRepository<DicomImage, Long> {

    List<DicomImage> findByConsultationIdOrderByUploadTimeAsc(Long consultationId);

    List<DicomImage> findByConsultationIdAndSeriesUidOrderBySliceIndexAsc(Long consultationId, String seriesUid);

    List<DicomImage> findByConsultationIdAndStudyUidOrderByUploadTimeAsc(Long consultationId, String studyUid);

    List<DicomImage> findDistinctSeriesUidByConsultationId(Long consultationId);
}
