package com.telemed.common.dto.schedule;

import lombok.Data;

import java.util.List;

@Data
public class ScheduleTemplateCreateDTO {

    private Long doctorId;

    private String templateName;

    private Integer dayOfWeek;

    private List<String> slotTimes;

    private Integer maxPatientsPerSlot = 1;
}
