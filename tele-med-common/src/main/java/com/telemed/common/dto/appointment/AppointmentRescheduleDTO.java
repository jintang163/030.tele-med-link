package com.telemed.common.dto.appointment;

import lombok.Data;

@Data
public class AppointmentRescheduleDTO {

    private Long appointmentId;

    private Long newScheduleSlotId;

    private String reason;

    private Long operatorId;
}
