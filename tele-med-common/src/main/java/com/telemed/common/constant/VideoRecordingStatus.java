package com.telemed.common.constant;

public enum VideoRecordingStatus {

    PENDING_AUTHORIZATION(0),
    RECORDING(1),
    UPLOADING(2),
    PROCESSING(3),
    COMPLETED(4),
    FAILED(5),
    CANCELLED(6),
    EXPIRED(7);

    private final int code;

    VideoRecordingStatus(int code) {
        this.code = code;
    }

    public int getCode() {
        return code;
    }
}
