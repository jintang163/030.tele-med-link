package com.telemed.service.impl;

import com.telemed.common.constant.AppointmentStatus;
import com.telemed.common.constant.ChangeType;
import com.telemed.common.constant.NotificationType;
import com.telemed.common.constant.ScheduleSlotConstants;
import com.telemed.common.constant.ScheduleSlotStatus;
import com.telemed.common.dto.appointment.AppointmentBookDTO;
import com.telemed.common.dto.appointment.AppointmentRescheduleDTO;
import com.telemed.common.dto.schedule.ScheduleBatchCopyDTO;
import com.telemed.common.dto.schedule.ScheduleCreateDTO;
import com.telemed.common.dto.schedule.ScheduleShiftDTO;
import com.telemed.common.dto.schedule.ScheduleSuspendDTO;
import com.telemed.common.dto.schedule.ScheduleTemplateApplyDTO;
import com.telemed.common.dto.schedule.ScheduleTemplateCreateDTO;
import com.telemed.common.exception.BusinessException;
import com.telemed.common.util.RedisDistributedLock;
import com.telemed.common.vo.notification.NotificationVO;
import com.telemed.common.vo.schedule.DailyScheduleVO;
import com.telemed.common.vo.schedule.ScheduleSlotVO;
import com.telemed.common.vo.schedule.ScheduleTemplateVO;
import com.telemed.common.vo.schedule.WeeklyScheduleVO;
import com.telemed.model.entity.Appointment;
import com.telemed.model.entity.Doctor;
import com.telemed.model.entity.DoctorScheduleSlot;
import com.telemed.model.entity.Patient;
import com.telemed.model.entity.PatientNotification;
import com.telemed.model.entity.ScheduleChangeLog;
import com.telemed.model.entity.ScheduleTemplate;
import com.telemed.model.entity.User;
import com.telemed.model.repository.AppointmentRepository;
import com.telemed.model.repository.DoctorRepository;
import com.telemed.model.repository.DoctorScheduleSlotRepository;
import com.telemed.model.repository.PatientNotificationRepository;
import com.telemed.model.repository.PatientRepository;
import com.telemed.model.repository.ScheduleChangeLogRepository;
import com.telemed.model.repository.ScheduleTemplateRepository;
import com.telemed.model.repository.UserRepository;
import com.telemed.service.ScheduleService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ScheduleServiceImpl implements ScheduleService {

    private final DoctorScheduleSlotRepository slotRepository;
    private final ScheduleTemplateRepository templateRepository;
    private final ScheduleChangeLogRepository changeLogRepository;
    private final PatientNotificationRepository notificationRepository;
    private final AppointmentRepository appointmentRepository;
    private final DoctorRepository doctorRepository;
    private final UserRepository userRepository;
    private final PatientRepository patientRepository;
    private final RedisDistributedLock redisDistributedLock;

    private static final String[] DAY_OF_WEEK_LABELS = {"", "周一", "周二", "周三", "周四", "周五", "周六", "周日"};
    private static final int LOCK_EXPIRE_SECONDS = 30;

    @Override
    @Transactional
    public List<ScheduleSlotVO> createSchedule(ScheduleCreateDTO dto) {
        if (dto.getDoctorId() == null || dto.getScheduleDate() == null) {
            throw new BusinessException("医生ID和排班日期不能为空");
        }
        if (dto.getSlotTimes() == null || dto.getSlotTimes().isEmpty()) {
            throw new BusinessException("请选择排班时段");
        }
        List<DoctorScheduleSlot> slots = new ArrayList<>();
        for (String slotTime : dto.getSlotTimes()) {
            long count = slotRepository.countByDoctorIdAndScheduleDateAndSlotTime(
                    dto.getDoctorId(), dto.getScheduleDate(), slotTime);
            if (count > 0) {
                continue;
            }
            DoctorScheduleSlot slot = new DoctorScheduleSlot();
            slot.setDoctorId(dto.getDoctorId());
            slot.setScheduleDate(dto.getScheduleDate());
            slot.setSlotTime(slotTime);
            slot.setSlotIndex(ScheduleSlotConstants.getSlotIndex(slotTime));
            slot.setMaxPatients(dto.getMaxPatientsPerSlot() != null ? dto.getMaxPatientsPerSlot() : 1);
            slot.setBookedCount(0);
            slot.setStatus(ScheduleSlotStatus.NORMAL.getCode());
            slot.setOperatorId(dto.getOperatorId());
            slots.add(slotRepository.save(slot));
        }
        return slots.stream().map(this::convertToSlotVO).collect(Collectors.toList());
    }

    @Override
    public WeeklyScheduleVO getWeeklySchedule(Long doctorId, LocalDate weekStart, boolean includeSuspended) {
        WeeklyScheduleVO weekly = new WeeklyScheduleVO();
        List<DailyScheduleVO> days = new ArrayList<>();
        LocalDate weekEnd = weekStart.plusDays(6);
        List<DoctorScheduleSlot> allSlots = slotRepository
                .findByDoctorIdAndScheduleDateBetweenOrderByScheduleDateAscSlotIndexAsc(doctorId, weekStart, weekEnd);
        if (!includeSuspended) {
            allSlots = allSlots.stream()
                    .filter(s -> s.getStatus() != ScheduleSlotStatus.SUSPENDED.getCode())
                    .collect(Collectors.toList());
        }
        Map<LocalDate, List<DoctorScheduleSlot>> slotMap = allSlots.stream()
                .collect(Collectors.groupingBy(DoctorScheduleSlot::getScheduleDate));
        for (int i = 0; i < 7; i++) {
            LocalDate date = weekStart.plusDays(i);
            DailyScheduleVO daily = new DailyScheduleVO();
            daily.setDate(date);
            daily.setDayOfWeek(date.getDayOfWeek().getValue());
            List<DoctorScheduleSlot> daySlots = slotMap.getOrDefault(date, new ArrayList<>());
            daily.setSlots(daySlots.stream().map(this::convertToSlotVO).collect(Collectors.toList()));
            days.add(daily);
        }
        weekly.setDays(days);
        return weekly;
    }

    @Override
    public DailyScheduleVO getDaySchedule(Long doctorId, LocalDate date) {
        DailyScheduleVO daily = new DailyScheduleVO();
        daily.setDate(date);
        daily.setDayOfWeek(date.getDayOfWeek().getValue());
        List<DoctorScheduleSlot> slots = slotRepository.findByDoctorIdAndScheduleDateOrderBySlotIndexAsc(doctorId, date);
        daily.setSlots(slots.stream().map(this::convertToSlotVO).collect(Collectors.toList()));
        return daily;
    }

    @Override
    @Transactional
    public List<ScheduleSlotVO> batchCopySchedule(ScheduleBatchCopyDTO dto) {
        List<DoctorScheduleSlot> sourceSlots = slotRepository
                .findByDoctorIdAndScheduleDateBetweenOrderByScheduleDateAscSlotIndexAsc(
                        dto.getDoctorId(), dto.getSourceStartDate(), dto.getSourceEndDate());
        if (sourceSlots.isEmpty()) {
            throw new BusinessException("源日期范围内没有排班记录");
        }
        Map<DayOfWeek, List<DoctorScheduleSlot>> sourceByDayOfWeek = sourceSlots.stream()
                .collect(Collectors.groupingBy(s -> s.getScheduleDate().getDayOfWeek()));
        List<DoctorScheduleSlot> created = new ArrayList<>();
        for (LocalDate targetDate : dto.getTargetDates()) {
            List<DoctorScheduleSlot> sourceForDay = sourceByDayOfWeek.get(targetDate.getDayOfWeek());
            if (sourceForDay == null || sourceForDay.isEmpty()) {
                continue;
            }
            for (DoctorScheduleSlot source : sourceForDay) {
                long count = slotRepository.countByDoctorIdAndScheduleDateAndSlotTime(
                        dto.getDoctorId(), targetDate, source.getSlotTime());
                if (count > 0) {
                    continue;
                }
                DoctorScheduleSlot slot = new DoctorScheduleSlot();
                slot.setDoctorId(dto.getDoctorId());
                slot.setScheduleDate(targetDate);
                slot.setSlotTime(source.getSlotTime());
                slot.setSlotIndex(source.getSlotIndex());
                slot.setMaxPatients(source.getMaxPatients());
                slot.setBookedCount(0);
                slot.setStatus(ScheduleSlotStatus.NORMAL.getCode());
                slot.setOperatorId(dto.getOperatorId());
                created.add(slotRepository.save(slot));
            }
        }
        return created.stream().map(this::convertToSlotVO).collect(Collectors.toList());
    }

    @Override
    @Transactional
    public ScheduleSlotVO suspendSchedule(ScheduleSuspendDTO dto) {
        DoctorScheduleSlot slot = slotRepository.findById(dto.getScheduleId())
                .orElseThrow(() -> new BusinessException("排班记录不存在"));
        if (slot.getStatus() == ScheduleSlotStatus.SUSPENDED.getCode()) {
            throw new BusinessException("该时段已处于停诊状态");
        }
        slot.setStatus(ScheduleSlotStatus.SUSPENDED.getCode());
        slot.setSuspendReason(dto.getSuspendReason());
        slot.setOperatorId(dto.getOperatorId());
        slotRepository.save(slot);

        ScheduleChangeLog log = new ScheduleChangeLog();
        log.setScheduleSlotId(slot.getId());
        log.setChangeType(ChangeType.SUSPEND.name());
        log.setOldValue(String.valueOf(ScheduleSlotStatus.NORMAL.getCode()));
        log.setNewValue(String.valueOf(ScheduleSlotStatus.SUSPENDED.getCode()));
        log.setReason(dto.getSuspendReason());
        log.setOperatorId(dto.getOperatorId());
        changeLogRepository.save(log);

        if (dto.isNotifyPatients()) {
            autoRescheduleForSuspendedSlot(slot.getId());
        }
        return convertToSlotVO(slot);
    }

    @Override
    @Transactional
    public ScheduleSlotVO resumeSchedule(Long scheduleId, Long operatorId) {
        DoctorScheduleSlot slot = slotRepository.findById(scheduleId)
                .orElseThrow(() -> new BusinessException("排班记录不存在"));
        if (slot.getStatus() != ScheduleSlotStatus.SUSPENDED.getCode()) {
            throw new BusinessException("该时段未处于停诊状态");
        }
        slot.setStatus(ScheduleSlotStatus.NORMAL.getCode());
        slot.setSuspendReason(null);
        slot.setOperatorId(operatorId);
        slotRepository.save(slot);

        ScheduleChangeLog log = new ScheduleChangeLog();
        log.setScheduleSlotId(slot.getId());
        log.setChangeType(ChangeType.RESUME.name());
        log.setOldValue(String.valueOf(ScheduleSlotStatus.SUSPENDED.getCode()));
        log.setNewValue(String.valueOf(ScheduleSlotStatus.NORMAL.getCode()));
        log.setOperatorId(operatorId);
        changeLogRepository.save(log);

        return convertToSlotVO(slot);
    }

    @Override
    @Transactional
    public ScheduleSlotVO shiftSchedule(ScheduleShiftDTO dto) {
        DoctorScheduleSlot slot = slotRepository.findById(dto.getScheduleId())
                .orElseThrow(() -> new BusinessException("原排班记录不存在"));
        if (slot.getStatus() != ScheduleSlotStatus.NORMAL.getCode()) {
            throw new BusinessException("只有正常状态的排班才能调班");
        }
        DoctorScheduleSlot targetSlot = null;
        if (dto.getShiftToSlotTime() != null && dto.getShiftToDate() != null) {
            List<DoctorScheduleSlot> targets = slotRepository.findByDoctorIdAndScheduleDateOrderBySlotIndexAsc(
                    dto.getShiftToDoctorId(), dto.getShiftToDate());
            targetSlot = targets.stream()
                    .filter(t -> t.getSlotTime().equals(dto.getShiftToSlotTime()))
                    .findFirst()
                    .orElse(null);
            if (targetSlot == null) {
                throw new BusinessException("目标排班时段不存在");
            }
            if (targetSlot.getStatus() != ScheduleSlotStatus.NORMAL.getCode()) {
                throw new BusinessException("目标排班时段不可用");
            }
        }

        slot.setStatus(ScheduleSlotStatus.SHIFTED.getCode());
        slot.setShiftToDoctorId(dto.getShiftToDoctorId());
        if (targetSlot != null) {
            slot.setShiftToSlotId(targetSlot.getId());
        }
        slot.setOperatorId(dto.getOperatorId());
        slotRepository.save(slot);

        ScheduleChangeLog log = new ScheduleChangeLog();
        log.setScheduleSlotId(slot.getId());
        log.setChangeType(ChangeType.SHIFT.name());
        log.setOldValue("医生:" + slot.getDoctorId() + ",日期:" + slot.getScheduleDate() + ",时段:" + slot.getSlotTime());
        String shiftInfo = "医生:" + dto.getShiftToDoctorId();
        if (dto.getShiftToDate() != null) {
            shiftInfo += ",日期:" + dto.getShiftToDate();
        }
        if (dto.getShiftToSlotTime() != null) {
            shiftInfo += ",时段:" + dto.getShiftToSlotTime();
        }
        log.setNewValue(shiftInfo);
        log.setOperatorId(dto.getOperatorId());
        changeLogRepository.save(log);

        if (dto.isAutoReschedule() && targetSlot != null) {
            List<Appointment> appointments = appointmentRepository
                    .findByScheduleSlotIdAndStatusNot(slot.getId(), AppointmentStatus.CANCELLED.getCode());
            for (Appointment appt : appointments) {
                Patient patient = patientRepository.findById(appt.getPatientId()).orElse(null);
                String patientName = patient != null ? patient.getName() : "患者";
                appt.setScheduleSlotId(targetSlot.getId());
                appt.setDoctorId(targetSlot.getDoctorId());
                appt.setAppointmentDate(targetSlot.getScheduleDate());
                appt.setTimeSlotStr(targetSlot.getSlotTime());
                appointmentRepository.save(appt);
                createNotification(appt.getPatientId(), patientName,
                        NotificationType.APPOINTMENT_RESCHEDULED,
                        "预约改约通知",
                        "您的预约已改约至：" + targetSlot.getScheduleDate() + " " + targetSlot.getSlotTime(),
                        appt.getId(), targetSlot.getId());
            }
            log.setNotifyStatus(1);
            changeLogRepository.save(log);
        } else if (!dto.isAutoReschedule()) {
            List<Appointment> appointments = appointmentRepository
                    .findByScheduleSlotIdAndStatusNot(slot.getId(), AppointmentStatus.CANCELLED.getCode());
            for (Appointment appt : appointments) {
                Patient patient = patientRepository.findById(appt.getPatientId()).orElse(null);
                String patientName = patient != null ? patient.getName() : "患者";
                createNotification(appt.getPatientId(), patientName,
                        NotificationType.APPOINTMENT_RESCHEDULED,
                        "预约调班通知",
                        "您预约的" + slot.getScheduleDate() + " " + slot.getSlotTime() + "已调班。",
                        appt.getId(), targetSlot != null ? targetSlot.getId() : null);
            }
        }
        return convertToSlotVO(slot);
    }

    @Override
    @Transactional
    public void deleteSchedule(Long scheduleId, Long operatorId) {
        DoctorScheduleSlot slot = slotRepository.findById(scheduleId)
                .orElseThrow(() -> new BusinessException("排班记录不存在"));
        if (slot.getBookedCount() != null && slot.getBookedCount() > 0) {
            throw new BusinessException("该时段已有预约，无法删除");
        }
        slotRepository.delete(slot);
    }

    @Override
    @Transactional
    public void deleteDaySchedule(Long doctorId, LocalDate date, Long operatorId) {
        List<DoctorScheduleSlot> slots = slotRepository.findByDoctorIdAndScheduleDateOrderBySlotIndexAsc(doctorId, date);
        for (DoctorScheduleSlot slot : slots) {
            if (slot.getBookedCount() != null && slot.getBookedCount() > 0) {
                throw new BusinessException("日期 " + date + " 的时段 " + slot.getSlotTime() + " 已有预约，无法删除");
            }
        }
        slotRepository.deleteAll(slots);
    }

    @Override
    public String[] getAllSlotTimes() {
        return ScheduleSlotConstants.getAllSlotTimes();
    }

    @Override
    public List<ScheduleTemplateVO> getTemplates(Long doctorId) {
        List<ScheduleTemplate> templates = templateRepository.findByDoctorId(doctorId);
        return templates.stream().map(this::convertToTemplateVO).collect(Collectors.toList());
    }

    @Override
    @Transactional
    public ScheduleTemplateVO createTemplate(ScheduleTemplateCreateDTO dto) {
        if (dto.getSlotTimes() == null || dto.getSlotTimes().isEmpty()) {
            throw new BusinessException("请选择排班时段");
        }
        ScheduleTemplate template = new ScheduleTemplate();
        template.setDoctorId(dto.getDoctorId());
        template.setTemplateName(dto.getTemplateName());
        template.setDayOfWeek(dto.getDayOfWeek());
        template.setSlotTimes(String.join(",", dto.getSlotTimes()));
        template.setMaxPatientsPerSlot(dto.getMaxPatientsPerSlot() != null ? dto.getMaxPatientsPerSlot() : 1);
        template = templateRepository.save(template);
        return convertToTemplateVO(template);
    }

    @Override
    @Transactional
    public List<ScheduleSlotVO> applyTemplate(Long templateId, ScheduleTemplateApplyDTO dto) {
        ScheduleTemplate template = templateRepository.findById(templateId)
                .orElseThrow(() -> new BusinessException("排班模板不存在"));
        if (!template.getDoctorId().equals(dto.getDoctorId())) {
            throw new BusinessException("无权应用此模板");
        }
        DayOfWeek templateDayOfWeek = DayOfWeek.of(template.getDayOfWeek());
        List<String> slotTimes = Arrays.asList(template.getSlotTimes().split(","));
        List<DoctorScheduleSlot> created = new ArrayList<>();
        for (LocalDate targetDate : dto.getTargetDates()) {
            if (targetDate.getDayOfWeek() != templateDayOfWeek) {
                continue;
            }
            for (String slotTime : slotTimes) {
                long count = slotRepository.countByDoctorIdAndScheduleDateAndSlotTime(
                        dto.getDoctorId(), targetDate, slotTime);
                if (count > 0) {
                    continue;
                }
                DoctorScheduleSlot slot = new DoctorScheduleSlot();
                slot.setDoctorId(dto.getDoctorId());
                slot.setScheduleDate(targetDate);
                slot.setSlotTime(slotTime);
                slot.setSlotIndex(ScheduleSlotConstants.getSlotIndex(slotTime));
                slot.setMaxPatients(template.getMaxPatientsPerSlot());
                slot.setBookedCount(0);
                slot.setStatus(ScheduleSlotStatus.NORMAL.getCode());
                slot.setOperatorId(dto.getOperatorId());
                created.add(slotRepository.save(slot));
            }
        }
        return created.stream().map(this::convertToSlotVO).collect(Collectors.toList());
    }

    @Override
    @Transactional
    public void deleteTemplate(Long id) {
        if (!templateRepository.existsById(id)) {
            throw new BusinessException("排班模板不存在");
        }
        templateRepository.deleteById(id);
    }

    @Override
    @Transactional
    public Appointment bookAppointment(AppointmentBookDTO dto) {
        String lockKey = "schedule:slot:" + dto.getScheduleSlotId();
        boolean locked = redisDistributedLock.tryLock(lockKey, LOCK_EXPIRE_SECONDS);
        if (!locked) {
            throw new BusinessException("预约繁忙，请重试");
        }
        try {
            DoctorScheduleSlot slot = slotRepository.findById(dto.getScheduleSlotId())
                    .orElseThrow(() -> new BusinessException("排班时段不存在"));
            if (slot.getStatus() != ScheduleSlotStatus.NORMAL.getCode()) {
                throw new BusinessException("该时段不可预约");
            }
            int remaining = slot.getMaxPatients() - slot.getBookedCount();
            if (remaining <= 0) {
                throw new BusinessException("该时段号源已满");
            }
            List<Appointment> duplicate = appointmentRepository.findByPatientIdAndScheduleSlotIdAndStatusNot(
                    dto.getPatientId(), dto.getScheduleSlotId(), AppointmentStatus.CANCELLED.getCode());
            if (!duplicate.isEmpty()) {
                throw new BusinessException("您已预约该时段");
            }
            Appointment appointment = new Appointment();
            appointment.setPatientId(dto.getPatientId());
            appointment.setDoctorId(slot.getDoctorId());
            appointment.setAppointmentDate(slot.getScheduleDate());
            appointment.setTimeSlotStr(slot.getSlotTime());
            appointment.setScheduleSlotId(slot.getId());
            appointment.setDescription(dto.getDescription());
            appointment.setStatus(AppointmentStatus.CONFIRMED.getCode());
            appointment = appointmentRepository.save(appointment);

            int updated = slotRepository.incrementBookedCount(slot.getId());
            if (updated == 0) {
                throw new BusinessException("号源不足");
            }

            createNotification(dto.getPatientId(), dto.getPatientName(),
                    NotificationType.APPOINTMENT_REMINDER,
                    "预约成功",
                    "您已成功预约：" + slot.getScheduleDate() + " " + slot.getSlotTime(),
                    appointment.getId(), slot.getId());

            return appointment;
        } finally {
            redisDistributedLock.unlock(lockKey);
        }
    }

    @Override
    @Transactional
    public Appointment rescheduleAppointment(AppointmentRescheduleDTO dto) {
        Appointment oldAppt = appointmentRepository.findById(dto.getAppointmentId())
                .orElseThrow(() -> new BusinessException("预约记录不存在"));
        if (oldAppt.getStatus() == AppointmentStatus.CANCELLED.getCode()
                || oldAppt.getStatus() == AppointmentStatus.COMPLETED.getCode()) {
            throw new BusinessException("该预约状态不允许改约");
        }
        Long oldSlotId = oldAppt.getScheduleSlotId();
        Long newSlotId = dto.getNewScheduleSlotId();

        Long firstLockId = Math.min(oldSlotId, newSlotId);
        Long secondLockId = Math.max(oldSlotId, newSlotId);
        String lockKey1 = "schedule:slot:" + firstLockId;
        String lockKey2 = "schedule:slot:" + secondLockId;

        boolean locked1 = redisDistributedLock.tryLock(lockKey1, LOCK_EXPIRE_SECONDS);
        if (!locked1) {
            throw new BusinessException("改约繁忙，请重试");
        }
        boolean locked2 = redisDistributedLock.tryLock(lockKey2, LOCK_EXPIRE_SECONDS);
        if (!locked2) {
            redisDistributedLock.unlock(lockKey1);
            throw new BusinessException("改约繁忙，请重试");
        }
        try {
            DoctorScheduleSlot newSlot = slotRepository.findById(newSlotId)
                    .orElseThrow(() -> new BusinessException("新排班时段不存在"));
            if (newSlot.getStatus() != ScheduleSlotStatus.NORMAL.getCode()) {
                throw new BusinessException("新时段不可预约");
            }
            int newRemaining = newSlot.getMaxPatients() - newSlot.getBookedCount();
            if (newRemaining <= 0) {
                throw new BusinessException("新时段号源已满");
            }
            List<Appointment> duplicate = appointmentRepository.findByPatientIdAndScheduleSlotIdAndStatusNot(
                    oldAppt.getPatientId(), newSlotId, AppointmentStatus.CANCELLED.getCode());
            if (!duplicate.isEmpty() && !duplicate.get(0).getId().equals(oldAppt.getId())) {
                throw new BusinessException("您已预约新时段");
            }

            if (oldSlotId != null) {
                slotRepository.decrementBookedCount(oldSlotId);
            }

            int updated = slotRepository.incrementBookedCount(newSlotId);
            if (updated == 0) {
                if (oldSlotId != null) {
                    slotRepository.incrementBookedCount(oldSlotId);
                }
                throw new BusinessException("号源不足");
            }

            oldAppt.setScheduleSlotId(newSlot.getId());
            oldAppt.setDoctorId(newSlot.getDoctorId());
            oldAppt.setAppointmentDate(newSlot.getScheduleDate());
            oldAppt.setTimeSlotStr(newSlot.getSlotTime());
            oldAppt = appointmentRepository.save(oldAppt);

            Patient patient = patientRepository.findById(oldAppt.getPatientId()).orElse(null);
            String patientName = patient != null ? patient.getName() : "患者";
            createNotification(oldAppt.getPatientId(), patientName,
                    NotificationType.APPOINTMENT_RESCHEDULED,
                    "改约成功",
                    "您的预约已改约至：" + newSlot.getScheduleDate() + " " + newSlot.getSlotTime(),
                    oldAppt.getId(), newSlot.getId());

            return oldAppt;
        } finally {
            redisDistributedLock.unlock(lockKey2);
            redisDistributedLock.unlock(lockKey1);
        }
    }

    @Override
    @Transactional
    public Appointment cancelAppointmentByPatient(Long appointmentId, Long patientId) {
        Appointment appointment = appointmentRepository.findById(appointmentId)
                .orElseThrow(() -> new BusinessException("预约记录不存在"));
        if (!appointment.getPatientId().equals(patientId)) {
            throw new BusinessException("无权取消此预约");
        }
        if (appointment.getStatus() != AppointmentStatus.PENDING.getCode()
                && appointment.getStatus() != AppointmentStatus.CONFIRMED.getCode()) {
            throw new BusinessException("预约状态不允许取消");
        }
        Long slotId = appointment.getScheduleSlotId();
        if (slotId != null) {
            String lockKey = "schedule:slot:" + slotId;
            boolean locked = redisDistributedLock.tryLock(lockKey, LOCK_EXPIRE_SECONDS);
            if (locked) {
                try {
                    slotRepository.decrementBookedCount(slotId);
                } finally {
                    redisDistributedLock.unlock(lockKey);
                }
            }
        }
        appointment.setStatus(AppointmentStatus.CANCELLED.getCode());
        return appointmentRepository.save(appointment);
    }

    @Override
    @Transactional
    public void autoRescheduleForSuspendedSlot(Long suspendedSlotId) {
        DoctorScheduleSlot suspendedSlot = slotRepository.findById(suspendedSlotId)
                .orElseThrow(() -> new BusinessException("排班时段不存在"));
        List<Appointment> appointments = appointmentRepository
                .findByScheduleSlotIdAndStatusNot(suspendedSlotId, AppointmentStatus.CANCELLED.getCode());
        if (appointments.isEmpty()) {
            return;
        }
        LocalDate today = LocalDate.now();
        LocalDate endDate = today.plusDays(7);
        List<DoctorScheduleSlot> availableSlots = slotRepository
                .findByDoctorIdAndScheduleDateBetweenOrderByScheduleDateAscSlotIndexAsc(
                        suspendedSlot.getDoctorId(), today, endDate)
                .stream()
                .filter(s -> s.getStatus() == ScheduleSlotStatus.NORMAL.getCode())
                .filter(s -> s.getMaxPatients() - s.getBookedCount() > 0)
                .collect(Collectors.toList());

        LocalTime suspendedTime = LocalTime.parse(suspendedSlot.getSlotTime(), ScheduleSlotConstants.SLOT_FORMATTER);

        for (Appointment appt : appointments) {
            Patient patient = patientRepository.findById(appt.getPatientId()).orElse(null);
            String patientName = patient != null ? patient.getName() : "患者";

            DoctorScheduleSlot bestSlot = availableSlots.stream()
                    .min(Comparator.comparingInt(s -> {
                        LocalTime slotTime = LocalTime.parse(s.getSlotTime(), ScheduleSlotConstants.SLOT_FORMATTER);
                        int timeDiff = Math.abs((int) (slotTime.toSecondOfDay() - suspendedTime.toSecondOfDay()));
                        int dayDiff = s.getScheduleDate().compareTo(LocalDate.now()) * 24 * 60;
                        return dayDiff + timeDiff / 60;
                    }))
                    .orElse(null);

            if (bestSlot != null) {
                String lockKey = "schedule:slot:" + bestSlot.getId();
                boolean locked = redisDistributedLock.tryLock(lockKey, LOCK_EXPIRE_SECONDS);
                if (locked) {
                    try {
                        if (bestSlot.getMaxPatients() - bestSlot.getBookedCount() > 0) {
                            slotRepository.incrementBookedCount(bestSlot.getId());
                            appt.setStatus(AppointmentStatus.CANCELLED.getCode());
                            appointmentRepository.save(appt);

                            Appointment newAppt = new Appointment();
                            newAppt.setPatientId(appt.getPatientId());
                            newAppt.setDoctorId(bestSlot.getDoctorId());
                            newAppt.setAppointmentDate(bestSlot.getScheduleDate());
                            newAppt.setTimeSlotStr(bestSlot.getSlotTime());
                            newAppt.setScheduleSlotId(bestSlot.getId());
                            newAppt.setDescription(appt.getDescription());
                            newAppt.setStatus(AppointmentStatus.CONFIRMED.getCode());
                            appointmentRepository.save(newAppt);

                            createNotification(appt.getPatientId(), patientName,
                                    NotificationType.APPOINTMENT_RESCHEDULED,
                                    "自动改约通知",
                                    "由于原时段停诊，您的预约已自动改约至：" + bestSlot.getScheduleDate() + " " + bestSlot.getSlotTime(),
                                    newAppt.getId(), bestSlot.getId());

                            availableSlots = availableSlots.stream()
                                    .filter(s -> !s.getId().equals(bestSlot.getId())
                                            || (s.getMaxPatients() - (s.getBookedCount() + 1) > 0))
                                    .collect(Collectors.toList());
                            continue;
                        }
                    } finally {
                        redisDistributedLock.unlock(lockKey);
                    }
                }
            }

            appt.setStatus(AppointmentStatus.CANCELLED.getCode());
            appointmentRepository.save(appt);
            createNotification(appt.getPatientId(), patientName,
                    NotificationType.SCHEDULE_SUSPENDED,
                    "停诊通知",
                    "您预约的" + suspendedSlot.getScheduleDate() + " " + suspendedSlot.getSlotTime() + "已停诊，暂时无法自动改约，请手动预约其他时段。",
                    appt.getId(), suspendedSlotId);
        }
        changeLogRepository.findByScheduleSlotIdOrderByCreateTimeDesc(suspendedSlotId).stream()
                .findFirst()
                .ifPresent(log -> {
                    log.setNotifyStatus(1);
                    changeLogRepository.save(log);
                });
    }

    @Override
    public List<NotificationVO> getPatientNotifications(Long patientId) {
        List<PatientNotification> notifications = notificationRepository.findByPatientIdOrderByCreateTimeDesc(patientId);
        return notifications.stream().map(this::convertToNotificationVO).collect(Collectors.toList());
    }

    @Override
    @Transactional
    public void markNotificationAsRead(Long id, Long patientId) {
        notificationRepository.markAsRead(id, patientId);
    }

    @Override
    public Appointment getAppointmentDetail(Long id) {
        return appointmentRepository.findById(id)
                .orElseThrow(() -> new BusinessException("预约记录不存在"));
    }

    private ScheduleSlotVO convertToSlotVO(DoctorScheduleSlot slot) {
        ScheduleSlotVO vo = new ScheduleSlotVO();
        BeanUtils.copyProperties(slot, vo);
        vo.setStatus(ScheduleSlotStatus.fromCode(slot.getStatus()).name());
        vo.setRemaining(slot.getMaxPatients() - slot.getBookedCount());
        vo.setBookedCount(slot.getBookedCount());

        User doctorUser = getDoctorUser(slot.getDoctorId());
        if (doctorUser != null) {
            vo.setDoctorName(doctorUser.getRealName());
        } else {
            vo.setDoctorName("医生" + slot.getDoctorId());
        }

        if (slot.getShiftToSlotId() != null) {
            DoctorScheduleSlot shiftTo = slotRepository.findById(slot.getShiftToSlotId()).orElse(null);
            if (shiftTo != null) {
                vo.setShiftToDate(shiftTo.getScheduleDate());
                vo.setShiftToSlotTime(shiftTo.getSlotTime());
                User shiftToUser = getDoctorUser(shiftTo.getDoctorId());
                vo.setShiftToDoctorName(shiftToUser != null ? shiftToUser.getRealName() : "医生" + shiftTo.getDoctorId());
            }
        } else if (slot.getShiftToDoctorId() != null) {
            User shiftToUser = getDoctorUser(slot.getShiftToDoctorId());
            vo.setShiftToDoctorName(shiftToUser != null ? shiftToUser.getRealName() : "医生" + slot.getShiftToDoctorId());
        }
        return vo;
    }

    private User getDoctorUser(Long doctorId) {
        if (doctorId == null) {
            return null;
        }
        return doctorRepository.findById(doctorId)
                .flatMap(d -> userRepository.findById(d.getUserId()))
                .orElse(null);
    }

    private ScheduleTemplateVO convertToTemplateVO(ScheduleTemplate template) {
        ScheduleTemplateVO vo = new ScheduleTemplateVO();
        BeanUtils.copyProperties(template, vo);
        vo.setDayOfWeekLabel(DAY_OF_WEEK_LABELS[template.getDayOfWeek()]);
        vo.setSlotTimes(Arrays.asList(template.getSlotTimes().split(",")));
        return vo;
    }

    private NotificationVO convertToNotificationVO(PatientNotification notification) {
        NotificationVO vo = new NotificationVO();
        BeanUtils.copyProperties(notification, vo);
        try {
            vo.setTypeText(NotificationType.valueOf(notification.getType()).getDesc());
        } catch (Exception e) {
            vo.setTypeText(notification.getType());
        }
        vo.setStatusText(notification.getStatus() == 1 ? "READ" : "UNREAD");
        return vo;
    }

    private void createNotification(Long patientId, String patientName, NotificationType type,
                                     String title, String content, Long appointmentId, Long relatedSlotId) {
        PatientNotification notification = new PatientNotification();
        notification.setPatientId(patientId);
        notification.setPatientName(patientName);
        notification.setType(type.name());
        notification.setTitle(title);
        notification.setContent(content);
        notification.setStatus(0);
        notification.setAppointmentId(appointmentId);
        notification.setRelatedSlotId(relatedSlotId);
        notificationRepository.save(notification);
    }
}
