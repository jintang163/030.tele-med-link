package com.telemed.model.repository;

import com.telemed.model.entity.ScheduleTemplate;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ScheduleTemplateRepository extends JpaRepository<ScheduleTemplate, Long> {

    List<ScheduleTemplate> findByDoctorId(Long doctorId);
}
