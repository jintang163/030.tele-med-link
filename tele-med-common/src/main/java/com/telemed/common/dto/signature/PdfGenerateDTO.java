package com.telemed.common.dto.signature;

import lombok.Data;

import java.util.List;

@Data
public class PdfGenerateDTO {

    private Long consultationId;

    private String conclusionContent;

    private List<String> imageUrls;

    private List<String> whiteboardImageUrls;

    private String patientName;

    private String patientIdCard;

    private String patientMedicalCardNo;

    private Integer patientGender;

    private Integer patientAge;

    private String doctorName;

    private String doctorTitle;

    private String department;

    private String hospitalName;

    private String consultationNo;
}
