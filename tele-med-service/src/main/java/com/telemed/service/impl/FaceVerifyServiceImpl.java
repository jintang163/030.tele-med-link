package com.telemed.service.impl;

import com.telemed.common.constant.FaceVerifyConstants;
import com.telemed.common.dto.faceverify.FaceVerifyRequestDTO;
import com.telemed.common.exception.BusinessException;
import com.telemed.common.vo.faceverify.FaceVerifyCloudResponse;
import com.telemed.common.vo.faceverify.FaceVerifyResultVO;
import com.telemed.common.vo.faceverify.FaceVerifyStatusVO;
import com.telemed.model.entity.FaceVerifyCounter;
import com.telemed.model.entity.FaceVerifyLog;
import com.telemed.model.entity.FaceVerifyToken;
import com.telemed.model.repository.FaceVerifyCounterRepository;
import com.telemed.model.repository.FaceVerifyLogRepository;
import com.telemed.service.FaceVerifyCloudService;
import com.telemed.service.FaceVerifyService;
import com.telemed.service.FaceVerifyTokenService;
import com.telemed.service.MinioService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Base64;

@Service
@RequiredArgsConstructor
public class FaceVerifyServiceImpl implements FaceVerifyService {

    private final FaceVerifyCloudService faceVerifyCloudService;
    private final FaceVerifyTokenService faceVerifyTokenService;
    private final FaceVerifyLogRepository logRepository;
    private final FaceVerifyCounterRepository counterRepository;
    private final MinioService minioService;

    @Value("${face-verify.similarity-threshold:0.8}")
    private double similarityThreshold;

    @Value("${face-verify.max-failure-count:3}")
    private int maxFailureCount;

    @Value("${face-verify.bucket:tele-med-face}")
    private String faceBucket;

    @Override
    @Transactional
    public FaceVerifyResultVO verify(FaceVerifyRequestDTO requestDTO) {
        if (requestDTO.getPatientId() == null) {
            throw new BusinessException("患者ID不能为空");
        }
        if (requestDTO.getVerifyType() == null) {
            requestDTO.setVerifyType(FaceVerifyConstants.VERIFY_TYPE_ID_CARD_FACE);
        }

        FaceVerifyCounter counter = getOrCreateCounter(requestDTO.getPatientId());
        if (counter.getLocked() != null && counter.getLocked() == FaceVerifyConstants.LOCK_STATUS_LOCKED) {
            throw new BusinessException("账户已锁定，请联系管理员解锁");
        }

        String faceImageUrl = null;
        if (requestDTO.getFaceImageBase64() != null && !requestDTO.getFaceImageBase64().isEmpty()) {
            faceImageUrl = saveFaceImage(requestDTO);
        }

        FaceVerifyCloudResponse cloudResponse = callCloudVerify(requestDTO);

        boolean passed = cloudResponse.getPassed() != null && cloudResponse.getPassed()
                && cloudResponse.getSimilarityScore() != null
                && cloudResponse.getSimilarityScore() >= similarityThreshold;

        saveVerifyLog(requestDTO, cloudResponse, passed, faceImageUrl);

        FaceVerifyResultVO result = new FaceVerifyResultVO();
        result.setPassed(passed);
        result.setSimilarityScore(cloudResponse.getSimilarityScore());
        result.setRequestId(cloudResponse.getRequestId());

        if (passed) {
            counterRepository.resetFailureCount(requestDTO.getPatientId());

            FaceVerifyToken token = faceVerifyTokenService.issueToken(
                    requestDTO.getPatientId(), FaceVerifyConstants.TOKEN_TYPE_PDF_DOWNLOAD);
            result.setFaceToken(token.getToken());
            result.setTokenExpireTime(token.getExpireTime());

            int remaining = maxFailureCount;
            result.setRemainingAttempts(remaining);
            result.setLocked(false);
            result.setVerifyResultText("核验通过");
        } else {
            counterRepository.incrementFailureCount(requestDTO.getPatientId());

            FaceVerifyCounter updatedCounter = counterRepository
                    .findByPatientId(requestDTO.getPatientId()).orElse(counter);
            int newFailCount = updatedCounter.getFailureCount() != null ? updatedCounter.getFailureCount() + 1 : 1;
            int remaining = Math.max(0, maxFailureCount - newFailCount);
            result.setRemainingAttempts(remaining);
            result.setFailureReason(cloudResponse.getErrorMsg() != null
                    ? cloudResponse.getErrorMsg() : "人脸比对未通过");

            if (newFailCount >= maxFailureCount) {
                counterRepository.lockPatient(requestDTO.getPatientId());
                result.setLocked(true);
                result.setLockTime(LocalDateTime.now());
                result.setVerifyResultText("核验失败，账户已锁定");
            } else {
                result.setLocked(false);
                result.setVerifyResultText("核验失败，剩余" + remaining + "次机会");
            }
        }

        return result;
    }

    @Override
    public FaceVerifyStatusVO getStatus(Long patientId) {
        if (patientId == null) {
            throw new BusinessException("患者ID不能为空");
        }

        FaceVerifyStatusVO status = new FaceVerifyStatusVO();
        status.setPatientId(patientId);

        FaceVerifyCounter counter = counterRepository.findByPatientId(patientId).orElse(null);
        if (counter == null) {
            status.setFailureCount(0);
            status.setRemainingAttempts(maxFailureCount);
            status.setLocked(false);
        } else {
            status.setFailureCount(counter.getFailureCount() != null ? counter.getFailureCount() : 0);
            status.setRemainingAttempts(Math.max(0, maxFailureCount
                    - (counter.getFailureCount() != null ? counter.getFailureCount() : 0)));
            status.setLocked(counter.getLocked() != null
                    && counter.getLocked() == FaceVerifyConstants.LOCK_STATUS_LOCKED);
            if (counter.getLockTime() != null) {
                status.setLockTime(counter.getLockTime()
                        .format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
            }
        }

        var logs = logRepository.findByPatientIdOrderByVerifyTimeDesc(patientId);
        if (!logs.isEmpty()) {
            var lastLog = logs.get(0);
            status.setLastVerifyTime(lastLog.getVerifyTime() != null
                    ? lastLog.getVerifyTime().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")) : null);
            status.setLastVerifyResult(lastLog.getResult());
        }

        return status;
    }

    @Override
    @Transactional
    public FaceVerifyResultVO unlockPatient(Long patientId, Long operatorId, String reason) {
        FaceVerifyCounter counter = counterRepository.findByPatientId(patientId)
                .orElseThrow(() -> new BusinessException("患者核验记录不存在"));

        counter.setLocked(FaceVerifyConstants.LOCK_STATUS_UNLOCKED);
        counter.setFailureCount(0);
        counter.setUnlockTime(LocalDateTime.now());
        counter.setUnlockReason(reason);
        counter.setUnlockOperatorId(operatorId);
        counterRepository.save(counter);

        FaceVerifyResultVO result = new FaceVerifyResultVO();
        result.setPassed(false);
        result.setLocked(false);
        result.setRemainingAttempts(maxFailureCount);
        result.setVerifyResultText("已解锁");
        return result;
    }

    @Override
    public boolean validateFaceToken(String token, Long patientId, Integer tokenType, String resource) {
        try {
            FaceVerifyToken tokenEntity = faceVerifyTokenService.validateToken(token, tokenType, resource);
            if (patientId != null && !patientId.equals(tokenEntity.getPatientId())) {
                return false;
            }
            return true;
        } catch (BusinessException e) {
            return false;
        }
    }

    private FaceVerifyCounter getOrCreateCounter(Long patientId) {
        return counterRepository.findByPatientId(patientId)
                .orElseGet(() -> {
                    FaceVerifyCounter c = new FaceVerifyCounter();
                    c.setPatientId(patientId);
                    c.setFailureCount(0);
                    c.setLocked(FaceVerifyConstants.LOCK_STATUS_UNLOCKED);
                    return counterRepository.save(c);
                });
    }

    private FaceVerifyCloudResponse callCloudVerify(FaceVerifyRequestDTO requestDTO) {
        int type = requestDTO.getVerifyType() != null
                ? requestDTO.getVerifyType() : FaceVerifyConstants.VERIFY_TYPE_ID_CARD_FACE;

        String name = requestDTO.getIdCardName();
        String idCard = requestDTO.getIdCardNo();
        String faceImg = requestDTO.getFaceImageBase64();
        String liveData = requestDTO.getLiveData();

        return switch (type) {
            case FaceVerifyConstants.VERIFY_TYPE_LIVE_DETECT ->
                    faceVerifyCloudService.liveDetect(faceImg, liveData);
            case FaceVerifyConstants.VERIFY_TYPE_LIVE_ID_COMPARE ->
                    faceVerifyCloudService.liveAndCompare(name, idCard, faceImg, liveData);
            default ->
                    faceVerifyCloudService.idCardFaceCompare(name, idCard, faceImg);
        };
    }

    private void saveVerifyLog(FaceVerifyRequestDTO requestDTO, FaceVerifyCloudResponse cloudResponse,
                               boolean passed, String faceImageUrl) {
        FaceVerifyLog log = new FaceVerifyLog();
        log.setPatientId(requestDTO.getPatientId());
        log.setVerifyType(requestDTO.getVerifyType());
        log.setSimilarityScore(cloudResponse.getSimilarityScore());
        log.setResult(passed ? FaceVerifyConstants.RESULT_PASS : FaceVerifyConstants.RESULT_FAIL);
        log.setFailureReason(cloudResponse.getErrorMsg());
        log.setRequestId(cloudResponse.getRequestId());
        log.setIdCardName(requestDTO.getIdCardName());
        log.setIdCardNo(maskIdCard(requestDTO.getIdCardNo()));
        log.setFaceImageUrl(faceImageUrl);
        log.setRawResponse(cloudResponse.getRawResponse());
        log.setVerifySource(requestDTO.getVerifySource());
        log.setVerifyTime(LocalDateTime.now());
        logRepository.save(log);
    }

    private String saveFaceImage(FaceVerifyRequestDTO requestDTO) {
        try {
            String base64 = requestDTO.getFaceImageBase64();
            if (base64.contains(",")) {
                base64 = base64.substring(base64.indexOf(",") + 1);
            }
            byte[] bytes = Base64.getDecoder().decode(base64);
            String objectName = "faces/" + requestDTO.getPatientId() + "/"
                    + System.currentTimeMillis() + ".jpg";
            return minioService.uploadBytes(faceBucket, objectName, bytes, "image/jpeg");
        } catch (Exception e) {
            return null;
        }
    }

    private String maskIdCard(String idCard) {
        if (idCard == null || idCard.length() < 8) return idCard;
        return idCard.substring(0, 4) + "********" + idCard.substring(idCard.length() - 4);
    }
}
