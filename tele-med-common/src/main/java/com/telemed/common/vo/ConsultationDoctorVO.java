package com.telemed.common.vo;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class ConsultationDoctorVO {

    private Long id;

    private Long doctorId;

    private String doctorName;

    private String title;

    private String department;

    private Long campusId;

    private String campusName;

    private Integer roleType;

    private String roleTypeText;

    private Integer joinStatus;

    private String joinStatusText;

    private LocalDateTime joinTime;

    private LocalDateTime leaveTime;
}
