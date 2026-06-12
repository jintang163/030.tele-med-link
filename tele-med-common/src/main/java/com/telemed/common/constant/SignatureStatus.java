package com.telemed.common.constant;

public enum SignatureStatus {

    PENDING(0, "待签名"),
    SIGNED(1, "已签名"),
    REJECTED(2, "已拒签"),
    INVALID(3, "已失效");

    private final int code;
    private final String description;

    SignatureStatus(int code, String description) {
        this.code = code;
        this.description = description;
    }

    public int getCode() {
        return code;
    }

    public String getDescription() {
        return description;
    }
}
