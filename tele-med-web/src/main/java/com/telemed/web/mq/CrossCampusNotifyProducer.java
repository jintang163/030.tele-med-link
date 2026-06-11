package com.telemed.web.mq;

import com.telemed.common.constant.CrossCampusConstants;
import com.telemed.common.dto.CrossCampusNotifyDTO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.spring.core.RocketMQTemplate;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class CrossCampusNotifyProducer {

    private final RocketMQTemplate rocketMQTemplate;

    public void sendCrossCampusNotify(CrossCampusNotifyDTO notifyDTO) {
        try {
            String destination = CrossCampusConstants.MQ_TOPIC_CROSS_CAMPUS_NOTIFY;
            rocketMQTemplate.send(destination,
                    MessageBuilder.withPayload(notifyDTO).build());
            log.info("发送跨院区会诊通知成功: consultationNo={}, targetCampusId={}",
                    notifyDTO.getConsultationNo(), notifyDTO.getTargetCampusId());
        } catch (Exception e) {
            log.error("发送跨院区会诊通知失败: consultationNo={}", notifyDTO.getConsultationNo(), e);
            throw new RuntimeException("发送跨院区会诊通知失败", e);
        }
    }

    public void sendAsyncCrossCampusNotify(CrossCampusNotifyDTO notifyDTO) {
        try {
            String destination = CrossCampusConstants.MQ_TOPIC_CROSS_CAMPUS_NOTIFY;
            rocketMQTemplate.asyncSend(destination,
                    MessageBuilder.withPayload(notifyDTO).build(),
                    new org.apache.rocketmq.spring.core.RocketMQLocalSendCallback() {
                        @Override
                        public void onSuccess(org.apache.rocketmq.client.producer.SendResult sendResult) {
                            log.info("异步发送跨院区会诊通知成功: consultationNo={}, msgId={}",
                                    notifyDTO.getConsultationNo(),
                                    sendResult != null ? sendResult.getMsgId() : null);
                        }

                        @Override
                        public void onException(Throwable e) {
                            log.error("异步发送跨院区会诊通知失败: consultationNo={}", notifyDTO.getConsultationNo(), e);
                        }
                    });
        } catch (Exception e) {
            log.error("异步发送跨院区会诊通知异常: consultationNo={}", notifyDTO.getConsultationNo(), e);
        }
    }
}
