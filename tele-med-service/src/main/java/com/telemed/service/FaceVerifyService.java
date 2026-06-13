package com.telemed.service;

import com.telemed.common.dto.faceverify.FaceVerifyRequestDTO;
import com.telemed.common.vo.faceverify.FaceVerifyResultVO;
import com.telemed.common.vo.faceverify.FaceVerifyStatusVO;

public interface FaceVerifyService {

    FaceVerifyResultVO verify(FaceVerifyRequestDTO requestDTO);

    FaceVerifyStatusVO getStatus(Long patientId);

    FaceVerifyResultVO unlockPatient(Long patientId, Long operatorId, String reason);

    boolean validateFaceToken(String token, Long patientId, Integer tokenType, String resource);
}
