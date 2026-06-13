package com.telemed.model.repository;

import com.telemed.model.entity.VideoRecording;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface VideoRecordingRepository extends JpaRepository<VideoRecording, Long> {

    Optional<VideoRecording> findByConsultationId(Long consultationId);

    List<VideoRecording> findByDoctorIdAndStatus(Long doctorId, Integer status);

    List<VideoRecording> findByPatientIdAndStatus(Long patientId, Integer status);

    List<VideoRecording> findByPatientId(Long patientId);

    @Query("SELECT v FROM VideoRecording v WHERE v.status = ?1 AND v.expireTime < ?2")
    List<VideoRecording> findByStatusAndExpireTimeBefore(Integer status, LocalDateTime expireTime);

    @Query("SELECT v FROM VideoRecording v WHERE v.status IN ?1 AND v.endTime < ?2")
    List<VideoRecording> findByStatusInAndEndTimeBefore(List<Integer> statuses, LocalDateTime endTime);

    List<VideoRecording> findByConsultationIdIn(List<Long> consultationIds);
}
