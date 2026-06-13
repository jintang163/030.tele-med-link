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
@Table(name = "t_consultation_conclusion")
public class ConsultationConclusion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true)
    private Long consultationId;

    private Long doctorId;

    private Long patientId;

    @Column(columnDefinition = "TEXT")
    private String content;

    private String fileUrl;

    private LocalDateTime createTime;

    @Column(length = 128)
    private String depositHash;

    private LocalDateTime depositTimestamp;

    @Column(length = 128)
    private String depositTxHash;

    @Column(columnDefinition = "INT DEFAULT 0")
    private Integer depositStatus;

    @Column(columnDefinition = "INT DEFAULT 0")
    private Integer depositRetryCount;

    @Column(length = 32)
    private String depositProvider;

    @Column(length = 64)
    private String depositBlockHeight;

    private LocalDateTime depositBlockTime;

    @Column(length = 512)
    private String depositVerifyUrl;

    @PrePersist
    protected void onCreate() {
        createTime = LocalDateTime.now();
        if (depositStatus == null) {
            depositStatus = 0;
        }
        if (depositRetryCount == null) {
            depositRetryCount = 0;
        }
    }
}
