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
public class AsrQualityReportVO {

    private Long id;

    private Long consultationId;

    private String consultationNo;

    private Long doctorId;

    private String doctorName;

    private Long patientId;

    private String patientName;

    private String status;

    private String fullTranscript;

    private Integer totalDuration;

    private Integer doctorTalkTime;

    private Integer patientTalkTime;

    private Integer keyIndicatorScore;

    private Integer safetyScore;

    private Integer overallScore;

    private String summary;

    private String recommendations;

    private Boolean safetyRisksDetected;

    private String asrProvider;

    @Builder.Default
    private List<String> mentionedKeyIndicators = new ArrayList<>();

    @Builder.Default
    private List<String> missingKeyIndicators = new ArrayList<>();

    @Builder.Default
    private List<AsrQualityIssueVO> issues = new ArrayList<>();

    @Builder.Default
    private List<TranscriptUtteranceVO> utterances = new ArrayList<>();

    private String createTime;

    private String updateTime;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TranscriptUtteranceVO {
        private Long id;
        private String speakerRole;
        private String speakerName;
        private String text;
        private Integer duration;
        private String createTime;
    }
}
