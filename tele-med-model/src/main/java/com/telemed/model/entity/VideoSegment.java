package com.telemed.model.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "t_video_segment", uniqueConstraints = {
    @UniqueConstraint(columnNames = {"recordingId", "segmentIndex"})
})
public class VideoSegment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long recordingId;

    private Long consultationId;

    private Integer segmentIndex;

    @Column(nullable = false, length = 256)
    private String fileName;

    @Column(nullable = false, length = 128)
    private String bucketName;

    @Column(nullable = false, length = 512)
    private String objectName;

    private Long fileSize;

    private Integer duration;

    @Column(length = 128)
    private String encryptionIv;

    @Column(length = 128)
    private String checksum;

    private Integer uploadStatus;

    private LocalDateTime uploadTime;

    private LocalDateTime createTime;

    @PrePersist
    protected void onCreate() {
        createTime = LocalDateTime.now();
    }
}
