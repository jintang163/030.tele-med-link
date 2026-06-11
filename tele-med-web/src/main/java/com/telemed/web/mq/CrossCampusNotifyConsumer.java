package com.telemed.web.mq;

import com.telemed.common.constant.CrossCampusConstants;
import com.telemed.common.dto.CrossCampusNotifyDTO;
import com.telemed.service.WechatNotifyService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.spring.annotation.RocketMQMessageListener;
import org.apache.rocketmq.spring.core.RocketMQListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
@RocketMQMessageListener(
        topic = CrossCampusConstants.MQ_TOPIC_CROSS_CAMPUS_NOTIFY,
        consumerGroup = "cross-campus-consumer-group"
)
public class CrossCampusNotifyConsumer implements RocketMQListener<CrossCampusNotifyDTO> {

    private final WechatNotifyService wechatNotifyService;

    @Override
    public void onMessage(CrossCampusNotifyDTO message) {
        log.info("收到跨院区会诊通知消息: consultationNo={}, primaryDoctorId={}",
                message.getConsultationNo(), message.getPrimaryDoctorId());

        try {
            notifyPrimaryDoctor(message);

            notifySourceCampus(message);

            log.info("跨院区会诊通知处理完成: consultationNo={}", message.getConsultationNo());
        } catch (Exception e) {
            log.error("处理跨院区会诊通知失败: consultationNo={}", message.getConsultationNo(), e);
        }
    }

    private void notifyPrimaryDoctor(CrossCampusNotifyDTO message) {
        try {
            String notifyContent = String.format(
                    "跨院区会诊邀请：患者%s申请于%s %s进行会诊，请及时处理。会诊房间ID：%s",
                    message.getPatientName(),
                    message.getAppointmentDate(),
                    message.getTimeSlotDesc(),
                    message.getRoomId()
            );
            log.info("通知主诊医生[{}]: {}", message.getPrimaryDoctorId(), notifyContent);

            wechatNotifyService.notifyDoctorNewConsultation(
                    message.getPrimaryDoctorId(),
                    message.getPatientName(),
                    message.getSourceCampusName(),
                    message.getAppointmentDate().toString(),
                    message.getTimeSlotDesc()
            );
        } catch (Exception e) {
            log.error("通知主诊医生失败: doctorId={}", message.getPrimaryDoctorId(), e);
        }
    }

    private void notifySourceCampus(CrossCampusNotifyDTO message) {
        try {
            log.info("通知源院区[{}]跨院区会诊申请已推送: consultationNo={}",
                    message.getSourceCampusId(), message.getConsultationNo());
        } catch (Exception e) {
            log.error("通知源院区失败: campusId={}", message.getSourceCampusId(), e);
        }
    }
}
