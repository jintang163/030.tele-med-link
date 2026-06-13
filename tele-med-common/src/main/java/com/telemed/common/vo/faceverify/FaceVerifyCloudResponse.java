package com.telemed.common.vo.faceverify;

import lombok.Data;

@Data
public class FaceVerifyCloudResponse {

    private Boolean passed;

    private Double similarityScore;

    private String requestId;

    private String errorCode;

    private String errorMsg;

    private String rawResponse;
}
