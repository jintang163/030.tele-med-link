package com.telemed.service.impl;

import com.telemed.common.constant.VideoConstants;
import com.telemed.common.constant.VideoRecordingStatus;
import com.telemed.common.exception.BusinessException;
import com.telemed.common.util.AesEncryptUtil;
import com.telemed.model.entity.VideoRecording;
import com.telemed.model.entity.VideoSegment;
import com.telemed.model.repository.VideoRecordingRepository;
import com.telemed.model.repository.VideoSegmentRepository;
import com.telemed.service.MinioService;
import com.telemed.service.VideoTranscodeService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class VideoTranscodeServiceImpl implements VideoTranscodeService {

    private final VideoRecordingRepository videoRecordingRepository;
    private final VideoSegmentRepository videoSegmentRepository;
    private final MinioService minioService;
    private final AesEncryptUtil aesEncryptUtil;

    @Value("${video.temp-dir:/tmp/telemed-video}")
    private String tempDir;

    @Value("${video.ffmpeg-path:ffmpeg}")
    private String ffmpegPath;

    @Value("${video.hls-segment-duration:10}")
    private int hlsSegmentDuration;

    @Override
    @Transactional
    public void mergeAndTranscodeToHls(Long recordingId) {
        processRecordingTranscode(recordingId);
    }

    @Override
    @Transactional
    public void processRecordingTranscode(Long recordingId) {
        log.info("开始处理视频转码，recordingId: {}", recordingId);

        VideoRecording recording = videoRecordingRepository.findById(recordingId)
                .orElseThrow(() -> new BusinessException("录制不存在"));

        if (recording.getStatus() != VideoRecordingStatus.PROCESSING.getCode()
                && recording.getStatus() != VideoRecordingStatus.UPLOADING.getCode()) {
            log.warn("录制状态不适合转码，recordingId: {}, status: {}", recordingId, recording.getStatus());
            return;
        }

        List<VideoSegment> segments = videoSegmentRepository.findByRecordingIdOrderBySegmentIndexAsc(recordingId);
        if (segments.isEmpty()) {
            log.error("没有找到视频片段，recordingId: {}", recordingId);
            recording.setStatus(VideoRecordingStatus.FAILED.getCode());
            videoRecordingRepository.save(recording);
            return;
        }

        Path workDir = null;
        try {
            workDir = prepareWorkDirectory(recordingId);
            List<Path> decryptedFiles = downloadAndDecryptSegments(segments, recording, workDir);
            Path mergedFile = mergeWebmFiles(decryptedFiles, workDir, recordingId);
            Path hlsDir = transcodeToHls(mergedFile, workDir, recordingId);
            uploadHlsToMinio(hlsDir, recording);

            recording.setStatus(VideoRecordingStatus.COMPLETED.getCode());
            videoRecordingRepository.save(recording);
            log.info("视频转码完成，recordingId: {}", recordingId);
        } catch (Exception e) {
            log.error("视频转码失败，recordingId: {}", recordingId, e);
            recording.setStatus(VideoRecordingStatus.FAILED.getCode());
            videoRecordingRepository.save(recording);
        } finally {
            if (workDir != null) {
                cleanupWorkDirectory(workDir);
            }
        }
    }

    private Path prepareWorkDirectory(Long recordingId) throws IOException {
        Path workDir = Paths.get(tempDir, String.valueOf(recordingId));
        Files.createDirectories(workDir);
        Path segmentsDir = workDir.resolve("segments");
        Files.createDirectories(segmentsDir);
        Path hlsDir = workDir.resolve("hls");
        Files.createDirectories(hlsDir);
        log.info("工作目录已准备: {}", workDir);
        return workDir;
    }

    private List<Path> downloadAndDecryptSegments(List<VideoSegment> segments, VideoRecording recording, Path workDir) throws IOException {
        List<Path> decryptedFiles = new ArrayList<>();
        byte[] keyBytes = Base64.getDecoder().decode(recording.getEncryptionKey());

        for (VideoSegment segment : segments) {
            log.info("下载片段: recordingId={}, segmentIndex={}, objectName={}",
                    segment.getRecordingId(), segment.getSegmentIndex(), segment.getObjectName());

            byte[] encryptedData = minioService.downloadFile(segment.getBucketName(), segment.getObjectName());

            byte[] ivBytes = segment.getEncryptionIv() != null
                    ? Base64.getDecoder().decode(segment.getEncryptionIv())
                    : new byte[16];

            byte[] decryptedData = aesEncryptUtil.decryptBytes(encryptedData, ivBytes);

            Path segmentFile = workDir.resolve("segments").resolve(
                    String.format("segment_%03d.webm", segment.getSegmentIndex()));
            Files.write(segmentFile, decryptedData);
            decryptedFiles.add(segmentFile);

            log.info("片段已解密: {}, 大小: {} bytes", segmentFile.getFileName(), decryptedData.length);
        }

        return decryptedFiles;
    }

    private Path mergeWebmFiles(List<Path> decryptedFiles, Path workDir, Long recordingId) throws IOException {
        Path concatListFile = workDir.resolve("concat_list.txt");
        StringBuilder concatContent = new StringBuilder();
        for (Path file : decryptedFiles) {
            concatContent.append("file '").append(file.toAbsolutePath()).append("'\n");
        }
        Files.writeString(concatListFile, concatContent.toString());

        Path mergedFile = workDir.resolve("merged.webm");

        ProcessBuilder pb = new ProcessBuilder(
                ffmpegPath,
                "-f", "concat",
                "-safe", "0",
                "-i", concatListFile.toAbsolutePath().toString(),
                "-c", "copy",
                "-y",
                mergedFile.toAbsolutePath().toString()
        );
        pb.redirectErrorStream(true);

        log.info("执行FFmpeg合并: recordingId={}", recordingId);
        Process process = pb.start();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
            String line;
            while ((line = reader.readLine()) != null) {
                log.debug("FFmpeg: {}", line);
            }
        }

        try {
            int exitCode = process.waitFor();
            if (exitCode != 0) {
                throw new IOException("FFmpeg合并失败，退出码: " + exitCode);
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IOException("FFmpeg合并被中断", e);
        }

        log.info("WebM合并完成: {}, 大小: {} bytes", mergedFile, Files.size(mergedFile));
        return mergedFile;
    }

    private Path transcodeToHls(Path mergedFile, Path workDir, Long recordingId) throws IOException {
        Path hlsDir = workDir.resolve("hls");
        String hlsPlaylistName = "playlist.m3u8";

        ProcessBuilder pb = new ProcessBuilder(
                ffmpegPath,
                "-i", mergedFile.toAbsolutePath().toString(),
                "-c:v", "libx264",
                "-c:a", "aac",
                "-preset", "fast",
                "-crf", "23",
                "-hls_time", String.valueOf(hlsSegmentDuration),
                "-hls_list_size", "0",
                "-hls_segment_filename", hlsDir.resolve("segment_%03d.ts").toAbsolutePath().toString(),
                "-hls_flags", "independent_segments",
                "-f", "hls",
                "-y",
                hlsDir.resolve(hlsPlaylistName).toAbsolutePath().toString()
        );
        pb.redirectErrorStream(true);

        log.info("执行FFmpeg HLS转码: recordingId={}", recordingId);
        Process process = pb.start();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
            String line;
            while ((line = reader.readLine()) != null) {
                log.debug("FFmpeg: {}", line);
            }
        }

        try {
            int exitCode = process.waitFor();
            if (exitCode != 0) {
                throw new IOException("FFmpeg HLS转码失败，退出码: " + exitCode);
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IOException("FFmpeg转码被中断", e);
        }

        log.info("HLS转码完成: {}", hlsDir);
        return hlsDir;
    }

    private void uploadHlsToMinio(Path hlsDir, VideoRecording recording) throws IOException {
        minioService.createBucketIfNotExists(VideoConstants.HLS_BUCKET);

        String baseObjectPath = String.format("%d/%d", recording.getConsultationId(), recording.getId());

        File[] hlsFiles = hlsDir.toFile().listFiles();
        if (hlsFiles == null || hlsFiles.length == 0) {
            throw new IOException("HLS目录为空，转码可能失败");
        }

        String playlistObjectName = null;

        for (File file : hlsFiles) {
            String objectName = baseObjectPath + "/" + file.getName();
            byte[] fileBytes = Files.readAllBytes(file.toPath());
            String contentType = file.getName().endsWith(".m3u8")
                    ? VideoConstants.HLS_MIME_TYPE
                    : VideoConstants.TS_MIME_TYPE;

            minioService.uploadBytes(VideoConstants.HLS_BUCKET, objectName, fileBytes, contentType);
            log.info("HLS文件已上传: {}/{}", VideoConstants.HLS_BUCKET, objectName);

            if (file.getName().endsWith(".m3u8")) {
                playlistObjectName = objectName;
            }
        }

        if (playlistObjectName != null) {
            String playlistUrl = minioService.getFileUrl(VideoConstants.HLS_BUCKET, playlistObjectName);
            recording.setHlsPlaylistUrl(playlistUrl);
            recording.setHlsBucket(VideoConstants.HLS_BUCKET);
            recording.setHlsObjectName(playlistObjectName);
            videoRecordingRepository.save(recording);

            log.info("HLS播放列表URL: {}", playlistUrl);
        }
    }

    private void cleanupWorkDirectory(Path workDir) {
        try {
            if (Files.exists(workDir)) {
                deleteRecursively(workDir.toFile());
                log.info("工作目录已清理: {}", workDir);
            }
        } catch (Exception e) {
            log.warn("清理工作目录失败: {}", workDir, e);
        }
    }

    private void deleteRecursively(File file) {
        if (file.isDirectory()) {
            File[] children = file.listFiles();
            if (children != null) {
                for (File child : children) {
                    deleteRecursively(child);
                }
            }
        }
        if (!file.delete()) {
            log.warn("无法删除文件: {}", file.getAbsolutePath());
        }
    }
}
