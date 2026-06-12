package com.telemed.web.controller;

import com.telemed.common.dto.schedule.ScheduleBatchCopyDTO;
import com.telemed.common.dto.schedule.ScheduleCreateDTO;
import com.telemed.common.dto.schedule.ScheduleShiftDTO;
import com.telemed.common.dto.schedule.ScheduleSuspendDTO;
import com.telemed.common.dto.schedule.ScheduleTemplateApplyDTO;
import com.telemed.common.dto.schedule.ScheduleTemplateCreateDTO;
import com.telemed.common.result.Result;
import com.telemed.common.vo.schedule.DailyScheduleVO;
import com.telemed.common.vo.schedule.ScheduleSlotVO;
import com.telemed.common.vo.schedule.ScheduleTemplateVO;
import com.telemed.common.vo.schedule.WeeklyScheduleVO;
import com.telemed.service.ScheduleService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/schedule")
@RequiredArgsConstructor
public class ScheduleController {

    private final ScheduleService scheduleService;

    @GetMapping("/slot-times")
    public Result<String[]> getAllSlotTimes() {
        return Result.ok(scheduleService.getAllSlotTimes());
    }

    @GetMapping("/weekly")
    public Result<WeeklyScheduleVO> getWeeklySchedule(
            @RequestParam Long doctorId,
            @RequestParam @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate weekStart,
            @RequestParam(defaultValue = "false") boolean includeSuspended) {
        return Result.ok(scheduleService.getWeeklySchedule(doctorId, weekStart, includeSuspended));
    }

    @GetMapping("/day")
    public Result<DailyScheduleVO> getDaySchedule(
            @RequestParam Long doctorId,
            @RequestParam @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate date) {
        return Result.ok(scheduleService.getDaySchedule(doctorId, date));
    }

    @PostMapping
    public Result<List<ScheduleSlotVO>> createSchedule(@RequestBody ScheduleCreateDTO dto) {
        return Result.ok(scheduleService.createSchedule(dto));
    }

    @PostMapping("/batch-copy")
    public Result<List<ScheduleSlotVO>> batchCopySchedule(@RequestBody ScheduleBatchCopyDTO dto) {
        return Result.ok(scheduleService.batchCopySchedule(dto));
    }

    @PostMapping("/suspend")
    public Result<ScheduleSlotVO> suspendSchedule(@RequestBody ScheduleSuspendDTO dto) {
        return Result.ok(scheduleService.suspendSchedule(dto));
    }

    @PostMapping("/resume")
    public Result<ScheduleSlotVO> resumeSchedule(
            @RequestParam Long scheduleId,
            @RequestParam Long operatorId) {
        return Result.ok(scheduleService.resumeSchedule(scheduleId, operatorId));
    }

    @PostMapping("/shift")
    public Result<ScheduleSlotVO> shiftSchedule(@RequestBody ScheduleShiftDTO dto) {
        return Result.ok(scheduleService.shiftSchedule(dto));
    }

    @DeleteMapping("/{scheduleId}")
    public Result<Void> deleteSchedule(
            @PathVariable Long scheduleId,
            @RequestParam Long operatorId) {
        scheduleService.deleteSchedule(scheduleId, operatorId);
        return Result.ok();
    }

    @DeleteMapping("/day")
    public Result<Void> deleteDaySchedule(@RequestBody Map<String, Object> body) {
        Long doctorId = Long.valueOf(body.get("doctorId").toString());
        LocalDate date = LocalDate.parse(body.get("date").toString());
        Long operatorId = Long.valueOf(body.get("operatorId").toString());
        scheduleService.deleteDaySchedule(doctorId, date, operatorId);
        return Result.ok();
    }

    @GetMapping("/templates")
    public Result<List<ScheduleTemplateVO>> getTemplates(@RequestParam Long doctorId) {
        return Result.ok(scheduleService.getTemplates(doctorId));
    }

    @PostMapping("/template")
    public Result<ScheduleTemplateVO> createTemplate(@RequestBody ScheduleTemplateCreateDTO dto) {
        return Result.ok(scheduleService.createTemplate(dto));
    }

    @PostMapping("/template/{id}/apply")
    public Result<List<ScheduleSlotVO>> applyTemplate(
            @PathVariable Long id,
            @RequestBody ScheduleTemplateApplyDTO dto) {
        return Result.ok(scheduleService.applyTemplate(id, dto));
    }

    @DeleteMapping("/template/{id}")
    public Result<Void> deleteTemplate(@PathVariable Long id) {
        scheduleService.deleteTemplate(id);
        return Result.ok();
    }
}
