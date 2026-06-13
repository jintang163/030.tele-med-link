package com.telemed.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.telemed.common.dto.blockchain.BlockchainDepositRequestDTO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

@Slf4j
public abstract class AbstractBlockchainDepositStrategy {

    @Value("${blockchain.verify-base-url:http://localhost:8080/api/blockchain/verify}")
    protected String verifyBaseUrl;

    protected final ObjectMapper objectMapper = new ObjectMapper();

    protected Map<String, Object> buildDepositData(BlockchainDepositRequestDTO request) {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("consultationId", request.getConsultationId());
        data.put("consultationNo", request.getConsultationNo());
        data.put("fileHash", request.getFileHash());
        data.put("hashAlgorithm", request.getHashAlgorithm() != null ? request.getHashAlgorithm() : "SHA-256");
        data.put("pdfUrl", request.getPdfUrl());
        data.put("patientName", request.getPatientName());
        data.put("patientIdCard", maskIdCard(request.getPatientIdCard()));
        data.put("doctorName", request.getDoctorName());
        data.put("signTime", request.getSignTime() != null ?
                request.getSignTime().atZone(ZoneId.systemDefault()).toInstant().toEpochMilli() : null);
        data.put("timestamp", System.currentTimeMillis());
        if (request.getExtraData() != null) {
            data.putAll(request.getExtraData());
        }
        return data;
    }

    protected String buildVerifyUrl(String fileHash) {
        return verifyBaseUrl + "?hash=" + fileHash;
    }

    protected String maskIdCard(String idCard) {
        if (idCard == null || idCard.length() < 8) {
            return idCard;
        }
        return idCard.substring(0, 6) + "********" + idCard.substring(idCard.length() - 4);
    }

    protected LocalDateTime convertTimestampToLocalDateTime(Long timestamp) {
        if (timestamp == null) {
            return null;
        }
        return LocalDateTime.ofInstant(
                java.time.Instant.ofEpochMilli(timestamp),
                ZoneId.systemDefault()
        );
    }

    protected Map<String, Object> buildMockResponse(String fileHash) {
        Map<String, Object> mockResponse = new HashMap<>();
        mockResponse.put("success", true);
        mockResponse.put("txHash", "0x" + fileHash.substring(0, 64));
        mockResponse.put("blockHeight", String.valueOf(System.currentTimeMillis() / 1000));
        mockResponse.put("blockTime", System.currentTimeMillis());
        mockResponse.put("timestamp", System.currentTimeMillis());
        return mockResponse;
    }
}
