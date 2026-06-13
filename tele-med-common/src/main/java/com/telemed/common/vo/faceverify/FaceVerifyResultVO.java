package com.telemed.common.vo.faceverify;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class FaceVerifyResultVO {

    private Boolean passed;

    private Double similarityScore;

    private String failureReason;

    private String requestId;

    private String faceToken;

    private LocalDateTime tokenExpireTime;

    private Integer remainingAttempts;

    private Boolean locked;

    private LocalDateTime lockTime;

    private String verifyResultText;
}
