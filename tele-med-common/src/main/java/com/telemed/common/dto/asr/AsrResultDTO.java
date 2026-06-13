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
public class AsrResultDTO {

    private Long consultationId;

    private String status;

    private String asrProvider;

    private Integer totalDuration;

    private String fullTranscript;

    private List<TranscriptSegment> segments;

    private String errorMessage;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TranscriptSegment {
        private String speakerRole;
        private Long speakerId;
        private String speakerName;
        private String text;
        private Integer startTime;
        private Integer endTime;
        private Double confidence;
    }
}
