package com.telemed.model.repository;

import com.telemed.model.entity.PatientNotification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface PatientNotificationRepository extends JpaRepository<PatientNotification, Long> {

    List<PatientNotification> findByPatientIdOrderByCreateTimeDesc(Long patientId);

    List<PatientNotification> findByPatientIdAndStatus(Long patientId, Integer status);

    @Modifying
    @Query("UPDATE PatientNotification n SET n.status = 1 WHERE n.id = :id AND n.patientId = :patientId")
    int markAsRead(@Param("id") Long id, @Param("patientId") Long patientId);
}
