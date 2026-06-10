package com.telemed.common.dto;

import lombok.Data;

@Data
public class ConsultationCreateDTO {

    private Long patientId;
    private Long doctorId;
    private Integer type;
}
