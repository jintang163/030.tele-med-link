package com.telemed.common.constant;

public enum ScheduleSlotStatus {

    NORMAL(0, "正常"),
    SUSPENDED(1, "停诊"),
    SHIFTED(2, "调班");

    private final int code;
    private final String desc;

    ScheduleSlotStatus(int code, String desc) {
        this.code = code;
        this.desc = desc;
    }

    public int getCode() {
        return code;
    }

    public String getDesc() {
        return desc;
    }

    public static ScheduleSlotStatus fromCode(int code) {
        for (ScheduleSlotStatus status : values()) {
            if (status.code == code) {
                return status;
            }
        }
        throw new IllegalArgumentException("Unknown schedule slot status code: " + code);
    }
}
