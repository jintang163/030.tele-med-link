package com.telemed.web.controller;

import com.telemed.common.dto.AppointmentCreateDTO;
import com.telemed.common.dto.appointment.AppointmentBookDTO;
import com.telemed.common.dto.appointment.AppointmentRescheduleDTO;
import com.telemed.common.result.Result;
import com.telemed.common.vo.notification.NotificationVO;
import com.telemed.model.entity.Appointment;
import com.telemed.service.AppointmentService;
import com.telemed.service.ScheduleService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/appointment")
public class AppointmentController {

    @Autowired
    private AppointmentService appointmentService;

    @Autowired
    private ScheduleService scheduleService;

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
        Appointment appointment = scheduleService.cancelAppointmentByPatient(appointmentId, patientId);
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

    @PostMapping("/book")
    public Result<Appointment> book(@RequestBody AppointmentBookDTO dto) {
        Appointment appointment = scheduleService.bookAppointment(dto);
        return Result.ok(appointment);
    }

    @PostMapping("/reschedule")
    public Result<Appointment> reschedule(@RequestBody AppointmentRescheduleDTO dto) {
        Appointment appointment = scheduleService.rescheduleAppointment(dto);
        return Result.ok(appointment);
    }

    @GetMapping("/notifications")
    public Result<List<NotificationVO>> notifications(@RequestParam Long patientId) {
        List<NotificationVO> list = scheduleService.getPatientNotifications(patientId);
        return Result.ok(list);
    }

    @PostMapping("/notifications/{id}/read")
    public Result<Void> markNotificationRead(
            @PathVariable Long id,
            @RequestParam Long patientId) {
        scheduleService.markNotificationAsRead(id, patientId);
        return Result.ok();
    }

    @GetMapping("/{id}")
    public Result<Appointment> detail(@PathVariable Long id) {
        Appointment appointment = scheduleService.getAppointmentDetail(id);
        return Result.ok(appointment);
    }
}
