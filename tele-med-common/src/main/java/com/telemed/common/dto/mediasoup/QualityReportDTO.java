package com.telemed.common.dto.mediasoup;

import lombok.Data;

@Data
public class QualityReportDTO {

    private Long userId;

    private Long consultationId;

    private String transportId;

    private String kind;

    private Double packetLostRate;

    private Long jitter;

    private Long roundTripTime;

    private Long bitrate;

    private String resolution;
}
