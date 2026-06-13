package com.telemed.web.controller;

import com.telemed.common.dto.asr.DiagnosisRequestDTO;
import com.telemed.common.result.Result;
import com.telemed.common.vo.asr.DiagnosisSuggestionVO;
import com.telemed.service.DiagnosisAssistService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/diagnosis-assist")
@RequiredArgsConstructor
public class DiagnosisAssistController {

    private final DiagnosisAssistService diagnosisAssistService;

    @PostMapping("/suggestion/generate")
    public Result<DiagnosisSuggestionVO> generateSuggestion(@RequestBody DiagnosisRequestDTO request) {
        DiagnosisSuggestionVO vo = diagnosisAssistService.generateSuggestion(request);
        return Result.ok(vo);
    }

    @GetMapping("/suggestion/consultation/{consultationId}")
    public Result<DiagnosisSuggestionVO> getSuggestionByConsultationId(@PathVariable Long consultationId) {
        DiagnosisSuggestionVO vo = diagnosisAssistService.getSuggestionByConsultationId(consultationId);
        return Result.ok(vo);
    }

    @GetMapping("/suggestion/patient/{patientId}")
    public Result<List<DiagnosisSuggestionVO>> getPatientSuggestions(@PathVariable Long patientId) {
        List<DiagnosisSuggestionVO> list = diagnosisAssistService.getPatientSuggestions(patientId);
        return Result.ok(list);
    }

    @GetMapping("/suggestion/doctor/{doctorId}")
    public Result<List<DiagnosisSuggestionVO>> getDoctorSuggestions(@PathVariable Long doctorId) {
        List<DiagnosisSuggestionVO> list = diagnosisAssistService.getDoctorSuggestions(doctorId);
        return Result.ok(list);
    }
}
