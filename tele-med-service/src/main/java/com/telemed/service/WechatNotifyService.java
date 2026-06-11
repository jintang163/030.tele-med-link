package com.telemed.service;

import java.time.LocalDate;

public interface WechatNotifyService {

    void notifyDoctorNewAppointment(Long doctorId, String patientName, LocalDate date, String timeSlot);

    void remindPatientConsultation(Long patientId, String doctorName, LocalDate date, String timeSlot);

    void notifyDoctorNewConsultation(Long doctorId, String patientName, String sourceCampusName, String date, String timeSlot);

    void notifyCrossCampusConsultationResult(Long patientId, String doctorName, boolean accepted, String reason);

    String getAccessToken();
}
