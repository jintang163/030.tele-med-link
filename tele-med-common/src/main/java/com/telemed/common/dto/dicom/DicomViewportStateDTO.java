package com.telemed.common.dto.dicom;

import lombok.Data;

import java.util.Map;

@Data
public class DicomViewportStateDTO {

    private Long consultationId;

    private Long imageId;

    private String token;

    private Double windowCenter;

    private Double windowWidth;

    private Double scale;

    private Map<String, Double> translation;

    private Integer rotation;

    private Boolean invert;

    private Long operatorId;

    private String operatorName;

    private Long timestamp;
}
