package com.telemed.web.job;

import com.telemed.service.VideoRecordingService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.quartz.DisallowConcurrentExecution;
import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.quartz.PersistJobDataAfterExecution;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@DisallowConcurrentExecution
@PersistJobDataAfterExecution
@RequiredArgsConstructor
public class VideoRecordingCleanupJob implements Job {

    private final VideoRecordingService videoRecordingService;

    @Override
    public void execute(JobExecutionContext context) throws JobExecutionException {
        log.info("开始清理过期视频录制...");
        try {
            videoRecordingService.cleanupExpiredRecordings();
            log.info("过期视频录制清理完成");
        } catch (Exception e) {
            log.error("清理过期视频录制失败", e);
            throw new JobExecutionException(e);
        }
    }
}
