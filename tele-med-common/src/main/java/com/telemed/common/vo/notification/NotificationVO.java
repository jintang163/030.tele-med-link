package com.telemed.common.vo.notification;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class NotificationVO {

    private Long id;

    private Long patientId;

    private String patientName;

    private String typeText;

    private String title;

    private String content;

    private String statusText;

    private Long appointmentId;

    private LocalDateTime createTime;
}
