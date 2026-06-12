package com.telemed.common.dto.appointment;

import lombok.Data;

@Data
public class AppointmentBookDTO {

    private Long scheduleSlotId;

    private Long patientId;

    private String description;

    private String patientName;
}
