package com.telemed.common.dto.video;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class VideoPlaybackAuthDTO {

    private Long recordingId;

    private Long userId;

    private String userRole;

    private Integer expireMinutes;
}
