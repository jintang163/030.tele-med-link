package com.telemed.common.dto.mediasoup;

import lombok.Data;

@Data
public class MediasoupNodeRegisterDTO {

    private String nodeName;

    private String nodeIp;

    private Integer nodePort;

    private Integer httpPort;

    private String region;

    private Integer weight;
}
