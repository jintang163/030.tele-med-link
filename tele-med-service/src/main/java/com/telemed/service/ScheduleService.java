package com.telemed.service;

import com.telemed.common.dto.appointment.AppointmentBookDTO;
import com.telemed.common.dto.appointment.AppointmentRescheduleDTO;
import com.telemed.common.dto.schedule.ScheduleBatchCopyDTO;
import com.telemed.common.dto.schedule.ScheduleCreateDTO;
import com.telemed.common.dto.schedule.ScheduleShiftDTO;
import com.telemed.common.dto.schedule.ScheduleSuspendDTO;
import com.telemed.common.dto.schedule.ScheduleTemplateApplyDTO;
import com.telemed.common.dto.schedule.ScheduleTemplateCreateDTO;
import com.telemed.common.vo.notification.NotificationVO;
import com.telemed.common.vo.schedule.DailyScheduleVO;
import com.telemed.common.vo.schedule.ScheduleSlotVO;
import com.telemed.common.vo.schedule.ScheduleTemplateVO;
import com.telemed.common.vo.schedule.WeeklyScheduleVO;
import com.telemed.model.entity.Appointment;

import java.time.LocalDate;
import java.util.List;

public interface ScheduleService {

    List<ScheduleSlotVO> createSchedule(ScheduleCreateDTO dto);

    WeeklyScheduleVO getWeeklySchedule(Long doctorId, LocalDate weekStart, boolean includeSuspended);

    DailyScheduleVO getDaySchedule(Long doctorId, LocalDate date);

    List<ScheduleSlotVO> batchCopySchedule(ScheduleBatchCopyDTO dto);

    ScheduleSlotVO suspendSchedule(ScheduleSuspendDTO dto);

    ScheduleSlotVO resumeSchedule(Long scheduleId, Long operatorId);

    ScheduleSlotVO shiftSchedule(ScheduleShiftDTO dto);

    void deleteSchedule(Long scheduleId, Long operatorId);

    void deleteDaySchedule(Long doctorId, LocalDate date, Long operatorId);

    String[] getAllSlotTimes();

    List<ScheduleTemplateVO> getTemplates(Long doctorId);

    ScheduleTemplateVO createTemplate(ScheduleTemplateCreateDTO dto);

    List<ScheduleSlotVO> applyTemplate(Long templateId, ScheduleTemplateApplyDTO dto);

    void deleteTemplate(Long id);

    Appointment bookAppointment(AppointmentBookDTO dto);

    Appointment rescheduleAppointment(AppointmentRescheduleDTO dto);

    Appointment cancelAppointmentByPatient(Long appointmentId, Long patientId);

    void autoRescheduleForSuspendedSlot(Long suspendedSlotId);

    List<NotificationVO> getPatientNotifications(Long patientId);

    void markNotificationAsRead(Long id, Long patientId);

    Appointment getAppointmentDetail(Long id);
}
