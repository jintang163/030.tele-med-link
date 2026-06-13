package com.telemed.model.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "t_blockchain_deposit_log")
public class BlockchainDepositLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long consultationId;

    @Column(length = 64)
    private String consultationNo;

    @Column(columnDefinition = "TEXT")
    private String pdfUrl;

    @Column(columnDefinition = "TEXT", nullable = false)
    private String fileHash;

    @Column(length = 32)
    private String hashAlgorithm;

    @Column(length = 32)
    private String depositProvider;

    @Column(columnDefinition = "INT DEFAULT 0")
    private Integer depositStatus;

    @Column(length = 128)
    private String txHash;

    @Column(length = 64)
    private String blockHeight;

    private LocalDateTime blockTime;

    private LocalDateTime depositTimestamp;

    @Column(columnDefinition = "TEXT")
    private String depositVerifyUrl;

    @Column(columnDefinition = "TEXT")
    private String depositData;

    @Column(columnDefinition = "INT DEFAULT 0")
    private Integer retryCount;

    @Column(columnDefinition = "TEXT")
    private String errorMessage;

    @Column(columnDefinition = "TEXT")
    private String errorStackTrace;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;

    @PrePersist
    protected void onCreate() {
        createTime = LocalDateTime.now();
        if (depositStatus == null) {
            depositStatus = 0;
        }
        if (retryCount == null) {
            retryCount = 0;
        }
        if (hashAlgorithm == null) {
            hashAlgorithm = "SHA-256";
        }
    }
}
