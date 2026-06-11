package com.telemed.common.constant;

public enum MediaKind {

    AUDIO("audio"),
    VIDEO("video");

    private final String value;

    MediaKind(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }
}
