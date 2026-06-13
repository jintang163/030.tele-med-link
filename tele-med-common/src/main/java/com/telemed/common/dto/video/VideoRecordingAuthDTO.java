package com.telemed.common.dto.video;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class VideoRecordingAuthDTO {

    private Long consultationId;

    private Long userId;

    private String userRole;

    private Boolean authorized;
}
