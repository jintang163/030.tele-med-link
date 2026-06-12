package com.telemed.common.vo.schedule;

import lombok.Data;

import java.util.List;

@Data
public class WeeklyScheduleVO {

    private List<DailyScheduleVO> days;
}
