package com.telemed.common.vo.dicom;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class DicomImageVO {

    private Long id;

    private Long consultationId;

    private String objectName;

    private String fileName;

    private String patientName;

    private String studyUid;

    private String seriesUid;

    private String instanceUid;

    private String modality;

    private String studyDescription;

    private String seriesDescription;

    private Integer sliceIndex;

    private Long uploaderId;

    private String uploaderName;

    private Long fileSize;

    private Integer width;

    private Integer height;

    private Double windowCenter;

    private Double windowWidth;

    private LocalDateTime uploadTime;
}
