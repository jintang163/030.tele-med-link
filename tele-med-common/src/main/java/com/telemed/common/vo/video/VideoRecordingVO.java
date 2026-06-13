package com.telemed.common.vo.video;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class VideoRecordingVO {

    private Long id;

    private Long consultationId;

    private String consultationNo;

    private Long doctorId;

    private String doctorName;

    private Long patientId;

    private String patientName;

    private Integer status;

    private String statusText;

    private Integer totalSegments;

    private Integer uploadedSegments;

    private Integer totalDuration;

    private String hlsPlaylistUrl;

    private Boolean doctorAuthorized;

    private Boolean patientAuthorized;

    private String watermarkText;

    private LocalDateTime startTime;

    private LocalDateTime endTime;

    private LocalDateTime expireTime;

    private String createTime;
}
