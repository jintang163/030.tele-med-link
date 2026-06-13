package com.telemed.model.repository;

import com.telemed.model.entity.ConsultationAsrTranscript;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ConsultationAsrTranscriptRepository extends JpaRepository<ConsultationAsrTranscript, Long> {

    List<ConsultationAsrTranscript> findByConsultationIdOrderByCreateTimeAsc(Long consultationId);

    List<ConsultationAsrTranscript> findByConsultationIdAndSpeakerRoleOrderByCreateTimeAsc(Long consultationId, String speakerRole);

    void deleteByConsultationId(Long consultationId);
}
