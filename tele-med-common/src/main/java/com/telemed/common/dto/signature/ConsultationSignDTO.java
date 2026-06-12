package com.telemed.common.dto.signature;

import lombok.Data;

@Data
public class ConsultationSignDTO {

    private Long consultationId;

    private Long doctorId;

    private String signatureData;

    private Float signPositionX;

    private Float signPositionY;

    private Float signWidth;

    private Float signHeight;

    private Integer signPage;

    private String signReason;

    private String signLocation;

    private String sm2PublicKey;

    private String sm2PrivateKey;

    private String sm2Signature;

    private String pdfBase64;
}
