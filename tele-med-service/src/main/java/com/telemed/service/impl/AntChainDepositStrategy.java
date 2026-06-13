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

import java.util.Map;

@Slf4j
@Service
@ConditionalOnProperty(name = "blockchain.provider", havingValue = "antchain", matchIfMissing = false)
public class AntChainDepositStrategy extends AbstractBlockchainDepositStrategy implements BlockchainDepositStrategy {

    private static final String PROVIDER_NAME = "ANTCHAIN";

    @Value("${blockchain.antchain.endpoint:https://rest.antchain.aliyuncs.com}")
    private String endpoint;

    @Value("${blockchain.antchain.access-key-id:}")
    private String accessKeyId;

    @Value("${blockchain.antchain.access-key-secret:}")
    private String accessKeySecret;

    @Value("${blockchain.antchain.contract-name:}")
    private String contractName;

    @Value("${blockchain.antchain.method:deposit}")
    private String method;

    @Value("${blockchain.antchain.enabled:false}")
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
            log.info("蚂蚁链存证请求, consultationNo: {}, data: {}", request.getConsultationNo(), depositDataJson);

            if (!enabled) {
                log.warn("蚂蚁链未启用，使用模拟模式");
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

            Map<String, Object> requestBody = Map.of(
                    "accessKeyId", accessKeyId,
                    "contractName", contractName,
                    "method", method,
                    "requestId", "req_" + System.currentTimeMillis(),
                    "content", Map.of(
                            "fileHash", request.getFileHash(),
                            "fileHashAlgorithm", request.getHashAlgorithm() != null ? request.getHashAlgorithm() : "SHA-256",
                            "data", depositDataJson
                    )
            );

            HttpResponse httpResponse = HttpRequest.post(endpoint + "/api/contract/invoke")
                    .header("Content-Type", "application/json")
                    .header("Authorization", "Bearer " + accessKeySecret)
                    .body(objectMapper.writeValueAsString(requestBody))
                    .timeout(30000)
                    .execute();

            String responseBody = httpResponse.body();
            response.setRawResponse(responseBody);

            if (httpResponse.isOk()) {
                Map<String, Object> resultMap = objectMapper.readValue(responseBody, Map.class);
                Boolean success = (Boolean) resultMap.get("success");
                if (Boolean.TRUE.equals(success)) {
                    Map<String, Object> data = (Map<String, Object>) resultMap.get("data");
                    response.setSuccess(true);
                    response.setTxHash((String) data.get("txHash"));
                    response.setBlockHeight(String.valueOf(data.get("blockHeight")));
                    response.setBlockTime(convertTimestampToLocalDateTime(((Number) data.get("blockTime")).longValue()));
                    response.setDepositTimestamp(convertTimestampToLocalDateTime(System.currentTimeMillis()));
                    response.setVerifyUrl(buildVerifyUrl(request.getFileHash()));
                    log.info("蚂蚁链存证成功, consultationNo: {}, txHash: {}", request.getConsultationNo(), response.getTxHash());
                } else {
                    response.setSuccess(false);
                    response.setErrorMessage((String) resultMap.get("message"));
                    log.error("蚂蚁链存证失败, consultationNo: {}, error: {}", request.getConsultationNo(), response.getErrorMessage());
                }
            } else {
                response.setSuccess(false);
                response.setErrorMessage("HTTP请求失败，状态码: " + httpResponse.getStatus());
                log.error("蚂蚁链存证HTTP请求失败, consultationNo: {}, status: {}, body: {}",
                        request.getConsultationNo(), httpResponse.getStatus(), responseBody);
            }
        } catch (Exception e) {
            response.setSuccess(false);
            response.setErrorMessage("存证异常: " + e.getMessage());
            log.error("蚂蚁链存证异常, consultationNo: {}", request.getConsultationNo(), e);
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
                log.warn("蚂蚁链未启用，查询使用模拟模式");
                response.setSuccess(true);
                return response;
            }

            Map<String, Object> requestBody = Map.of(
                    "accessKeyId", accessKeyId,
                    "txHash", txHash,
                    "requestId", "query_" + System.currentTimeMillis()
            );

            HttpResponse httpResponse = HttpRequest.post(endpoint + "/api/transaction/query")
                    .header("Content-Type", "application/json")
                    .header("Authorization", "Bearer " + accessKeySecret)
                    .body(objectMapper.writeValueAsString(requestBody))
                    .timeout(30000)
                    .execute();

            String responseBody = httpResponse.body();
            response.setRawResponse(responseBody);

            if (httpResponse.isOk()) {
                Map<String, Object> resultMap = objectMapper.readValue(responseBody, Map.class);
                Boolean success = (Boolean) resultMap.get("success");
                response.setSuccess(Boolean.TRUE.equals(success));
                if (!response.isSuccess()) {
                    response.setErrorMessage((String) resultMap.get("message"));
                }
            } else {
                response.setSuccess(false);
                response.setErrorMessage("HTTP请求失败，状态码: " + httpResponse.getStatus());
            }
        } catch (Exception e) {
            response.setSuccess(false);
            response.setErrorMessage("查询异常: " + e.getMessage());
            log.error("蚂蚁链查询交易异常, txHash: {}", txHash, e);
        }

        return response;
    }

    @Override
    public BlockchainDepositResponseDTO queryByFileHash(String fileHash) {
        return queryByTxHash("0x" + fileHash.substring(0, 64));
    }
}
