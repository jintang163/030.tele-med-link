package com.telemed.model.repository;

import com.telemed.model.entity.ScheduleChangeLog;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ScheduleChangeLogRepository extends JpaRepository<ScheduleChangeLog, Long> {

    List<ScheduleChangeLog> findByScheduleSlotIdOrderByCreateTimeDesc(Long scheduleSlotId);
}
