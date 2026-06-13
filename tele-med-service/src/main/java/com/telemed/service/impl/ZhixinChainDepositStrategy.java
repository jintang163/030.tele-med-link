package com.telemed.service.impl;

import cn.hutool.http.HttpRequest;
import cn.hutool.http.HttpResponse;
import com.telemed.common.dto.blockchain.BlockchainDepositRequestDTO;
import com.telemed.common.dto.blockchain.BlockchainDepositResponseDTO;
import com.telemed.service.BlockchainDepositStrategy;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.LocalDateTime;
import java.util.Map;

@Slf4j
@Service
@ConditionalOnProperty(name = "blockchain.provider", havingValue = "zhixinchain", matchIfMissing = false)
public class ZhixinChainDepositStrategy extends AbstractBlockchainDepositStrategy implements BlockchainDepositStrategy {

    private static final String PROVIDER_NAME = "ZHIXINCHAIN";

    @Value("${blockchain.zhixinchain.endpoint:https://api.zhixinchain.com}")
    private String endpoint;

    @Value("${blockchain.zhixinchain.app-id:}")
    private String appId;

    @Value("${blockchain.zhixinchain.app-secret:}")
    private String appSecret;

    @Value("${blockchain.zhixinchain.biz-id:}")
    private String bizId;

    @Value("${blockchain.zhixinchain.enabled:false}")
    private boolean enabled;

    @Override
    public String getProviderName() {
        return PROVIDER_NAME;
    }

    @Override
    public BlockchainDepositResponseDTO deposit(BlockchainDepositRequestDTO request) {
        BlockchainDepositResponseDTO response = new BlockchainDepositResponseDTO();
        response.setProvider(PROVIDER_NAME);

        try {
            Map<String, Object> depositData = buildDepositData(request);
            String depositDataJson = objectMapper.writeValueAsString(depositData);
            log.info("至信链存证请求, consultationNo: {}, data: {}", request.getConsultationNo(), depositDataJson);

            if (!enabled) {
                log.warn("至信链未启用，使用模拟模式");
                Map<String, Object> mockResp = buildMockResponse(request.getFileHash());
                response.setSuccess(true);
                response.setTxHash((String) mockResp.get("txHash"));
                response.setBlockHeight((String) mockResp.get("blockHeight"));
                response.setBlockTime(convertTimestampToLocalDateTime((Long) mockResp.get("blockTime")));
                response.setDepositTimestamp(convertTimestampToLocalDateTime((Long) mockResp.get("timestamp")));
                response.setVerifyUrl(buildVerifyUrl(request.getFileHash()));
                response.setRawResponse(objectMapper.writeValueAsString(mockResp));
                return response;
            }

            String timestamp = String.valueOf(System.currentTimeMillis());
            String nonce = generateNonce();
            String signature = generateSignature(appId, appSecret, timestamp, nonce, depositDataJson);

            Map<String, Object> requestBody = Map.of(
                    "appId", appId,
                    "bizId", bizId,
                    "timestamp", timestamp,
                    "nonce", nonce,
                    "signature", signature,
                    "evidence", Map.of(
                            "fileHash", request.getFileHash(),
                            "hashAlgorithm", request.getHashAlgorithm() != null ? request.getHashAlgorithm() : "SHA-256",
                            "evidenceData", depositDataJson,
                            "evidenceType", "PDF_CONSULTATION",
                            "businessNo", request.getConsultationNo()
                    )
            );

            HttpResponse httpResponse = HttpRequest.post(endpoint + "/api/v2/evidence/deposit")
                    .header("Content-Type", "application/json")
                    .header("X-App-Id", appId)
                    .header("X-Timestamp", timestamp)
                    .header("X-Nonce", nonce)
                    .header("X-Signature", signature)
                    .body(objectMapper.writeValueAsString(requestBody))
                    .timeout(30000)
                    .execute();

            String responseBody = httpResponse.body();
            response.setRawResponse(responseBody);

            if (httpResponse.isOk()) {
                Map<String, Object> resultMap = objectMapper.readValue(responseBody, Map.class);
                Integer code = (Integer) resultMap.get("code");
                if (code != null && code == 0) {
                    Map<String, Object> data = (Map<String, Object>) resultMap.get("data");
                    response.setSuccess(true);
                    response.setTxHash((String) data.get("txHash"));
                    response.setBlockHeight(String.valueOf(data.get("blockHeight")));
                    Object blockTime = data.get("blockTime");
                    if (blockTime != null) {
                        if (blockTime instanceof Number) {
                            response.setBlockTime(convertTimestampToLocalDateTime(((Number) blockTime).longValue()));
                        } else if (blockTime instanceof String) {
                            response.setBlockTime(LocalDateTime.parse((String) blockTime,
                                    java.time.format.DateTimeFormatter.ISO_LOCAL_DATE_TIME));
                        }
                    }
                    response.setDepositTimestamp(convertTimestampToLocalDateTime(System.currentTimeMillis()));
                    response.setVerifyUrl(buildVerifyUrl(request.getFileHash()));
                    log.info("至信链存证成功, consultationNo: {}, txHash: {}", request.getConsultationNo(), response.getTxHash());
                } else {
                    response.setSuccess(false);
                    response.setErrorMessage((String) resultMap.get("msg"));
                    log.error("至信链存证失败, consultationNo: {}, code: {}, msg: {}",
                            request.getConsultationNo(), code, response.getErrorMessage());
                }
            } else {
                response.setSuccess(false);
                response.setErrorMessage("HTTP请求失败，状态码: " + httpResponse.getStatus());
                log.error("至信链存证HTTP请求失败, consultationNo: {}, status: {}, body: {}",
                        request.getConsultationNo(), httpResponse.getStatus(), responseBody);
            }
        } catch (Exception e) {
            response.setSuccess(false);
            response.setErrorMessage("存证异常: " + e.getMessage());
            log.error("至信链存证异常, consultationNo: {}", request.getConsultationNo(), e);
        }

        return response;
    }

    @Override
    public BlockchainDepositResponseDTO queryByTxHash(String txHash) {
        BlockchainDepositResponseDTO response = new BlockchainDepositResponseDTO();
        response.setProvider(PROVIDER_NAME);
        response.setTxHash(txHash);

        try {
            if (!enabled) {
                log.warn("至信链未启用，查询使用模拟模式");
                response.setSuccess(true);
                return response;
            }

            String timestamp = String.valueOf(System.currentTimeMillis());
            String nonce = generateNonce();
            String signature = generateSignature(appId, appSecret, timestamp, nonce, txHash);

            Map<String, Object> requestBody = Map.of(
                    "appId", appId,
                    "txHash", txHash,
                    "timestamp", timestamp,
                    "nonce", nonce
            );

            HttpResponse httpResponse = HttpRequest.post(endpoint + "/api/v2/evidence/queryByTxHash")
                    .header("Content-Type", "application/json")
                    .header("X-App-Id", appId)
                    .header("X-Timestamp", timestamp)
                    .header("X-Nonce", nonce)
                    .header("X-Signature", signature)
                    .body(objectMapper.writeValueAsString(requestBody))
                    .timeout(30000)
                    .execute();

            String responseBody = httpResponse.body();
            response.setRawResponse(responseBody);

            if (httpResponse.isOk()) {
                Map<String, Object> resultMap = objectMapper.readValue(responseBody, Map.class);
                Integer code = (Integer) resultMap.get("code");
                response.setSuccess(code != null && code == 0);
                if (!response.isSuccess()) {
                    response.setErrorMessage((String) resultMap.get("msg"));
                }
            } else {
                response.setSuccess(false);
                response.setErrorMessage("HTTP请求失败，状态码: " + httpResponse.getStatus());
            }
        } catch (Exception e) {
            response.setSuccess(false);
            response.setErrorMessage("查询异常: " + e.getMessage());
            log.error("至信链查询交易异常, txHash: {}", txHash, e);
        }

        return response;
    }

    @Override
    public BlockchainDepositResponseDTO queryByFileHash(String fileHash) {
        BlockchainDepositResponseDTO response = new BlockchainDepositResponseDTO();
        response.setProvider(PROVIDER_NAME);

        try {
            if (!enabled) {
                log.warn("至信链未启用，查询使用模拟模式");
                response.setSuccess(true);
                return response;
            }

            String timestamp = String.valueOf(System.currentTimeMillis());
            String nonce = generateNonce();
            String signature = generateSignature(appId, appSecret, timestamp, nonce, fileHash);

            Map<String, Object> requestBody = Map.of(
                    "appId", appId,
                    "bizId", bizId,
                    "fileHash", fileHash,
                    "timestamp", timestamp,
                    "nonce", nonce
            );

            HttpResponse httpResponse = HttpRequest.post(endpoint + "/api/v2/evidence/queryByFileHash")
                    .header("Content-Type", "application/json")
                    .header("X-App-Id", appId)
                    .header("X-Timestamp", timestamp)
                    .header("X-Nonce", nonce)
                    .header("X-Signature", signature)
                    .body(objectMapper.writeValueAsString(requestBody))
                    .timeout(30000)
                    .execute();

            String responseBody = httpResponse.body();
            response.setRawResponse(responseBody);

            if (httpResponse.isOk()) {
                Map<String, Object> resultMap = objectMapper.readValue(responseBody, Map.class);
                Integer code = (Integer) resultMap.get("code");
                response.setSuccess(code != null && code == 0);
                if (!response.isSuccess()) {
                    response.setErrorMessage((String) resultMap.get("msg"));
                } else {
                    Map<String, Object> data = (Map<String, Object>) resultMap.get("data");
                    if (data != null) {
                        response.setTxHash((String) data.get("txHash"));
                        response.setBlockHeight(String.valueOf(data.get("blockHeight")));
                    }
                }
            } else {
                response.setSuccess(false);
                response.setErrorMessage("HTTP请求失败，状态码: " + httpResponse.getStatus());
            }
        } catch (Exception e) {
            response.setSuccess(false);
            response.setErrorMessage("查询异常: " + e.getMessage());
            log.error("至信链查询文件哈希异常, fileHash: {}", fileHash, e);
        }

        return response;
    }

    private String generateNonce() {
        return java.util.UUID.randomUUID().toString().replace("-", "").substring(0, 16);
    }

    private String generateSignature(String appId, String appSecret, String timestamp, String nonce, String body) {
        try {
            String signStr = appId + timestamp + nonce + body + appSecret;
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(signStr.getBytes(StandardCharsets.UTF_8));
            StringBuilder hexString = new StringBuilder();
            for (byte b : hash) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) {
                    hexString.append('0');
                }
                hexString.append(hex);
            }
            return hexString.toString();
        } catch (Exception e) {
            log.error("生成至信链签名失败", e);
            throw new RuntimeException("生成签名失败", e);
        }
    }
}
