package com.telemed.service;

import com.telemed.common.constant.FaceVerifyConstants;
import com.telemed.model.entity.FaceVerifyToken;

public interface FaceVerifyTokenService {

    FaceVerifyToken issueToken(Long patientId, Integer tokenType);

    default FaceVerifyToken issueToken(Long patientId) {
        return issueToken(patientId, FaceVerifyConstants.TOKEN_TYPE_GENERAL);
    }

    FaceVerifyToken validateToken(String token, Integer tokenType, String resource);

    boolean isValidToken(String token, Long patientId);

    void markUsed(Long tokenId, String resource);
}
