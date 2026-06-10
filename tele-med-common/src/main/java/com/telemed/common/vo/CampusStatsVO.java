package com.telemed.common.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CampusStatsVO {

    private Long campusId;

    private String campusName;

    private Long hospitalId;

    private String hospitalName;

    private Integer doctorCount;

    private Integer patientCount;

    private Integer onlineDoctorCount;

    private Long todayConsultationCount;

    private Long totalConsultationCount;

    private Long todayAppointmentCount;

    private Long totalAppointmentCount;
}
