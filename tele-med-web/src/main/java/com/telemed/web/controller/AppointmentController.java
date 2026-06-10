package com.telemed.web.controller;

import com.telemed.common.dto.AppointmentCreateDTO;
import com.telemed.common.result.Result;
import com.telemed.model.entity.Appointment;
import com.telemed.service.AppointmentService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/appointment")
public class AppointmentController {

    @Autowired
    private AppointmentService appointmentService;

    @PostMapping("/create")
    public Result<Appointment> create(@RequestBody AppointmentCreateDTO dto) {
        Appointment appointment = appointmentService.createAppointment(
                dto.getPatientId(), dto.getDoctorId(),
                dto.getAppointmentDate(), dto.getTimeSlot(), dto.getDescription());
        return Result.ok(appointment);
    }

    @PostMapping("/confirm")
    public Result<Appointment> confirm(@RequestParam Long appointmentId, @RequestParam Long doctorId) {
        Appointment appointment = appointmentService.confirmAppointment(appointmentId, doctorId);
        return Result.ok(appointment);
    }

    @PostMapping("/cancel")
    public Result<Appointment> cancel(@RequestParam Long appointmentId, @RequestParam Long patientId) {
        Appointment appointment = appointmentService.cancelAppointment(appointmentId, patientId);
        return Result.ok(appointment);
    }

    @PostMapping("/start")
    public Result<Appointment> start(@RequestParam Long appointmentId, @RequestParam Long doctorId) {
        Appointment appointment = appointmentService.startAppointment(appointmentId, doctorId);
        return Result.ok(appointment);
    }

    @GetMapping("/patient/{patientId}")
    public Result<List<Appointment>> patientAppointments(@PathVariable Long patientId) {
        List<Appointment> list = appointmentService.getPatientAppointments(patientId);
        return Result.ok(list);
    }

    @GetMapping("/doctor/{doctorId}")
    public Result<List<Appointment>> doctorAppointments(@PathVariable Long doctorId,
                                                         @RequestParam(required = false) Integer status) {
        List<Appointment> list = appointmentService.getDoctorAppointments(doctorId, status);
        return Result.ok(list);
    }
}
