package com.telemed.web.job;

import com.telemed.common.constant.ConsultationStatus;
import com.telemed.model.entity.Consultation;
import com.telemed.model.entity.AsrQualityReport;
import com.telemed.model.repository.ConsultationRepository;
import com.telemed.model.repository.AsrQualityReportRepository;
import com.telemed.service.AsrService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.quartz.DisallowConcurrentExecution;
import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.quartz.PersistJobDataAfterExecution;
import org.springframework.stereotype.Component;

import java.util.List;

@Slf4j
@Component
@DisallowConcurrentExecution
@PersistJobDataAfterExecution
@RequiredArgsConstructor
public class AsrQualityProcessJob implements Job {

    private final ConsultationRepository consultationRepository;
    private final AsrQualityReportRepository qualityReportRepository;
    private final AsrService asrService;

    @Override
    public void execute(JobExecutionContext context) throws JobExecutionException {
        log.info("开始会诊ASR+质检扫描任务...");
        try {
            List<Consultation> finished = consultationRepository.findByStatus(
                    ConsultationStatus.FINISHED.getCode());

            int processed = 0;
            int skipped = 0;
            int failed = 0;

            for (Consultation c : finished) {
                try {
                    boolean hasReport = qualityReportRepository.findByConsultationId(c.getId()).isPresent();
                    if (hasReport) {
                        skipped++;
                        continue;
                    }
                    asrService.processConsultationAsrAndQuality(c.getId());
                    processed++;
                } catch (Exception e) {
                    failed++;
                    log.error("处理会诊ASR质检失败，consultationId: {}", c.getId(), e);
                }
            }

            log.info("ASR+质检扫描完成，处理: {}, 跳过(已有报告): {}, 失败: {}",
                    processed, skipped, failed);
        } catch (Exception e) {
            log.error("ASR+质检任务执行异常", e);
            throw new JobExecutionException(e);
        }
    }
}
