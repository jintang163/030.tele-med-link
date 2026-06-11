package com.telemed.common.vo;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
public class ConsultationVO {

    private Long id;

    private String consultationNo;

    private Long patientId;

    private String patientName;

    private Long doctorId;

    private String doctorName;

    private Long hospitalId;

    private Long campusId;

    private Integer status;

    private String statusText;

    private Integer type;

    private Long appointmentId;

    private String roomId;

    private LocalDateTime startTime;

    private LocalDateTime endTime;

    private Integer duration;

    private LocalDateTime createTime;

    private String conclusionContent;

    private String conclusionFileUrl;

    private Boolean crossCampus;

    private Long sourceCampusId;

    private String sourceCampusName;

    private Long targetCampusId;

    private String targetCampusName;

    private String campusTag;

    private List<ConsultationDoctorVO> assistantDoctors;

    private String primaryDoctorName;

    private java.time.LocalDateTime expireTime;

    private java.time.LocalDateTime confirmTime;
}
