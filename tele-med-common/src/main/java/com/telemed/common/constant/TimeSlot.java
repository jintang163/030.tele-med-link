package com.telemed.common.constant;

public enum TimeSlot {

    MORNING(0, "上午"),
    AFTERNOON(1, "下午");

    private final int code;
    private final String desc;

    TimeSlot(int code, String desc) {
        this.code = code;
        this.desc = desc;
    }

    public int getCode() {
        return code;
    }

    public String getDesc() {
        return desc;
    }
}
