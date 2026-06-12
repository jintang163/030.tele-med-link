package com.telemed.common.constant;

public enum NotificationType {

    APPOINTMENT_REMINDER("预约提醒"),
    SCHEDULE_SUSPENDED("排班停诊"),
    APPOINTMENT_RESCHEDULED("预约改约");

    private final String desc;

    NotificationType(String desc) {
        this.desc = desc;
    }

    public String getDesc() {
        return desc;
    }
}
