package com.telemed.common.vo.blockchain;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class BlockchainDepositVO {

    private Long id;

    private Long consultationId;

    private String consultationNo;

    private String pdfUrl;

    private String fileHash;

    private String hashAlgorithm;

    private String depositProvider;

    private Integer depositStatus;

    private String depositStatusText;

    private String txHash;

    private String blockHeight;

    private LocalDateTime blockTime;

    private LocalDateTime depositTimestamp;

    private String depositVerifyUrl;

    private Integer retryCount;

    private String errorMessage;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;
}
