package com.telemed.common.vo.mediasoup;

import lombok.Data;

@Data
public class ConsumerVO {

    private String id;

    private String producerId;

    private String kind;

    private Long userId;

    private Boolean paused;

    private String rtpParameters;
}
