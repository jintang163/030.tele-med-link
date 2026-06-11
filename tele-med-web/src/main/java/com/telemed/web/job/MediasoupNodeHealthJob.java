package com.telemed.web.job;

import com.telemed.service.MediasoupNodeService;
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
public class MediasoupNodeHealthJob implements Job {

    private final MediasoupNodeService mediasoupNodeService;

    @Override
    public void execute(JobExecutionContext context) throws JobExecutionException {
        log.info("开始执行 Mediasoup 节点健康检查任务");
        try {
            mediasoupNodeService.healthCheck();
            log.info("Mediasoup 节点健康检查任务执行完成");
        } catch (Exception e) {
            log.error("Mediasoup 节点健康检查任务执行失败", e);
            throw new JobExecutionException(e);
        }
    }
}
