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
@Table(name = "t_diagnosis_suggestion")
public class DiagnosisSuggestion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long consultationId;

    private Long patientId;

    private Long doctorId;

    @Column(length = 100)
    private String department;

    @Lob
    @Column(columnDefinition = "TEXT")
    private String patientComplaint;

    @Lob
    @Column(columnDefinition = "TEXT")
    private String imagingFindings;

    @Column(length = 200)
    private String primaryDisease;

    private Double primaryConfidence;

    @Lob
    @Column(columnDefinition = "TEXT")
    private String primaryEvidence;

    @Column(length = 200)
    private String secondaryDisease1;

    private Double secondaryConfidence1;

    @Column(length = 200)
    private String secondaryDisease2;

    private Double secondaryConfidence2;

    @Column(length = 200)
    private String secondaryDisease3;

    private Double secondaryConfidence3;

    @Lob
    @Column(columnDefinition = "TEXT")
    private String relatedSymptoms;

    @Lob
    @Column(columnDefinition = "TEXT")
    private String recommendedTests;

    @Lob
    @Column(columnDefinition = "TEXT")
    private String differentialDiagnosis;

    @Column(length = 20)
    private String status;

    @Column(length = 300)
    private String disclaimer;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;

    @PrePersist
    protected void onCreate() {
        createTime = LocalDateTime.now();
        updateTime = LocalDateTime.now();
        status = "PENDING";
        disclaimer = "本结果由AI辅助诊断系统生成，仅供医生参考，不构成最终诊断结论。最终诊断请以医生判断为准。";
    }
}
