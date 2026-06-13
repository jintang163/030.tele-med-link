package com.telemed.model.repository;

import com.telemed.model.entity.VideoPlaybackAuth;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface VideoPlaybackAuthRepository extends JpaRepository<VideoPlaybackAuth, Long> {

    Optional<VideoPlaybackAuth> findByAuthToken(String authToken);

    List<VideoPlaybackAuth> findByRecordingIdAndUserId(Long recordingId, Long userId);

    Optional<VideoPlaybackAuth> findByRecordingIdAndUserIdAndUserRole(Long recordingId, Long userId, String userRole);

    List<VideoPlaybackAuth> findByExpireTimeBefore(LocalDateTime expireTime);

    void deleteByRecordingId(Long recordingId);
}
