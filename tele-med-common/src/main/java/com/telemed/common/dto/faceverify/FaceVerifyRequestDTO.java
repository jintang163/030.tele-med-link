package com.telemed.common.dto.faceverify;

import lombok.Data;

@Data
public class FaceVerifyRequestDTO {

    private Long patientId;

    private Integer verifyType;

    private String idCardName;

    private String idCardNo;

    private String faceImageBase64;

    private String liveData;

    private String verifySource;
}
