package com.telemed.common.dto.schedule;

import lombok.Data;

import java.time.LocalDate;
import java.util.List;

@Data
public class ScheduleBatchCopyDTO {

    private Long doctorId;

    private LocalDate sourceStartDate;

    private LocalDate sourceEndDate;

    private List<LocalDate> targetDates;

    private Long operatorId;
}
