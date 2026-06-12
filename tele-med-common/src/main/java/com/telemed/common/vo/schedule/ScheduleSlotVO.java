package com.telemed.common.vo.schedule;

import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
public class ScheduleSlotVO {

    private Long id;

    private Long doctorId;

    private String doctorName;

    private LocalDate scheduleDate;

    private String slotTime;

    private String status;

    private Integer maxPatients;

    private Integer remaining;

    private String suspendReason;

    private Long shiftToDoctorId;

    private String shiftToDoctorName;

    private LocalDate shiftToDate;

    private String shiftToSlotTime;

    private LocalDateTime createTime;

    private Integer bookedCount;
}
