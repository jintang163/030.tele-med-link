package com.telemed.common.vo;

import lombok.Data;

import java.time.LocalTime;

@Data
public class TimeSlotVO {

    private Integer code;

    private String name;

    private LocalTime startTime;

    private LocalTime endTime;

    private Boolean available;

    private Integer remainingCapacity;
}
