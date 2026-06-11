package com.telemed.common.vo;

import lombok.Data;

import java.time.LocalDate;
import java.util.List;

@Data
public class DoctorScheduleVO {

    private Long doctorId;

    private String doctorName;

    private String title;

    private String department;

    private Long campusId;

    private String campusName;

    private Long hospitalId;

    private String hospitalName;

    private LocalDate scheduleDate;

    private List<TimeSlotVO> timeSlots;
}
