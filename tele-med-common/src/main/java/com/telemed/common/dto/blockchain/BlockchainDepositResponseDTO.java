package com.telemed.common.dto.blockchain;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class BlockchainDepositResponseDTO {

    private boolean success;

    private String txHash;

    private String blockHeight;

    private LocalDateTime blockTime;

    private LocalDateTime depositTimestamp;

    private String verifyUrl;

    private String provider;

    private String errorMessage;

    private String rawResponse;
}
