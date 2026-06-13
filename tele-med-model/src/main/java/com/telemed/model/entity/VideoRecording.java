package com.telemed.model.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "t_video_recording")
public class VideoRecording {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long consultationId;

    private Long doctorId;

    private Long patientId;

    private Integer status;

    private Integer segmentDuration;

    private Integer totalSegments;

    private Integer totalDuration;

    @Column(length = 512)
    private String encryptionKey;

    @Column(length = 128)
    private String encryptionIv;

    @Column(length = 512)
    private String hlsPlaylistUrl;

    @Column(length = 128)
    private String hlsBucket;

    @Column(length = 512)
    private String hlsObjectName;

    @Column(length = 128)
    private String mp4Bucket;

    @Column(length = 512)
    private String mp4ObjectName;

    private Boolean doctorAuthorized = false;

    private Boolean patientAuthorized = false;

    @Column(length = 256)
    private String watermarkText;

    private LocalDateTime expireTime;

    private LocalDateTime startTime;

    private LocalDateTime endTime;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;

    @PrePersist
    protected void onCreate() {
        createTime = LocalDateTime.now();
        updateTime = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updateTime = LocalDateTime.now();
    }
}
