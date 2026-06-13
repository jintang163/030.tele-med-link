package com.telemed.common.vo.video;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class VideoPlaybackAuthVO {

    private Long recordingId;

    private String authToken;

    private String hlsPlaylistUrl;

    private String encryptionKey;

    private LocalDateTime expireTime;
}
