package com.telemed.common.vo.mediasoup;

import lombok.Data;

@Data
public class QualityAdviceVO {

    private String targetResolution;

    private Long targetBitrate;

    private Boolean shouldDowngrade;

    private String reason;
}
