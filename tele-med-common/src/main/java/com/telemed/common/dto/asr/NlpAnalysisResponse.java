package com.telemed.common.dto.asr;

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
public class NlpAnalysisResponse {

    private Long consultationId;

    private Integer keyIndicatorScore;

    private Integer safetyScore;

    private Integer overallScore;

    private String summary;

    private String recommendations;

    private Boolean safetyRisksDetected;

    @Builder.Default
    private List<String> mentionedKeyIndicators = new ArrayList<>();

    @Builder.Default
    private List<String> missingKeyIndicators = new ArrayList<>();

    @Builder.Default
    private List<QualityIssueDetail> issues = new ArrayList<>();

    private String nlpModelVersion;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class QualityIssueDetail {
        private String issueType;
        private String severity;
        private String description;
        private String relatedText;
        private String suggestion;
        private Integer timelineStart;
        private Integer timelineEnd;
    }
}
