package com.telemed.common.vo;

import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
public class AppointmentVO {

    private Long id;

    private Long patientId;

    private String patientName;

    private Long doctorId;

    private String doctorName;

    private String doctorTitle;

    private String doctorDepartment;

    private Long hospitalId;

    private String hospitalName;

    private LocalDate appointmentDate;

    private Integer timeSlot;

    private String timeSlotDesc;

    private Integer status;

    private String statusText;

    private String description;

    private Long consultationId;

    private Boolean crossCampus;

    private Long campusId;

    private String campusName;

    private Long targetCampusId;

    private String targetCampusName;

    private String campusTag;

    private LocalDateTime createTime;

    private LocalDateTime expireTime;
}
