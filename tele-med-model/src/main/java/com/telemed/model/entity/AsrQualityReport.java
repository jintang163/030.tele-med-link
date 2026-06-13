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
@Table(name = "t_asr_quality_report")
public class AsrQualityReport {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long consultationId;

    @Column(length = 50)
    private String consultationNo;

    private Long doctorId;

    @Column(length = 100)
    private String doctorName;

    private Long patientId;

    @Column(length = 100)
    private String patientName;

    @Column(length = 20)
    private String status;

    @Lob
    @Column(columnDefinition = "TEXT")
    private String fullTranscript;

    private Integer totalDuration;

    private Integer doctorTalkTime;

    private Integer patientTalkTime;

    private Integer keyIndicatorScore;

    private Integer safetyScore;

    private Integer overallScore;

    @Lob
    @Column(columnDefinition = "TEXT")
    private String summary;

    @Lob
    @Column(columnDefinition = "TEXT")
    private String recommendations;

    private Boolean safetyRisksDetected;

    @Lob
    @Column(columnDefinition = "TEXT")
    private String nlpMetadata;

    @Column(length = 100)
    private String asrProvider;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;

    @PrePersist
    protected void onCreate() {
        createTime = LocalDateTime.now();
        updateTime = LocalDateTime.now();
    }
}
