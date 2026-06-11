package com.telemed.common.vo.mediasoup;

import lombok.Data;

@Data
public class TransportConnectVO {

    private String id;

    private String iceParameters;

    private String iceCandidates;

    private String dtlsParameters;
}
