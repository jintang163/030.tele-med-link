package com.telemed.common.dto.mediasoup;

import lombok.Data;

@Data
public class ConsumerCreateDTO {

    private Long consultationId;

    private Long userId;

    private String transportId;

    private String producerId;

    private String rtpCapabilities;
}
