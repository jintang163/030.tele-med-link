package com.telemed.common.dto.dicom;

import lombok.Data;

@Data
public class DicomUploadDTO {

    private Long consultationId;

    private Long uploaderId;

    private String uploaderName;

    private String patientName;

    private String studyUid;

    private String seriesUid;

    private String instanceUid;

    private String modality;

    private String studyDescription;

    private String seriesDescription;

    private Integer sliceIndex;
}
