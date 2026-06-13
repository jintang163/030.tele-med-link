package com.telemed.common.dto.faceverify;

import lombok.Data;

@Data
public class FaceVerifyTokenValidateDTO {

    private String faceToken;

    private Long patientId;

    private Integer tokenType;

    private String resource;
}
