package com.telemed.common.vo.mediasoup;

import lombok.Data;

@Data
public class NearestNodeVO {

    private Long nodeId;

    private String nodeUrl;

    private String wsUrl;

    private String region;

    private Integer latencyMs;

    private RouterRtpCapabilitiesVO rtpCapabilities;
}
