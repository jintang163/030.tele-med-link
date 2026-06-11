package com.telemed.common.vo.mediasoup;

import lombok.Data;

@Data
public class ProducerVO {

    private String id;

    private String kind;

    private Long userId;

    private Long consultationId;

    private Boolean paused;
}
