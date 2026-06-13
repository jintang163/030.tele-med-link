package com.telemed.common.vo.video;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class VideoRecordingKeyVO {

    private Long recordingId;

    private String encryptionKey;

    private String encryptionIv;
}
