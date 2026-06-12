package com.telemed.common.dto.schedule;

import lombok.Data;

import java.time.LocalDate;

@Data
public class ScheduleShiftDTO {

    private Long scheduleId;

    private Long shiftToDoctorId;

    private LocalDate shiftToDate;

    private String shiftToSlotTime;

    private boolean autoReschedule = true;

    private Long operatorId;
}
