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
@Table(name = "t_consultation_signature")
public class ConsultationSignature {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long consultationId;

    @Column(nullable = false)
    private Long doctorId;

    private Integer signOrder;

    private Integer signStatus;

    private String signatureImageUrl;

    @Column(columnDefinition = "TEXT")
    private String signatureData;

    @Column(columnDefinition = "TEXT")
    private String sm2PublicKey;

    @Column(columnDefinition = "TEXT")
    private String sm2Signature;

    private Float signPositionX;

    private Float signPositionY;

    private Float signWidth;

    private Float signHeight;

    private Integer signPage;

    private String signReason;

    private String signLocation;

    private LocalDateTime signTime;

    private LocalDateTime createTime;

    @PrePersist
    protected void onCreate() {
        createTime = LocalDateTime.now();
    }
}
