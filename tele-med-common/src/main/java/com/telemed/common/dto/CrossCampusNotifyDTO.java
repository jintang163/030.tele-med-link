package com.telemed.common.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CrossCampusNotifyDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long consultationId;

    private String consultationNo;

    private Long patientId;

    private String patientName;

    private Long primaryDoctorId;

    private String primaryDoctorName;

    private Long sourceCampusId;

    private String sourceCampusName;

    private Long targetCampusId;

    private String targetCampusName;

    private String campusTag;

    private LocalDate appointmentDate;

    private Integer timeSlot;

    private String timeSlotDesc;

    private String description;

    private String roomId;

    private LocalDateTime expireTime;

    private LocalDateTime createTime;
}
