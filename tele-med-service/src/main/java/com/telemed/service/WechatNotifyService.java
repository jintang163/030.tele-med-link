package com.telemed.service;

import java.time.LocalDate;

public interface WechatNotifyService {

    void notifyDoctorNewAppointment(Long doctorId, String patientName, LocalDate date, String timeSlot);

    void remindPatientConsultation(Long patientId, String doctorName, LocalDate date, String timeSlot);

    String getAccessToken();
}
