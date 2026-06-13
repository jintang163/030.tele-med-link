package com.telemed.common.constant;

import lombok.Getter;

@Getter
public enum DepositStatus {

    PENDING(0, "待存证"),
    DEPOSITING(1, "存证中"),
    SUCCESS(2, "存证成功"),
    FAILED(3, "存证失败"),
    RETRYING(4, "重试中");

    private final Integer code;
    private final String description;

    DepositStatus(Integer code, String description) {
        this.code = code;
        this.description = description;
    }

    public static String getDescriptionByCode(Integer code) {
        if (code == null) {
            return "未知";
        }
        for (DepositStatus status : values()) {
            if (status.getCode().equals(code)) {
                return status.getDescription();
            }
        }
        return "未知";
    }
}
