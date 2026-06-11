package com.telemed.web.job;

import com.telemed.service.CrossCampusConsultationService;
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
public class CrossCampusConsultationCleanJob implements Job {

    private final CrossCampusConsultationService crossCampusConsultationService;

    @Override
    public void execute(JobExecutionContext context) throws JobExecutionException {
        log.info("开始执行跨院区会诊超时清理任务");
        try {
            crossCampusConsultationService.cleanExpiredConsultations();
            log.info("跨院区会诊超时清理任务执行完成");
        } catch (Exception e) {
            log.error("跨院区会诊超时清理任务执行失败", e);
            throw new JobExecutionException(e);
        }
    }
}
