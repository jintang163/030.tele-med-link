package com.telemed.model.repository;

import com.telemed.model.entity.Appointment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public interface AppointmentRepository extends JpaRepository<Appointment, Long> {

    List<Appointment> findByDoctorIdAndAppointmentDateAndTimeSlot(Long doctorId, LocalDate appointmentDate, Integer timeSlot);

    List<Appointment> findByPatientIdOrderByAppointmentDateDesc(Long patientId);

    List<Appointment> findByStatusAndAppointmentDate(Integer status, LocalDate appointmentDate);

    List<Appointment> findByStatusAndAppointmentDateAndTimeSlot(Integer status, LocalDate appointmentDate, Integer timeSlot);

    List<Appointment> findByDoctorIdAndStatus(Long doctorId, Integer status);

    List<Appointment> findByPatientIdAndStatus(Long patientId, Integer status);

    List<Appointment> findByConsultationId(Long consultationId);

    List<Appointment> findByCrossCampusTrue();

    List<Appointment> findByCrossCampusTrueAndStatus(Integer status);

    List<Appointment> findByTargetCampusIdAndStatus(Long targetCampusId, Integer status);

    @Query("SELECT a FROM Appointment a WHERE a.status = :status AND a.expireTime < :now AND a.crossCampus = true")
    List<Appointment> findExpiredCrossCampusAppointments(@Param("status") Integer status, @Param("now") LocalDateTime now);

    @Query("SELECT a FROM Appointment a WHERE a.doctorId = :doctorId AND a.appointmentDate = :date AND a.status <> :excludeStatus")
    List<Appointment> findDoctorAppointmentsByDateExcludingStatus(@Param("doctorId") Long doctorId,
                                                                    @Param("date") LocalDate date,
                                                                    @Param("excludeStatus") Integer excludeStatus);

    @Query("SELECT a FROM Appointment a WHERE a.scheduleSlotId = :scheduleSlotId AND a.status <> :excludeStatus")
    List<Appointment> findByScheduleSlotIdAndStatusNot(@Param("scheduleSlotId") Long scheduleSlotId,
                                                        @Param("excludeStatus") Integer excludeStatus);

    List<Appointment> findByScheduleSlotId(Long scheduleSlotId);

    @Query("SELECT a FROM Appointment a WHERE a.patientId = :patientId AND a.scheduleSlotId = :scheduleSlotId AND a.status <> :excludeStatus")
    List<Appointment> findByPatientIdAndScheduleSlotIdAndStatusNot(@Param("patientId") Long patientId,
                                                                     @Param("scheduleSlotId") Long scheduleSlotId,
                                                                     @Param("excludeStatus") Integer excludeStatus);
}
