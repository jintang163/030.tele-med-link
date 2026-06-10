package com.telemed.common.dto;

import lombok.Data;

import java.time.LocalDate;

@Data
public class AppointmentCreateDTO {

    private Long patientId;
    private Long doctorId;
    private LocalDate appointmentDate;
    private Integer timeSlot;
    private String description;
}
