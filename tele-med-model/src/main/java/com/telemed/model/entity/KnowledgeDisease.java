package com.telemed.model.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Lob;
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
@Table(name = "t_knowledge_disease")
public class KnowledgeDisease {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(length = 200)
    private String diseaseName;

    @Column(length = 100)
    private String icdCode;

    @Column(length = 100)
    private String department;

    @Lob
    @Column(columnDefinition = "TEXT")
    private String keywords;

    @Lob
    @Column(columnDefinition = "TEXT")
    private String symptoms;

    @Lob
    @Column(columnDefinition = "TEXT")
    private String commonSigns;

    @Lob
    @Column(columnDefinition = "TEXT")
    private String relatedImagingFeatures;

    @Lob
    @Column(columnDefinition = "TEXT")
    private String recommendedTests;

    @Lob
    @Column(columnDefinition = "TEXT")
    private String typicalTreatments;

    @Lob
    @Column(columnDefinition = "TEXT")
    private String differentialFrom;

    private Double severityWeight;

    @Column(length = 20)
    private String status;

    private LocalDateTime createTime;

    @PrePersist
    protected void onCreate() {
        createTime = LocalDateTime.now();
        status = "ACTIVE";
    }
}
