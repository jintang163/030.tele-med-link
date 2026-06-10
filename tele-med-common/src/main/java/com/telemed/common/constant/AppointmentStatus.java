package com.telemed.common.constant;

public enum AppointmentStatus {

    PENDING(0),
    CONFIRMED(1),
    CANCELLED(2),
    COMPLETED(3);

    private final int code;

    AppointmentStatus(int code) {
        this.code = code;
    }

    public int getCode() {
        return code;
    }
}
