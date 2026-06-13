package com.telemed.service;

import com.telemed.common.dto.asr.DiagnosisRequestDTO;
import com.telemed.common.vo.asr.DiagnosisSuggestionVO;

import java.util.List;

public interface DiagnosisAssistService {

    DiagnosisSuggestionVO generateSuggestion(DiagnosisRequestDTO request);

    DiagnosisSuggestionVO getSuggestionByConsultationId(Long consultationId);

    List<DiagnosisSuggestionVO> getPatientSuggestions(Long patientId);

    List<DiagnosisSuggestionVO> getDoctorSuggestions(Long doctorId);

    void seedKnowledgeBaseIfEmpty();
}
