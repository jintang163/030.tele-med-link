package com.telemed.common.dto.video;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class VideoSegmentUploadDTO {

    private Long recordingId;

    private Long consultationId;

    private Integer segmentIndex;

    private String fileName;

    private Integer duration;

    private String encryptionIv;

    private String checksum;
}
