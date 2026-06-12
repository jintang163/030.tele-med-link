package com.telemed.common.vo.signature;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class ConsultationSignatureVO {

    private Long id;

    private Long consultationId;

    private Long doctorId;

    private String doctorName;

    private String doctorTitle;

    private String department;

    private Integer signOrder;

    private Integer signStatus;

    private String signStatusText;

    private String signatureImageUrl;

    private String sm2PublicKey;

    private Float signPositionX;

    private Float signPositionY;

    private Float signWidth;

    private Float signHeight;

    private Integer signPage;

    private String signReason;

    private String signLocation;

    private LocalDateTime signTime;

    private LocalDateTime createTime;
}
