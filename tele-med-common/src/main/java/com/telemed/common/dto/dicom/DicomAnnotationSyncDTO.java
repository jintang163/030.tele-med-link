package com.telemed.common.dto.dicom;

import lombok.Data;

import java.util.List;
import java.util.Map;

@Data
public class DicomAnnotationSyncDTO {

    private Long consultationId;

    private Long imageId;

    private String token;

    private String annotationId;

    private String annotationType;

    private List<Map<String, Double>> coordinates;

    private Map<String, Object> properties;

    private String operation;

    private Long operatorId;

    private String operatorName;

    private Long timestamp;
}
