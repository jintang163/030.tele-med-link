package com.telemed.web.controller;

import com.telemed.common.dto.asr.AsrTaskDTO;
import com.telemed.common.result.Result;
import com.telemed.common.vo.asr.AsrQualityIssueVO;
import com.telemed.common.vo.asr.AsrQualityReportVO;
import com.telemed.service.AsrQualityService;
import com.telemed.service.AsrService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/asr-quality")
@RequiredArgsConstructor
public class AsrQualityController {

    private final AsrService asrService;
    private final AsrQualityService asrQualityService;

    @PostMapping("/task/create")
    public Result<Void> createAsrTask(@RequestBody AsrTaskDTO taskDTO) {
        asrService.createAsrTask(taskDTO);
        return Result.ok();
    }

    @PostMapping("/process/{consultationId}")
    public Result<AsrQualityReportVO> processAsrAndQuality(@PathVariable Long consultationId) {
        AsrQualityReportVO vo = asrService.processConsultationAsrAndQuality(consultationId);
        return Result.ok(vo);
    }

    @GetMapping("/report/consultation/{consultationId}")
    public Result<AsrQualityReportVO> getReportByConsultationId(@PathVariable Long consultationId) {
        AsrQualityReportVO vo = asrQualityService.getQualityReportByConsultationId(consultationId);
        return Result.ok(vo);
    }

    @GetMapping("/report/doctor/{doctorId}")
    public Result<List<AsrQualityReportVO>> getDoctorReports(@PathVariable Long doctorId) {
        List<AsrQualityReportVO> list = asrQualityService.getDoctorQualityReports(doctorId);
        return Result.ok(list);
    }

    @GetMapping("/report/patient/{patientId}")
    public Result<List<AsrQualityReportVO>> getPatientReports(@PathVariable Long patientId) {
        List<AsrQualityReportVO> list = asrQualityService.getPatientQualityReports(patientId);
        return Result.ok(list);
    }

    @GetMapping("/report/{reportId}/issues")
    public Result<List<AsrQualityIssueVO>> getReportIssues(@PathVariable Long reportId) {
        List<AsrQualityIssueVO> list = asrQualityService.getQualityIssues(reportId);
        return Result.ok(list);
    }

    @PostMapping("/issue/{issueId}/resolve")
    public Result<AsrQualityIssueVO> resolveIssue(
            @PathVariable Long issueId,
            @RequestParam Long operatorId) {
        AsrQualityIssueVO vo = asrQualityService.resolveIssue(issueId, operatorId);
        return Result.ok(vo);
    }
}
