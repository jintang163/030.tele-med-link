package com.telemed.common.dto.mediasoup;

import lombok.Data;

@Data
public class TransportCreateDTO {

    private Long consultationId;

    private Long userId;

    private String kind;
}
