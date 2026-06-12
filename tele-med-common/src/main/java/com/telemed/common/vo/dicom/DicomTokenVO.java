package com.telemed.common.vo.dicom;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class DicomTokenVO {

    private String token;

    private Long imageId;

    private Long consultationId;

    private LocalDateTime expireTime;

    private Long expireMinutes;

    private DicomImageVO imageInfo;
}
