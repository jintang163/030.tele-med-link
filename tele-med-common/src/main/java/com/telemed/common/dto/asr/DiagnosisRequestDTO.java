package com.telemed.common.dto.asr;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DiagnosisRequestDTO {

    private Long consultationId;

    private Long patientId;

    private Long doctorId;

    private String department;

    private String patientComplaint;

    private String medicalHistory;

    private String imagingFindings;

    private String vitalSigns;

    private String labResults;

    private Boolean generateSuggestions = true;
}
