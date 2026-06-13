package com.telemed.service.impl;

import com.telemed.common.constant.FaceVerifyConstants;
import com.telemed.common.exception.BusinessException;
import com.telemed.model.entity.FaceVerifyToken;
import com.telemed.model.repository.FaceVerifyTokenRepository;
import com.telemed.service.FaceVerifyTokenService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
public class FaceVerifyTokenServiceImpl implements FaceVerifyTokenService {

    private final FaceVerifyTokenRepository tokenRepository;
    private final StringRedisTemplate redisTemplate;

    @Value("${face-verify.token-expire-minutes:5}")
    private long tokenExpireMinutes;

    private static final String TOKEN_CACHE_PREFIX = "face_verify:token:";

    @Override
    @Transactional
    public FaceVerifyToken issueToken(Long patientId, Integer tokenType) {
        String tokenValue = UUID.randomUUID().toString().replace("-", "")
                + UUID.randomUUID().toString().replace("-", "").substring(0, 16);

        LocalDateTime expireTime = LocalDateTime.now().plusMinutes(tokenExpireMinutes);

        FaceVerifyToken token = new FaceVerifyToken();
        token.setToken(tokenValue);
        token.setPatientId(patientId);
        token.setTokenType(tokenType);
        token.setExpireTime(expireTime);
        token.setUsed(FaceVerifyConstants.TOKEN_UNUSED);
        token = tokenRepository.save(token);

        String cacheKey = TOKEN_CACHE_PREFIX + tokenValue;
        redisTemplate.opsForValue().set(cacheKey, patientId + ":" + tokenType,
                tokenExpireMinutes, TimeUnit.MINUTES);

        return token;
    }

    @Override
    @Transactional
    public FaceVerifyToken validateToken(String token, Integer tokenType, String resource) {
        if (token == null || token.isEmpty()) {
            throw new BusinessException(401, "人脸核验令牌不能为空");
        }

        FaceVerifyToken tokenEntity = tokenRepository.findValidToken(token, LocalDateTime.now())
                .orElseThrow(() -> new BusinessException(401, "人脸核验令牌无效或已过期"));

        if (tokenType != null && !tokenType.equals(tokenEntity.getTokenType())
                && !FaceVerifyConstants.TOKEN_TYPE_GENERAL.equals(tokenEntity.getTokenType())) {
            throw new BusinessException(401, "人脸核验令牌类型不匹配");
        }

        markUsed(tokenEntity.getId(), resource);
        return tokenEntity;
    }

    @Override
    public boolean isValidToken(String token, Long patientId) {
        if (token == null || token.isEmpty()) {
            return false;
        }
        try {
            FaceVerifyToken tokenEntity = tokenRepository.findValidToken(token, LocalDateTime.now())
                    .orElse(null);
            if (tokenEntity == null) return false;
            if (patientId != null && !patientId.equals(tokenEntity.getPatientId())) return false;
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    @Override
    @Transactional
    public void markUsed(Long tokenId, String resource) {
        tokenRepository.markUsed(tokenId, LocalDateTime.now(), resource);
        String token = tokenRepository.findById(tokenId)
                .map(FaceVerifyToken::getToken)
                .orElse(null);
        if (token != null) {
            redisTemplate.delete(TOKEN_CACHE_PREFIX + token);
        }
    }
}
