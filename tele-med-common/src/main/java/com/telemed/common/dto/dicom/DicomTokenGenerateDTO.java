package com.telemed.common.dto.dicom;

import lombok.Data;

@Data
public class DicomTokenGenerateDTO {

    private Long consultationId;

    private Long imageId;

    private Long requestUserId;

    private String requestUserName;
}
