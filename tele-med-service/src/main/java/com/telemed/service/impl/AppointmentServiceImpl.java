package com.telemed.service.impl;

import com.telemed.common.constant.AppointmentStatus;
import com.telemed.common.constant.TimeSlot;
import com.telemed.common.exception.BusinessException;
import com.telemed.model.entity.Appointment;
import com.telemed.model.entity.Consultation;
import com.telemed.model.entity.Patient;
import com.telemed.model.repository.AppointmentRepository;
import com.telemed.model.repository.PatientRepository;
import com.telemed.service.AppointmentService;
import com.telemed.service.ConsultationService;
import com.telemed.service.WechatNotifyService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

@Service
@RequiredArgsConstructor
public class AppointmentServiceImpl implements AppointmentService {

    private final AppointmentRepository appointmentRepository;
    private final PatientRepository patientRepository;
    private final ConsultationService consultationService;
    private final WechatNotifyService wechatNotifyService;

    @Override
    @Transactional
    public Appointment createAppointment(Long patientId, Long doctorId, LocalDate date, Integer timeSlot, String description) {
        List<Appointment> existing = appointmentRepository.findByDoctorIdAndAppointmentDateAndTimeSlot(doctorId, date, timeSlot).stream()
                .filter(a -> a.getStatus() != AppointmentStatus.CANCELLED.getCode())
                .toList();
        if (!existing.isEmpty()) {
            throw new BusinessException("该医生在此时间段已有预约");
        }
        Appointment appointment = new Appointment();
        appointment.setPatientId(patientId);
        appointment.setDoctorId(doctorId);
        appointment.setAppointmentDate(date);
        appointment.setTimeSlot(timeSlot);
        appointment.setDescription(description);
        appointment.setStatus(AppointmentStatus.CONFIRMED.getCode());
        appointment = appointmentRepository.save(appointment);

        Patient patient = patientRepository.findById(patientId).orElse(null);
        String patientName = patient != null ? patient.getName() : "患者";
        String timeSlotDesc = timeSlot == 0 ? TimeSlot.MORNING.getDesc() : TimeSlot.AFTERNOON.getDesc();
        wechatNotifyService.notifyDoctorNewAppointment(doctorId, patientName, date, timeSlotDesc);

        return appointment;
    }

    @Override
    @Transactional
    public Appointment confirmAppointment(Long appointmentId, Long doctorId) {
        Appointment appointment = appointmentRepository.findById(appointmentId)
                .orElseThrow(() -> new BusinessException("预约不存在"));
        if (!appointment.getDoctorId().equals(doctorId)) {
            throw new BusinessException("无权确认此预约");
        }
        if (appointment.getStatus() != AppointmentStatus.PENDING.getCode()) {
            throw new BusinessException("预约状态不是待确认，无法确认");
        }
        appointment.setStatus(AppointmentStatus.CONFIRMED.getCode());
        return appointmentRepository.save(appointment);
    }

    @Override
    @Transactional
    public Appointment cancelAppointment(Long appointmentId, Long patientId) {
        Appointment appointment = appointmentRepository.findById(appointmentId)
                .orElseThrow(() -> new BusinessException("预约不存在"));
        if (!appointment.getPatientId().equals(patientId)) {
            throw new BusinessException("无权取消此预约");
        }
        if (appointment.getStatus() != AppointmentStatus.PENDING.getCode()
                && appointment.getStatus() != AppointmentStatus.CONFIRMED.getCode()) {
            throw new BusinessException("预约状态不允许取消");
        }
        appointment.setStatus(AppointmentStatus.CANCELLED.getCode());
        return appointmentRepository.save(appointment);
    }

    @Override
    @Transactional
    public Appointment startAppointment(Long appointmentId, Long doctorId) {
        Appointment appointment = appointmentRepository.findById(appointmentId)
                .orElseThrow(() -> new BusinessException("预约不存在"));
        if (!appointment.getDoctorId().equals(doctorId)) {
            throw new BusinessException("无权开始此预约");
        }
        if (appointment.getStatus() != AppointmentStatus.PENDING.getCode()
                && appointment.getStatus() != AppointmentStatus.CONFIRMED.getCode()) {
            throw new BusinessException("预约状态不允许开始问诊");
        }
        appointment.setStatus(AppointmentStatus.IN_PROGRESS.getCode());
        Consultation consultation = consultationService.createConsultation(
                appointment.getPatientId(), doctorId, 1);
        appointment.setConsultationId(consultation.getId());
        return appointmentRepository.save(appointment);
    }

    @Override
    @Transactional
    public void completeAppointmentByConsultationId(Long consultationId) {
        List<Appointment> appointments = appointmentRepository.findByConsultationId(consultationId);
        for (Appointment appointment : appointments) {
            if (appointment.getStatus() == AppointmentStatus.IN_PROGRESS.getCode()
                    || appointment.getStatus() == AppointmentStatus.CONFIRMED.getCode()) {
                appointment.setStatus(AppointmentStatus.COMPLETED.getCode());
                appointmentRepository.save(appointment);
            }
        }
    }

    @Override
    public List<Appointment> getPatientAppointments(Long patientId) {
        return appointmentRepository.findByPatientIdOrderByAppointmentDateDesc(patientId);
    }

    @Override
    public List<Appointment> getDoctorAppointments(Long doctorId, Integer status) {
        return appointmentRepository.findByDoctorIdAndStatus(doctorId, status);
    }

    @Override
    public List<Appointment> getUpcomingAppointments(LocalDate date, Integer timeSlot) {
        if (timeSlot == null) {
            return appointmentRepository.findByStatusAndAppointmentDate(
                    AppointmentStatus.CONFIRMED.getCode(), date);
        }
        return appointmentRepository.findByStatusAndAppointmentDateAndTimeSlot(
                AppointmentStatus.CONFIRMED.getCode(), date, timeSlot);
    }
}
