package com.telemed.common.vo.asr;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DiagnosisSuggestionVO {

    private Long id;

    private Long consultationId;

    private Long patientId;

    private Long doctorId;

    private String department;

    private String patientComplaint;

    private String imagingFindings;

    private String primaryDisease;

    private Double primaryConfidence;

    private String primaryEvidence;

    private String secondaryDisease1;

    private Double secondaryConfidence1;

    private String secondaryDisease2;

    private Double secondaryConfidence2;

    private String secondaryDisease3;

    private Double secondaryConfidence3;

    @Builder.Default
    private List<String> relatedSymptoms = new ArrayList<>();

    @Builder.Default
    private List<String> recommendedTests = new ArrayList<>();

    private String differentialDiagnosis;

    private String status;

    private String disclaimer;

    private String createTime;
}
