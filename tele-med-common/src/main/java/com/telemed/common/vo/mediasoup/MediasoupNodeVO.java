package com.telemed.common.vo.mediasoup;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class MediasoupNodeVO {

    private Long id;

    private String nodeName;

    private String nodeIp;

    private Integer nodePort;

    private Integer httpPort;

    private String region;

    private Integer weight;

    private Integer status;

    private Double cpuUsage;

    private Double memoryUsage;

    private Integer activeConsumers;

    private Integer activeProducers;

    private LocalDateTime lastHeartbeat;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;
}
