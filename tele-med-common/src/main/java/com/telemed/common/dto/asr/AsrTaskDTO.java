package com.telemed.common.dto.asr;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AsrTaskDTO {
    private Long consultationId;
    private String consultationNo;
    private Long doctorId;
    private Long patientId;
    private String audioUrl;
    private Integer durationSeconds;
    private String source;
}
