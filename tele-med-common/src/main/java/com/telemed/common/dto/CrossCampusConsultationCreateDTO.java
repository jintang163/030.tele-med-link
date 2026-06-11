package com.telemed.common.dto;

import lombok.Data;

import java.time.LocalDate;
import java.util.List;

@Data
public class CrossCampusConsultationCreateDTO {

    private Long patientId;

    private Long sourceCampusId;

    private Long targetCampusId;

    private Long primaryDoctorId;

    private List<Long> assistantDoctorIds;

    private LocalDate appointmentDate;

    private Integer timeSlot;

    private String description;

    private String patientSymptoms;

    private Integer consultationType;
}
