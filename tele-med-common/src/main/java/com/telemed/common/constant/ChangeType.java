package com.telemed.common.constant;

public enum ChangeType {

    SUSPEND("停诊"),
    RESUME("复诊"),
    SHIFT("调班"),
    AUTO_RESCHEDULE("自动改约");

    private final String desc;

    ChangeType(String desc) {
        this.desc = desc;
    }

    public String getDesc() {
        return desc;
    }
}
