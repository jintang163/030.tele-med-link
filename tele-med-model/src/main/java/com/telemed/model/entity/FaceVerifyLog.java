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
@Table(name = "t_face_verify_log")
public class FaceVerifyLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long patientId;

    @Column(nullable = false)
    private Integer verifyType;

    private Double similarityScore;

    private Integer result;

    private String failureReason;

    private String requestId;

    private String idCardName;

    private String idCardNo;

    private String faceImageUrl;

    @Column(columnDefinition = "TEXT")
    private String rawResponse;

    private String verifySource;

    private String clientIp;

    private LocalDateTime verifyTime;

    private LocalDateTime createTime;

    @PrePersist
    protected void onCreate() {
        createTime = LocalDateTime.now();
        if (verifyTime == null) {
            verifyTime = LocalDateTime.now();
        }
    }
}
