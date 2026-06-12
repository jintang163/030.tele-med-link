package com.telemed.common.vo.schedule;

import lombok.Data;

import java.time.LocalDate;
import java.util.List;

@Data
public class DailyScheduleVO {

    private LocalDate date;

    private Integer dayOfWeek;

    private List<ScheduleSlotVO> slots;
}
