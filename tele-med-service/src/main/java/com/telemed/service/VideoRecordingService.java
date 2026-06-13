package com.telemed.service;

import com.telemed.common.dto.video.VideoPlaybackAuthDTO;
import com.telemed.common.dto.video.VideoRecordingAuthDTO;
import com.telemed.common.dto.video.VideoRecordingStartDTO;
import com.telemed.common.dto.video.VideoSegmentUploadDTO;
import com.telemed.common.vo.video.VideoPlaybackAuthVO;
import com.telemed.common.vo.video.VideoRecordingKeyVO;
import com.telemed.common.vo.video.VideoRecordingVO;
import com.telemed.common.vo.video.VideoSegmentVO;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

public interface VideoRecordingService {

    VideoRecordingVO startRecording(VideoRecordingStartDTO dto);

    VideoRecordingVO authorizeRecording(VideoRecordingAuthDTO dto);

    VideoSegmentVO uploadSegment(VideoSegmentUploadDTO dto, MultipartFile file, String encryptionKey, String encryptionIv);

    VideoRecordingVO stopRecording(Long consultationId);

    VideoRecordingKeyVO generateEncryptionKey(Long consultationId);

    List<VideoRecordingVO> getDoctorRecordings(Long doctorId, Integer status);

    List<VideoRecordingVO> getPatientRecordings(Long patientId);

    VideoRecordingVO getRecordingDetail(Long id);

    List<VideoSegmentVO> getRecordingSegments(Long recordingId);

    VideoPlaybackAuthVO generatePlaybackAuth(VideoPlaybackAuthDTO dto);

    VideoPlaybackAuthVO validatePlaybackToken(String token);

    void incrementPlayCount(String token);

    void triggerTranscode(Long recordingId);

    void cleanupExpiredRecordings();
}
