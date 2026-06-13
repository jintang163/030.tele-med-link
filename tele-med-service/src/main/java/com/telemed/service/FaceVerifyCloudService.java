package com.telemed.service;

import com.telemed.common.vo.faceverify.FaceVerifyCloudResponse;

public interface FaceVerifyCloudService {

    FaceVerifyCloudResponse idCardFaceCompare(String idCardName, String idCardNo, String faceImageBase64);

    FaceVerifyCloudResponse liveDetect(String faceImageBase64, String liveData);

    FaceVerifyCloudResponse liveAndCompare(String idCardName, String idCardNo,
                                           String faceImageBase64, String liveData);
}
