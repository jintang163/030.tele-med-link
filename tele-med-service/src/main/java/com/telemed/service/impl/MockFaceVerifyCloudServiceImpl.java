package com.telemed.service.impl;

import com.telemed.common.constant.FaceVerifyConstants;
import com.telemed.common.vo.faceverify.FaceVerifyCloudResponse;
import com.telemed.service.FaceVerifyCloudService;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
@ConditionalOnProperty(name = "face-verify.provider", havingValue = "mock", matchIfMissing = true)
public class MockFaceVerifyCloudServiceImpl implements FaceVerifyCloudService {

    @Override
    public FaceVerifyCloudResponse idCardFaceCompare(String idCardName, String idCardNo, String faceImageBase64) {
        FaceVerifyCloudResponse response = new FaceVerifyCloudResponse();
        response.setRequestId(UUID.randomUUID().toString().replace("-", ""));
        response.setPassed(true);
        response.setSimilarityScore(0.92);
        response.setRawResponse("{\"code\":\"OK\",\"score\":0.92}");
        return response;
    }

    @Override
    public FaceVerifyCloudResponse liveDetect(String faceImageBase64, String liveData) {
        FaceVerifyCloudResponse response = new FaceVerifyCloudResponse();
        response.setRequestId(UUID.randomUUID().toString().replace("-", ""));
        response.setPassed(true);
        response.setSimilarityScore(1.0);
        response.setRawResponse("{\"code\":\"OK\",\"live\":true}");
        return response;
    }

    @Override
    public FaceVerifyCloudResponse liveAndCompare(String idCardName, String idCardNo,
                                                    String faceImageBase64, String liveData) {
        FaceVerifyCloudResponse response = new FaceVerifyCloudResponse();
        response.setRequestId(UUID.randomUUID().toString().replace("-", ""));
        response.setPassed(true);
        response.setSimilarityScore(0.88);
        response.setRawResponse("{\"code\":\"OK\",\"score\":0.88,\"live\":true}");
        return response;
    }
}
