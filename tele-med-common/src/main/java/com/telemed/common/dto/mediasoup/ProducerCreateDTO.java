package com.telemed.common.dto.mediasoup;

import lombok.Data;

@Data
public class ProducerCreateDTO {

    private Long consultationId;

    private Long userId;

    private String transportId;

    private String kind;

    private String rtpParameters;
}
