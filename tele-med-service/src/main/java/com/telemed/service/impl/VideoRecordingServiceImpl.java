package com.telemed.service.impl;

import cn.hutool.core.util.IdUtil;
import com.telemed.common.constant.ConsultationStatus;
import com.telemed.common.constant.VideoConstants;
import com.telemed.common.constant.VideoRecordingStatus;
import com.telemed.common.constant.VideoUploadStatus;
import com.telemed.common.dto.video.VideoPlaybackAuthDTO;
import com.telemed.common.dto.video.VideoRecordingAuthDTO;
import com.telemed.common.dto.video.VideoRecordingStartDTO;
import com.telemed.common.dto.video.VideoSegmentUploadDTO;
import com.telemed.common.exception.BusinessException;
import com.telemed.common.util.AesEncryptUtil;
import com.telemed.common.vo.video.VideoPlaybackAuthVO;
import com.telemed.common.vo.video.VideoRecordingKeyVO;
import com.telemed.common.vo.video.VideoRecordingVO;
import com.telemed.common.vo.video.VideoSegmentVO;
import com.telemed.model.entity.Consultation;
import com.telemed.model.entity.Doctor;
import com.telemed.model.entity.Patient;
import com.telemed.model.entity.VideoPlaybackAuth;
import com.telemed.model.entity.VideoRecording;
import com.telemed.model.entity.VideoSegment;
import com.telemed.model.repository.ConsultationRepository;
import com.telemed.model.repository.DoctorRepository;
import com.telemed.model.repository.PatientRepository;
import com.telemed.model.repository.VideoPlaybackAuthRepository;
import com.telemed.model.repository.VideoRecordingRepository;
import com.telemed.model.repository.VideoSegmentRepository;
import com.telemed.service.MinioService;
import com.telemed.service.VideoRecordingService;
import com.telemed.service.VideoTranscodeService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class VideoRecordingServiceImpl implements VideoRecordingService {

    private final VideoRecordingRepository videoRecordingRepository;
    private final VideoSegmentRepository videoSegmentRepository;
    private final VideoPlaybackAuthRepository videoPlaybackAuthRepository;
    private final ConsultationRepository consultationRepository;
    private final DoctorRepository doctorRepository;
    private final PatientRepository patientRepository;
    private final MinioService minioService;
    private final AesEncryptUtil aesEncryptUtil;
    private final VideoTranscodeService videoTranscodeService;
    private final RedisTemplate<String, Object> redisTemplate;

    @Value("${video.retention-days:90}")
    private int retentionDays;

    @Override
    @Transactional
    public VideoRecordingVO startRecording(VideoRecordingStartDTO dto) {
        Consultation consultation = consultationRepository.findById(dto.getConsultationId())
                .orElseThrow(() -> new BusinessException("会诊不存在"));

        if (consultation.getStatus() != ConsultationStatus.ONGOING.getCode()) {
            throw new BusinessException("会诊未进行中，无法录制");
        }

        VideoRecording existing = videoRecordingRepository.findByConsultationId(dto.getConsultationId()).orElse(null);
        if (existing != null && existing.getStatus() == VideoRecordingStatus.RECORDING.getCode()) {
            throw new BusinessException("该会诊正在录制中");
        }

        VideoRecording recording = new VideoRecording();
        recording.setConsultationId(dto.getConsultationId());
        recording.setDoctorId(dto.getDoctorId());
        recording.setPatientId(consultation.getPatientId());
        recording.setStatus(VideoRecordingStatus.PENDING_AUTHORIZATION.getCode());
        recording.setSegmentDuration(dto.getSegmentDuration() != null ? dto.getSegmentDuration() : VideoConstants.DEFAULT_SEGMENT_DURATION);
        recording.setWatermarkText(dto.getWatermarkText() != null ? dto.getWatermarkText() :
                String.format(VideoConstants.WATERMARK_TEXT, consultation.getConsultationNo()));
        recording.setEncryptionKey(aesEncryptUtil.generateRandomKey());
        recording.setEncryptionIv(aesEncryptUtil.generateRandomIv());
        recording.setExpireTime(LocalDateTime.now().plusDays(retentionDays));
        recording.setStartTime(LocalDateTime.now());
        recording.setDoctorAuthorized(true);
        recording.setPatientAuthorized(false);

        VideoRecording saved = videoRecordingRepository.save(recording);
        log.info("视频录制已创建，等待授权，recordingId: {}", saved.getId());

        return convertToVO(saved);
    }

    @Override
    @Transactional
    public VideoRecordingVO authorizeRecording(VideoRecordingAuthDTO dto) {
        VideoRecording recording = videoRecordingRepository.findByConsultationId(dto.getConsultationId())
                .orElseThrow(() -> new BusinessException("录制不存在"));

        if (!dto.getAuthorized()) {
            recording.setStatus(VideoRecordingStatus.CANCELLED.getCode());
            recording.setEndTime(LocalDateTime.now());
            videoRecordingRepository.save(recording);
            log.info("用户拒绝录制授权，consultationId: {}", dto.getConsultationId());
            return convertToVO(recording);
        }

        if ("DOCTOR".equals(dto.getUserRole())) {
            recording.setDoctorAuthorized(true);
        } else if ("PATIENT".equals(dto.getUserRole())) {
            recording.setPatientAuthorized(true);
        }

        if (recording.getDoctorAuthorized() && recording.getPatientAuthorized()) {
            recording.setStatus(VideoRecordingStatus.RECORDING.getCode());
            recording.setStartTime(LocalDateTime.now());
            log.info("双方已授权，开始录制，recordingId: {}", recording.getId());
        }

        VideoRecording saved = videoRecordingRepository.save(recording);
        return convertToVO(saved);
    }

    @Override
    @Transactional
    public VideoSegmentVO uploadSegment(VideoSegmentUploadDTO dto, MultipartFile file, String encryptionKey, String encryptionIv) {
        VideoRecording recording = videoRecordingRepository.findById(dto.getRecordingId())
                .orElseThrow(() -> new BusinessException("录制不存在"));

        if (recording.getStatus() != VideoRecordingStatus.RECORDING.getCode()
                && recording.getStatus() != VideoRecordingStatus.UPLOADING.getCode()) {
            throw new BusinessException("录制状态不允许上传");
        }

        if (recording.getStatus() == VideoRecordingStatus.RECORDING.getCode()) {
            recording.setStatus(VideoRecordingStatus.UPLOADING.getCode());
            videoRecordingRepository.save(recording);
        }

        String objectName = buildSegmentObjectName(dto.getRecordingId(), dto.getConsultationId(), dto.getSegmentIndex(), dto.getFileName());

        byte[] encryptedBytes;
        try {
            byte[] fileBytes = file.getBytes();
            byte[] ivBytes = Base64.getDecoder().decode(dto.getEncryptionIv());
            encryptedBytes = aesEncryptUtil.encryptBytes(fileBytes, ivBytes);
        } catch (Exception e) {
            log.error("视频片段加密失败", e);
            throw new BusinessException("视频片段加密失败");
        }

        String storedName = minioService.uploadBytes(VideoConstants.SEGMENT_BUCKET, objectName, encryptedBytes, "application/octet-stream");

        VideoSegment segment = new VideoSegment();
        segment.setRecordingId(dto.getRecordingId());
        segment.setConsultationId(dto.getConsultationId());
        segment.setSegmentIndex(dto.getSegmentIndex());
        segment.setFileName(dto.getFileName());
        segment.setBucketName(VideoConstants.SEGMENT_BUCKET);
        segment.setObjectName(storedName);
        segment.setFileSize(file.getSize());
        segment.setDuration(dto.getDuration());
        segment.setEncryptionIv(dto.getEncryptionIv());
        segment.setChecksum(dto.getChecksum());
        segment.setUploadStatus(VideoUploadStatus.SUCCESS.getCode());
        segment.setUploadTime(LocalDateTime.now());

        VideoSegment saved = videoSegmentRepository.save(segment);
        log.info("视频片段上传成功，segmentId: {}, index: {}", saved.getId(), dto.getSegmentIndex());

        return convertSegmentToVO(saved);
    }

    @Override
    @Transactional
    public VideoRecordingVO stopRecording(Long consultationId) {
        VideoRecording recording = videoRecordingRepository.findByConsultationId(consultationId)
                .orElseThrow(() -> new BusinessException("录制不存在"));

        if (recording.getStatus() != VideoRecordingStatus.RECORDING.getCode()
                && recording.getStatus() != VideoRecordingStatus.UPLOADING.getCode()) {
            throw new BusinessException("录制状态不允许停止");
        }

        recording.setStatus(VideoRecordingStatus.PROCESSING.getCode());
        recording.setEndTime(LocalDateTime.now());

        List<VideoSegment> segments = videoSegmentRepository.findByRecordingIdOrderBySegmentIndexAsc(recording.getId());
        recording.setTotalSegments(segments.size());
        int totalDuration = segments.stream()
                .mapToInt(s -> s.getDuration() != null ? s.getDuration() : 0)
                .sum();
        recording.setTotalDuration(totalDuration);

        VideoRecording saved = videoRecordingRepository.save(recording);
        log.info("录制已停止，开始处理合并转码，recordingId: {}, 片段数: {}, 总时长: {}s",
                saved.getId(), segments.size(), totalDuration);

        triggerTranscodeAsync(recording.getId());

        return convertToVO(saved);
    }

    @Async
    public void triggerTranscodeAsync(Long recordingId) {
        try {
            videoTranscodeService.processRecordingTranscode(recordingId);
        } catch (Exception e) {
            log.error("异步转码失败，recordingId: {}", recordingId, e);
            VideoRecording recording = videoRecordingRepository.findById(recordingId).orElse(null);
            if (recording != null) {
                recording.setStatus(VideoRecordingStatus.FAILED.getCode());
                videoRecordingRepository.save(recording);
            }
        }
    }

    @Override
    public VideoRecordingKeyVO generateEncryptionKey(Long consultationId) {
        VideoRecording recording = videoRecordingRepository.findByConsultationId(consultationId)
                .orElseThrow(() -> new BusinessException("录制不存在"));

        if (recording.getStatus() != VideoRecordingStatus.RECORDING.getCode()
                && recording.getStatus() != VideoRecordingStatus.PENDING_AUTHORIZATION.getCode()
                && recording.getStatus() != VideoRecordingStatus.UPLOADING.getCode()) {
            throw new BusinessException("录制状态不允许获取密钥");
        }

        return new VideoRecordingKeyVO(
                recording.getId(),
                recording.getEncryptionKey(),
                recording.getEncryptionIv()
        );
    }

    @Override
    public List<VideoRecordingVO> getDoctorRecordings(Long doctorId, Integer status) {
        List<VideoRecording> recordings;
        if (status != null) {
            recordings = videoRecordingRepository.findByDoctorIdAndStatus(doctorId, status);
        } else {
            recordings = videoRecordingRepository.findAll().stream()
                    .filter(r -> doctorId.equals(r.getDoctorId()))
                    .collect(Collectors.toList());
        }
        return recordings.stream()
                .map(this::convertToVO)
                .collect(Collectors.toList());
    }

    @Override
    public List<VideoRecordingVO> getPatientRecordings(Long patientId) {
        List<VideoRecording> recordings = videoRecordingRepository.findByPatientId(patientId);
        return recordings.stream()
                .map(this::convertToVO)
                .collect(Collectors.toList());
    }

    @Override
    public VideoRecordingVO getRecordingDetail(Long id) {
        VideoRecording recording = videoRecordingRepository.findById(id)
                .orElseThrow(() -> new BusinessException("录制不存在"));
        return convertToVO(recording);
    }

    @Override
    public List<VideoSegmentVO> getRecordingSegments(Long recordingId) {
        List<VideoSegment> segments = videoSegmentRepository.findByRecordingIdOrderBySegmentIndexAsc(recordingId);
        return segments.stream()
                .map(this::convertSegmentToVO)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public VideoPlaybackAuthVO generatePlaybackAuth(VideoPlaybackAuthDTO dto) {
        VideoRecording recording = videoRecordingRepository.findById(dto.getRecordingId())
                .orElseThrow(() -> new BusinessException("录制不存在"));

        if (recording.getStatus() != VideoRecordingStatus.COMPLETED.getCode()) {
            throw new BusinessException("视频未完成处理，无法播放");
        }

        if (recording.getExpireTime() != null && recording.getExpireTime().isBefore(LocalDateTime.now())) {
            throw new BusinessException("视频已过期");
        }

        boolean hasPermission = checkPlaybackPermission(dto.getRecordingId(), dto.getUserId(), dto.getUserRole(),
                recording);
        if (!hasPermission) {
            throw new BusinessException("无播放权限");
        }

        int expireMinutes = dto.getExpireMinutes() != null ? dto.getExpireMinutes() : 60;
        String token = IdUtil.fastSimpleUUID() + IdUtil.fastSimpleUUID();

        VideoPlaybackAuth auth = new VideoPlaybackAuth();
        auth.setRecordingId(dto.getRecordingId());
        auth.setUserId(dto.getUserId());
        auth.setUserRole(dto.getUserRole());
        auth.setAuthToken(token);
        auth.setExpireTime(LocalDateTime.now().plusMinutes(expireMinutes));
        auth.setPlayCount(0);
        videoPlaybackAuthRepository.save(auth);

        String cacheKey = "video:playback:auth:" + token;
        redisTemplate.opsForValue().set(cacheKey, recording.getId(), expireMinutes, TimeUnit.MINUTES);

        VideoPlaybackAuthVO vo = new VideoPlaybackAuthVO();
        vo.setRecordingId(recording.getId());
        vo.setAuthToken(token);
        vo.setHlsPlaylistUrl(recording.getHlsPlaylistUrl());
        vo.setEncryptionKey(recording.getEncryptionKey());
        vo.setExpireTime(auth.getExpireTime());

        log.info("生成播放授权，recordingId: {}, userId: {}, token: {}", dto.getRecordingId(), dto.getUserId(), token);
        return vo;
    }

    private boolean checkPlaybackPermission(Long recordingId, Long userId, String userRole, VideoRecording recording) {
        if ("DOCTOR".equals(userRole)) {
            return userId.equals(recording.getDoctorId());
        } else if ("PATIENT".equals(userRole)) {
            return userId.equals(recording.getPatientId());
        }
        return false;
    }

    @Override
    public VideoPlaybackAuthVO validatePlaybackToken(String token) {
        if (token == null || token.isEmpty()) {
            throw new BusinessException("播放令牌为空");
        }

        String cacheKey = "video:playback:auth:" + token;
        Object cachedId = redisTemplate.opsForValue().get(cacheKey);
        Long recordingId;
        VideoPlaybackAuth auth;

        if (cachedId != null) {
            recordingId = Long.valueOf(cachedId.toString());
            auth = videoPlaybackAuthRepository.findByAuthToken(token).orElse(null);
        } else {
            auth = videoPlaybackAuthRepository.findByAuthToken(token).orElse(null);
            if (auth == null) {
                throw new BusinessException("无效的播放令牌");
            }
            recordingId = auth.getRecordingId();
        }

        if (auth == null || auth.getExpireTime().isBefore(LocalDateTime.now())) {
            throw new BusinessException("播放令牌已过期");
        }

        VideoRecording recording = videoRecordingRepository.findById(recordingId)
                .orElseThrow(() -> new BusinessException("录制不存在"));

        if (recording.getExpireTime() != null && recording.getExpireTime().isBefore(LocalDateTime.now())) {
            throw new BusinessException("视频已过期");
        }

        VideoPlaybackAuthVO vo = new VideoPlaybackAuthVO();
        vo.setRecordingId(recording.getId());
        vo.setAuthToken(token);
        vo.setHlsPlaylistUrl(recording.getHlsPlaylistUrl());
        vo.setEncryptionKey(recording.getEncryptionKey());
        vo.setExpireTime(auth.getExpireTime());
        return vo;
    }

    @Override
    @Transactional
    public void incrementPlayCount(String token) {
        VideoPlaybackAuth auth = videoPlaybackAuthRepository.findByAuthToken(token).orElse(null);
        if (auth != null) {
            auth.setPlayCount(auth.getPlayCount() + 1);
            auth.setLastPlayTime(LocalDateTime.now());
            videoPlaybackAuthRepository.save(auth);
        }
    }

    @Override
    public void triggerTranscode(Long recordingId) {
        videoTranscodeService.processRecordingTranscode(recordingId);
    }

    @Override
    @Transactional
    public void cleanupExpiredRecordings() {
        LocalDateTime now = LocalDateTime.now();
        List<VideoRecording> expiredRecordings = videoRecordingRepository
                .findByStatusAndExpireTimeBefore(VideoRecordingStatus.COMPLETED.getCode(), now);

        log.info("清理过期录制，找到 {} 条过期记录", expiredRecordings.size());

        for (VideoRecording recording : expiredRecordings) {
            try {
                deleteRecordingFiles(recording);
                recording.setStatus(VideoRecordingStatus.EXPIRED.getCode());
                videoRecordingRepository.save(recording);
                log.info("录制已过期标记，recordingId: {}", recording.getId());
            } catch (Exception e) {
                log.error("清理过期录制失败，recordingId: {}", recording.getId(), e);
            }
        }

        List<VideoPlaybackAuth> expiredAuths = videoPlaybackAuthRepository.findByExpireTimeBefore(now);
        if (!expiredAuths.isEmpty()) {
            videoPlaybackAuthRepository.deleteAll(expiredAuths);
            log.info("清理过期播放授权 {} 条", expiredAuths.size());
        }
    }

    private void deleteRecordingFiles(VideoRecording recording) {
        List<VideoSegment> segments = videoSegmentRepository.findByRecordingIdOrderBySegmentIndexAsc(recording.getId());
        for (VideoSegment segment : segments) {
            try {
                minioService.deleteFile(segment.getBucketName(), segment.getObjectName());
            } catch (Exception e) {
                log.warn("删除片段文件失败，segmentId: {}, objectName: {}", segment.getId(), segment.getObjectName(), e);
            }
        }

        if (recording.getHlsBucket() != null && recording.getHlsObjectName() != null) {
            try {
                String baseName = recording.getHlsObjectName().replace(".m3u8", "");
                minioService.deleteFile(recording.getHlsBucket(), recording.getHlsObjectName());
                List<VideoSegment> allSegments = videoSegmentRepository.findByRecordingIdOrderBySegmentIndexAsc(recording.getId());
                for (int i = 0; i < allSegments.size() + 20; i++) {
                    try {
                        minioService.deleteFile(recording.getHlsBucket(), baseName + "_" + i + ".ts");
                    } catch (Exception ignore) {
                    }
                }
            } catch (Exception e) {
                log.warn("删除HLS文件失败，recordingId: {}", recording.getId(), e);
            }
        }
    }

    private String buildSegmentObjectName(Long recordingId, Long consultationId, Integer segmentIndex, String fileName) {
        String extension = "";
        int dotIndex = fileName.lastIndexOf('.');
        if (dotIndex > 0) {
            extension = fileName.substring(dotIndex);
        }
        return String.format("%d/%d/segment_%d%s", consultationId, recordingId, segmentIndex, extension);
    }

    private VideoRecordingVO convertToVO(VideoRecording recording) {
        VideoRecordingVO vo = new VideoRecordingVO();
        BeanUtils.copyProperties(recording, vo);
        vo.setStatusText(getStatusText(recording.getStatus()));

        Consultation consultation = consultationRepository.findById(recording.getConsultationId()).orElse(null);
        if (consultation != null) {
            vo.setConsultationNo(consultation.getConsultationNo());
            Doctor doctor = doctorRepository.findById(recording.getDoctorId()).orElse(null);
            if (doctor != null) {
                vo.setDoctorName(doctor.getName());
            }
            Patient patient = patientRepository.findById(recording.getPatientId()).orElse(null);
            if (patient != null) {
                vo.setPatientName(patient.getName());
            }
        }

        long uploadedCount = videoSegmentRepository.countByRecordingIdAndUploadStatus(
                recording.getId(), VideoUploadStatus.SUCCESS.getCode());
        vo.setUploadedSegments((int) uploadedCount);

        vo.setTotalSegments(recording.getTotalSegments() != null ? recording.getTotalSegments() : (int) uploadedCount);

        return vo;
    }

    private VideoSegmentVO convertSegmentToVO(VideoSegment segment) {
        VideoSegmentVO vo = new VideoSegmentVO();
        BeanUtils.copyProperties(segment, vo);
        vo.setUploadStatusText(getUploadStatusText(segment.getUploadStatus()));
        return vo;
    }

    private String getStatusText(Integer status) {
        if (status == null) return "未知";
        for (VideoRecordingStatus s : VideoRecordingStatus.values()) {
            if (s.getCode() == status) {
                switch (s) {
                    case PENDING_AUTHORIZATION: return "等待授权";
                    case RECORDING: return "录制中";
                    case UPLOADING: return "上传中";
                    case PROCESSING: return "处理中";
                    case COMPLETED: return "已完成";
                    case FAILED: return "失败";
                    case CANCELLED: return "已取消";
                    case EXPIRED: return "已过期";
                }
            }
        }
        return "未知";
    }

    private String getUploadStatusText(Integer status) {
        if (status == null) return "未知";
        for (VideoUploadStatus s : VideoUploadStatus.values()) {
            if (s.getCode() == status) {
                switch (s) {
                    case PENDING: return "待上传";
                    case UPLOADING: return "上传中";
                    case SUCCESS: return "上传成功";
                    case FAILED: return "上传失败";
                }
            }
        }
        return "未知";
    }
}
