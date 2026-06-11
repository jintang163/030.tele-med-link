package com.telemed.common.dto.mediasoup;

import lombok.Data;

@Data
public class MediasoupNodeHeartbeatDTO {

    private Long nodeId;

    private Double cpuUsage;

    private Double memoryUsage;

    private Integer activeConsumers;

    private Integer activeProducers;
}
