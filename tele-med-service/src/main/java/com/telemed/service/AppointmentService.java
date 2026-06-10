package com.telemed.service;

import com.telemed.model.entity.Appointment;

import java.time.LocalDate;
import java.util.List;

public interface AppointmentService {

    Appointment createAppointment(Long patientId, Long doctorId, LocalDate date, Integer timeSlot, String description);

    Appointment cancelAppointment(Long appointmentId, Long patientId);

    Appointment startAppointment(Long appointmentId, Long doctorId);

    List<Appointment> getPatientAppointments(Long patientId);

    List<Appointment> getDoctorAppointments(Long doctorId, Integer status);

    List<Appointment> getUpcomingAppointments(LocalDate date, Integer timeSlot);
}
