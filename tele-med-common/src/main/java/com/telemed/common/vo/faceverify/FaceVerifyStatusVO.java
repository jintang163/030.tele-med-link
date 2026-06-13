package com.telemed.common.vo.faceverify;

import lombok.Data;

@Data
public class FaceVerifyStatusVO {

    private Long patientId;

    private Integer failureCount;

    private Integer remainingAttempts;

    private Boolean locked;

    private String lockTime;

    private String lastVerifyTime;

    private Integer lastVerifyResult;
}
