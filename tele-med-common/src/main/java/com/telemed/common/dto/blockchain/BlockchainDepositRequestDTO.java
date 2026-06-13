package com.telemed.common.dto.blockchain;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class BlockchainDepositRequestDTO {

    private Long consultationId;

    private String consultationNo;

    private String fileHash;

    private String hashAlgorithm;

    private String pdfUrl;

    private String patientName;

    private String patientIdCard;

    private String doctorName;

    private LocalDateTime signTime;

    private Map<String, Object> extraData;
}
