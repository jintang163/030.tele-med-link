package com.telemed.model.repository;

import com.telemed.model.entity.VideoSegment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface VideoSegmentRepository extends JpaRepository<VideoSegment, Long> {

    List<VideoSegment> findByRecordingIdOrderBySegmentIndexAsc(Long recordingId);

    List<VideoSegment> findByRecordingIdAndUploadStatus(Long recordingId, Integer uploadStatus);

    Optional<VideoSegment> findByRecordingIdAndSegmentIndex(Long recordingId, Integer segmentIndex);

    long countByRecordingIdAndUploadStatus(Long recordingId, Integer uploadStatus);

    void deleteByRecordingId(Long recordingId);
}
