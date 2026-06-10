package com.telemed.model.repository;

import com.telemed.model.entity.Appointment;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;

public interface AppointmentRepository extends JpaRepository<Appointment, Long> {

    List<Appointment> findByDoctorIdAndAppointmentDateAndTimeSlot(Long doctorId, LocalDate appointmentDate, Integer timeSlot);

    List<Appointment> findByPatientIdOrderByAppointmentDateDesc(Long patientId);

    List<Appointment> findByStatusAndAppointmentDate(Integer status, LocalDate appointmentDate);

    List<Appointment> findByStatusAndAppointmentDateAndTimeSlot(Integer status, LocalDate appointmentDate, Integer timeSlot);

    List<Appointment> findByDoctorIdAndStatus(Long doctorId, Integer status);

    List<Appointment> findByPatientIdAndStatus(Long patientId, Integer status);

    List<Appointment> findByConsultationId(Long consultationId);
}
