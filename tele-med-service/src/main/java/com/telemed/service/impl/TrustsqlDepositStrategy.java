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
@ConditionalOnProperty(name = "blockchain.provider", havingValue = "trustsql", matchIfMissing = false)
public class TrustsqlDepositStrategy extends AbstractBlockchainDepositStrategy implements BlockchainDepositStrategy {

    private static final String PROVIDER_NAME = "TRUSTSQL";

    @Value("${blockchain.trustsql.endpoint:https://trustsql.qq.com}")
    private String endpoint;

    @Value("${blockchain.trustsql.app-id:}")
    private String appId;

    @Value("${blockchain.trustsql.app-secret:}")
    private String appSecret;

    @Value("${blockchain.trustsql.chain-id:}")
    private String chainId;

    @Value("${blockchain.trustsql.account:}")
    private String account;

    @Value("${blockchain.trustsql.enabled:false}")
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

            Map<String, Object> requestBody = Map.of(
                    "app_id", appId,
                    "chain_id", chainId,
                    "account", account,
                    "nonce", String.valueOf(System.currentTimeMillis()),
                    "operation", "issue",
                    "metadata", Map.of(
                            "fileHash", request.getFileHash(),
                            "fileHashAlgorithm", request.getHashAlgorithm() != null ? request.getHashAlgorithm() : "SHA-256",
                            "data", depositDataJson
                    )
            );

            HttpResponse httpResponse = HttpRequest.post(endpoint + "/api/v1/ledger/issue")
                    .header("Content-Type", "application/json")
                    .header("X-App-Secret", appSecret)
                    .body(objectMapper.writeValueAsString(requestBody))
                    .timeout(30000)
                    .execute();

            String responseBody = httpResponse.body();
            response.setRawResponse(responseBody);

            if (httpResponse.isOk()) {
                Map<String, Object> resultMap = objectMapper.readValue(responseBody, Map.class);
                Integer errCode = (Integer) resultMap.get("errCode");
                if (errCode != null && errCode == 0) {
                    Map<String, Object> data = (Map<String, Object>) resultMap.get("data");
                    response.setSuccess(true);
                    response.setTxHash((String) data.get("hash"));
                    response.setBlockHeight(String.valueOf(data.get("blockHeight")));
                    response.setBlockTime(convertTimestampToLocalDateTime(((Number) data.get("blockTimestamp")).longValue()));
                    response.setDepositTimestamp(convertTimestampToLocalDateTime(System.currentTimeMillis()));
                    response.setVerifyUrl(buildVerifyUrl(request.getFileHash()));
                    log.info("至信链存证成功, consultationNo: {}, txHash: {}", request.getConsultationNo(), response.getTxHash());
                } else {
                    response.setSuccess(false);
                    response.setErrorMessage((String) resultMap.get("errMsg"));
                    log.error("至信链存证失败, consultationNo: {}, error: {}", request.getConsultationNo(), response.getErrorMessage());
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

            Map<String, Object> requestBody = Map.of(
                    "app_id", appId,
                    "chain_id", chainId,
                    "hash", txHash,
                    "nonce", String.valueOf(System.currentTimeMillis())
            );

            HttpResponse httpResponse = HttpRequest.post(endpoint + "/api/v1/ledger/query")
                    .header("Content-Type", "application/json")
                    .header("X-App-Secret", appSecret)
                    .body(objectMapper.writeValueAsString(requestBody))
                    .timeout(30000)
                    .execute();

            String responseBody = httpResponse.body();
            response.setRawResponse(responseBody);

            if (httpResponse.isOk()) {
                Map<String, Object> resultMap = objectMapper.readValue(responseBody, Map.class);
                Integer errCode = (Integer) resultMap.get("errCode");
                response.setSuccess(errCode != null && errCode == 0);
                if (!response.isSuccess()) {
                    response.setErrorMessage((String) resultMap.get("errMsg"));
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
        return queryByTxHash("0x" + fileHash.substring(0, 64));
    }
}
