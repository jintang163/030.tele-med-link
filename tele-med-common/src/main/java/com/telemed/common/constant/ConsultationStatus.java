package com.telemed.common.constant;

public enum ConsultationStatus {

    WAITING(0),
    ONGOING(1),
    FINISHED(2),
    CANCELLED(3);

    private final int code;

    ConsultationStatus(int code) {
        this.code = code;
    }

    public int getCode() {
        return code;
    }
}
