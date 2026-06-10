package com.telemed.common.dto;

import lombok.Data;

@Data
public class SignalingMessage {

    private String type;
    private String from;
    private String to;
    private String roomId;
    private Object payload;
    private Long timestamp;
}
