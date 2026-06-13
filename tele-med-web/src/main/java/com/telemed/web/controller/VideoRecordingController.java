package com.telemed.web.controller;

import com.telemed.common.constant.VideoConstants;
import com.telemed.common.dto.video.VideoPlaybackAuthDTO;
import com.telemed.common.dto.video.VideoRecordingAuthDTO;
import com.telemed.common.dto.video.VideoRecordingStartDTO;
import com.telemed.common.dto.video.VideoSegmentUploadDTO;
import com.telemed.common.result.Result;
import com.telemed.common.vo.video.VideoPlaybackAuthVO;
import com.telemed.common.vo.video.VideoRecordingKeyVO;
import com.telemed.common.vo.video.VideoRecordingVO;
import com.telemed.common.vo.video.VideoSegmentVO;
import com.telemed.service.VideoRecordingService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/api/video")
@RequiredArgsConstructor
public class VideoRecordingController {

    private final VideoRecordingService videoRecordingService;

    @PostMapping("/recording/start")
    public Result<VideoRecordingVO> startRecording(@RequestBody VideoRecordingStartDTO dto) {
        VideoRecordingVO vo = videoRecordingService.startRecording(dto);
        return Result.ok(vo);
    }

    @PostMapping("/recording/authorize")
    public Result<VideoRecordingVO> authorizeRecording(@RequestBody VideoRecordingAuthDTO dto) {
        VideoRecordingVO vo = videoRecordingService.authorizeRecording(dto);
        return Result.ok(vo);
    }

    @PostMapping(value = "/recording/segment", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public Result<VideoSegmentVO> uploadSegment(
            @RequestPart("file") MultipartFile file,
            @RequestParam Long recordingId,
            @RequestParam Long consultationId,
            @RequestParam Integer segmentIndex,
            @RequestParam String fileName,
            @RequestParam(required = false) Integer duration,
            @RequestParam String encryptionIv,
            @RequestParam(required = false) String checksum
    ) {
        VideoSegmentUploadDTO dto = new VideoSegmentUploadDTO();
        dto.setRecordingId(recordingId);
        dto.setConsultationId(consultationId);
        dto.setSegmentIndex(segmentIndex);
        dto.setFileName(fileName);
        dto.setDuration(duration);
        dto.setEncryptionIv(encryptionIv);
        dto.setChecksum(checksum);

        VideoSegmentVO vo = videoRecordingService.uploadSegment(dto, file, null, null);
        return Result.ok(vo);
    }

    @PostMapping("/recording/stop")
    public Result<VideoRecordingVO> stopRecording(@RequestParam Long consultationId) {
        VideoRecordingVO vo = videoRecordingService.stopRecording(consultationId);
        return Result.ok(vo);
    }

    @GetMapping("/recording/encryption-key")
    public Result<VideoRecordingKeyVO> getEncryptionKey(@RequestParam Long consultationId) {
        VideoRecordingKeyVO vo = videoRecordingService.generateEncryptionKey(consultationId);
        return Result.ok(vo);
    }

    @GetMapping("/recording/doctor/{doctorId}")
    public Result<List<VideoRecordingVO>> getDoctorRecordings(
            @PathVariable Long doctorId,
            @RequestParam(required = false) Integer status) {
        List<VideoRecordingVO> list = videoRecordingService.getDoctorRecordings(doctorId, status);
        return Result.ok(list);
    }

    @GetMapping("/recording/patient/{patientId}")
    public Result<List<VideoRecordingVO>> getPatientRecordings(@PathVariable Long patientId) {
        List<VideoRecordingVO> list = videoRecordingService.getPatientRecordings(patientId);
        return Result.ok(list);
    }

    @GetMapping("/recording/{id}")
    public Result<VideoRecordingVO> getRecordingDetail(@PathVariable Long id) {
        VideoRecordingVO vo = videoRecordingService.getRecordingDetail(id);
        return Result.ok(vo);
    }

    @GetMapping("/recording/{recordingId}/segments")
    public Result<List<VideoSegmentVO>> getRecordingSegments(@PathVariable Long recordingId) {
        List<VideoSegmentVO> list = videoRecordingService.getRecordingSegments(recordingId);
        return Result.ok(list);
    }

    @PostMapping("/playback/auth")
    public Result<VideoPlaybackAuthVO> generatePlaybackAuth(@RequestBody VideoPlaybackAuthDTO dto) {
        VideoPlaybackAuthVO vo = videoRecordingService.generatePlaybackAuth(dto);
        return Result.ok(vo);
    }

    @GetMapping("/playback/validate")
    public Result<VideoPlaybackAuthVO> validatePlaybackToken(
            @RequestHeader(VideoConstants.AUTH_TOKEN_HEADER) String token) {
        VideoPlaybackAuthVO vo = videoRecordingService.validatePlaybackToken(token);
        return Result.ok(vo);
    }

    @PostMapping("/playback/increment")
    public Result<Void> incrementPlayCount(
            @RequestHeader(VideoConstants.AUTH_TOKEN_HEADER) String token) {
        videoRecordingService.incrementPlayCount(token);
        return Result.ok();
    }

    @PostMapping("/recording/{recordingId}/transcode")
    public Result<Void> triggerTranscode(@PathVariable Long recordingId) {
        videoRecordingService.triggerTranscode(recordingId);
        return Result.ok();
    }
}
