package com.telemed.service;

import com.telemed.common.dto.asr.NlpAnalysisRequest;
import com.telemed.common.dto.asr.NlpAnalysisResponse;
import com.telemed.common.vo.asr.AsrQualityIssueVO;
import com.telemed.common.vo.asr.AsrQualityReportVO;

import java.util.List;

public interface AsrQualityService {

    NlpAnalysisResponse analyzeTranscript(NlpAnalysisRequest request);

    AsrQualityReportVO generateQualityReport(Long consultationId, NlpAnalysisResponse nlpResponse);

    AsrQualityReportVO getQualityReportByConsultationId(Long consultationId);

    List<AsrQualityReportVO> getDoctorQualityReports(Long doctorId);

    List<AsrQualityReportVO> getPatientQualityReports(Long patientId);

    List<AsrQualityIssueVO> getQualityIssues(Long reportId);

    AsrQualityIssueVO resolveIssue(Long issueId, Long operatorId);
}
