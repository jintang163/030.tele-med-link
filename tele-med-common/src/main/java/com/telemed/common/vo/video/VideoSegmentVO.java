package com.telemed.common.vo.video;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class VideoSegmentVO {

    private Long id;

    private Long recordingId;

    private Integer segmentIndex;

    private String fileName;

    private Long fileSize;

    private Integer duration;

    private Integer uploadStatus;

    private String uploadStatusText;

    private LocalDateTime uploadTime;
}
