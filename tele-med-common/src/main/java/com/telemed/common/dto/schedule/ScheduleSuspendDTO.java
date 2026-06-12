package com.telemed.common.dto.schedule;

import lombok.Data;

@Data
public class ScheduleSuspendDTO {

    private Long scheduleId;

    private String suspendReason;

    private boolean notifyPatients = true;

    private Long operatorId;
}
