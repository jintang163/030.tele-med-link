package com.telemed.web.job;

import com.telemed.common.constant.AppointmentStatus;
import com.telemed.common.constant.TimeSlot;
import com.telemed.model.entity.Appointment;
import com.telemed.model.entity.Doctor;
import com.telemed.model.entity.Patient;
import com.telemed.model.repository.AppointmentRepository;
import com.telemed.model.repository.DoctorRepository;
import com.telemed.model.repository.PatientRepository;
import com.telemed.service.WechatNotifyService;
import org.quartz.DisallowConcurrentExecution;
import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.quartz.PersistJobDataAfterExecution;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.List;

@Component
@DisallowConcurrentExecution
@PersistJobDataAfterExecution
public class AppointmentReminderJob implements Job {

    private final AppointmentRepository appointmentRepository;
    private final WechatNotifyService wechatNotifyService;
    private final DoctorRepository doctorRepository;
    private final PatientRepository patientRepository;

    public AppointmentReminderJob(AppointmentRepository appointmentRepository,
                                  WechatNotifyService wechatNotifyService,
                                  DoctorRepository doctorRepository,
                                  PatientRepository patientRepository) {
        this.appointmentRepository = appointmentRepository;
        this.wechatNotifyService = wechatNotifyService;
        this.doctorRepository = doctorRepository;
        this.patientRepository = patientRepository;
    }

    @Override
    public void execute(JobExecutionContext context) throws JobExecutionException {
        int timeSlotCode = context.getJobDetail().getJobDataMap().getInt("timeSlot");
        LocalDate today = LocalDate.now();

        List<Appointment> appointments = appointmentRepository.findByStatusAndAppointmentDateAndTimeSlot(
                AppointmentStatus.CONFIRMED.getCode(), today, timeSlotCode);

        for (Appointment appointment : appointments) {
            Doctor doctor = doctorRepository.findById(appointment.getDoctorId()).orElse(null);
            Patient patient = patientRepository.findById(appointment.getPatientId()).orElse(null);
            if (doctor != null && patient != null) {
                String timeSlotDesc = timeSlotCode == TimeSlot.MORNING.getCode()
                        ? TimeSlot.MORNING.getDesc()
                        : TimeSlot.AFTERNOON.getDesc();
                String doctorName = doctor.getSpecialty() != null ? doctor.getSpecialty() : "医生";
                wechatNotifyService.remindPatientConsultation(
                        patient.getId(), doctorName, today, timeSlotDesc);
            }
        }
    }
}
