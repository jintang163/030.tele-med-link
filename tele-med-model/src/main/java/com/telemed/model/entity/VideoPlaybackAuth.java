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
@Table(name = "t_video_playback_auth")
public class VideoPlaybackAuth {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long recordingId;

    private Long userId;

    @Column(length = 20)
    private String userRole;

    @Column(nullable = false, length = 512)
    private String authToken;

    private LocalDateTime expireTime;

    private Integer playCount;

    private LocalDateTime lastPlayTime;

    private LocalDateTime createTime;

    @PrePersist
    protected void onCreate() {
        createTime = LocalDateTime.now();
        if (playCount == null) {
            playCount = 0;
        }
    }
}
