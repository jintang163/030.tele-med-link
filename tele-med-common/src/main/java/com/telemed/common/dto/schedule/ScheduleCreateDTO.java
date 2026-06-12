package com.telemed.common.dto.schedule;

import lombok.Data;

import java.time.LocalDate;
import java.util.List;

@Data
public class ScheduleCreateDTO {

    private Long doctorId;

    private LocalDate scheduleDate;

    private List<String> slotTimes;

    private Integer maxPatientsPerSlot = 1;

    private Long operatorId;
}
