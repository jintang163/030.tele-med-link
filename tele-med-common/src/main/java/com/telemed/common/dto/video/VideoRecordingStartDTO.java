package com.telemed.common.dto.video;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class VideoRecordingStartDTO {

    private Long consultationId;

    private Long doctorId;

    private String watermarkText;

    private Integer segmentDuration;
}
