package com.telemed.service.impl;

import com.telemed.common.dto.blockchain.BlockchainDepositRequestDTO;
import com.telemed.common.dto.blockchain.BlockchainDepositResponseDTO;
import com.telemed.service.BlockchainDepositStrategy;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Map;

@Slf4j
@Service
@ConditionalOnProperty(name = "blockchain.provider", havingValue = "mock", matchIfMissing = true)
public class MockBlockchainDepositStrategy extends AbstractBlockchainDepositStrategy implements BlockchainDepositStrategy {

    private static final String PROVIDER_NAME = "MOCK";

    @Override
    public String getProviderName() {
        return PROVIDER_NAME;
    }

    @Override
    public BlockchainDepositResponseDTO deposit(BlockchainDepositRequestDTO request) {
        log.info("模拟区块链存证, consultationNo: {}, fileHash: {}", request.getConsultationNo(), request.getFileHash());

        try {
            Map<String, Object> depositData = buildDepositData(request);
            String depositDataJson = objectMapper.writeValueAsString(depositData);

            Map<String, Object> mockResp = buildMockResponse(request.getFileHash());

            BlockchainDepositResponseDTO response = new BlockchainDepositResponseDTO();
            response.setProvider(PROVIDER_NAME);
            response.setSuccess(true);
            response.setTxHash((String) mockResp.get("txHash"));
            response.setBlockHeight((String) mockResp.get("blockHeight"));
            response.setBlockTime(convertTimestampToLocalDateTime((Long) mockResp.get("blockTime")));
            response.setDepositTimestamp(LocalDateTime.now());
            response.setVerifyUrl(buildVerifyUrl(request.getFileHash()));
            response.setRawResponse(depositDataJson);

            log.info("模拟区块链存证成功, consultationNo: {}, txHash: {}", request.getConsultationNo(), response.getTxHash());
            return response;
        } catch (Exception e) {
            BlockchainDepositResponseDTO response = new BlockchainDepositResponseDTO();
            response.setProvider(PROVIDER_NAME);
            response.setSuccess(false);
            response.setErrorMessage("模拟存证异常: " + e.getMessage());
            log.error("模拟区块链存证异常, consultationNo: {}", request.getConsultationNo(), e);
            return response;
        }
    }

    @Override
    public BlockchainDepositResponseDTO queryByTxHash(String txHash) {
        log.info("模拟区块链查询交易, txHash: {}", txHash);
        BlockchainDepositResponseDTO response = new BlockchainDepositResponseDTO();
        response.setProvider(PROVIDER_NAME);
        response.setTxHash(txHash);
        response.setSuccess(true);
        response.setDepositTimestamp(LocalDateTime.now());
        return response;
    }

    @Override
       public BlockchainDepositResponseDTO queryByFileHash(String fileHash) {
        log.info("模拟区块链查询文件哈希, fileHash: {}", fileHash);
        return queryByTxHash("0x" + fileHash.substring(0, 64));
    }
}
