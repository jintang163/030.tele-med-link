package com.telemed.common.vo.dicom;

import lombok.Data;

import java.util.List;
import java.util.Map;

@Data
public class DicomAnnotationVO {

    private String annotationId;

    private Long imageId;

    private String annotationType;

    private List<Map<String, Double>> coordinates;

    private Map<String, Object> properties;

    private Long creatorId;

    private String creatorName;

    private Long createTime;

    private Long updateTime;
}
