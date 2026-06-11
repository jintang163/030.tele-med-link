package com.telemed.common.constant;

public enum TransportType {

    SEND("send"),
    RECV("recv");

    private final String value;

    TransportType(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }
}
