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
@Table(name = "t_asr_quality_issue")
public class AsrQualityIssue {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long reportId;

    private Long consultationId;

    @Column(length = 30)
    private String issueType;

    @Column(length = 30)
    private String severity;

    @Column(length = 500)
    private String description;

    @Column(length = 500)
    private String relatedText;

    @Column(length = 1000)
    private String suggestion;

    private Integer timelineStart;

    private Integer timelineEnd;

    private Boolean resolved;

    private LocalDateTime createTime;

    @PrePersist
    protected void onCreate() {
        createTime = LocalDateTime.now();
    }
}
