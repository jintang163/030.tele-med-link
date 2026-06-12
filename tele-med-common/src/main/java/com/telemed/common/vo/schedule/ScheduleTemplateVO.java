package com.telemed.common.vo.schedule;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
public class ScheduleTemplateVO {

    private Long id;

    private Long doctorId;

    private String templateName;

    private Integer dayOfWeek;

    private String dayOfWeekLabel;

    private List<String> slotTimes;

    private Integer maxPatientsPerSlot;

    private LocalDateTime createTime;
}
