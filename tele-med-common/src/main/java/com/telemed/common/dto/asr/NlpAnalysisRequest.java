package com.telemed.common.dto.asr;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NlpAnalysisRequest {

    private Long consultationId;

    private String fullTranscript;

    private List<SegmentUtterance> utterances;

    private String department;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SegmentUtterance {
        private String speakerRole;
        private String text;
        private Integer startTime;
        private Integer endTime;
    }
}
