package com.telemed.common.constant;

public enum VideoUploadStatus {

    PENDING(0),
    UPLOADING(1),
    SUCCESS(2),
    FAILED(3);

    private final int code;

    VideoUploadStatus(int code) {
        this.code = code;
    }

    public int getCode() {
        return code;
    }
}
