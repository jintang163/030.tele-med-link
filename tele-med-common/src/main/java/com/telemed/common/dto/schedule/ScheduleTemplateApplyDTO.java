package com.telemed.common.dto.schedule;

import lombok.Data;

import java.time.LocalDate;
import java.util.List;

@Data
public class ScheduleTemplateApplyDTO {

    private Long doctorId;

    private List<LocalDate> targetDates;

    private Long operatorId;
}
