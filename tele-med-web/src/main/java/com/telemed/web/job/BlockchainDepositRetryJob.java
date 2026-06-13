package com.telemed.web.job;

import com.telemed.service.BlockchainDepositService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class BlockchainDepositRetryJob {

    private final BlockchainDepositService blockchainDepositService;

    @Scheduled(cron = "${blockchain.retry-cron:0 */30 * * * ?}")
    public void retryFailedDeposits() {
        log.info("定时任务：开始重试失败的区块链存证");
        try {
            blockchainDepositService.retryFailedDeposits();
            log.info("定时任务：区块链存证重试完成");
        } catch (Exception e) {
            log.error("定时任务：区块链存证重试异常", e);
        }
    }
}
