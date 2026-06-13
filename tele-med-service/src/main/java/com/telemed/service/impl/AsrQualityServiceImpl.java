package com.telemed.service.impl;

import com.telemed.common.dto.asr.NlpAnalysisRequest;
import com.telemed.common.dto.asr.NlpAnalysisResponse;
import com.telemed.common.exception.BusinessException;
import com.telemed.common.vo.asr.AsrQualityIssueVO;
import com.telemed.common.vo.asr.AsrQualityReportVO;
import com.telemed.model.entity.AsrQualityIssue;
import com.telemed.model.entity.AsrQualityReport;
import com.telemed.model.entity.Consultation;
import com.telemed.model.entity.ConsultationAsrTranscript;
import com.telemed.model.entity.Doctor;
import com.telemed.model.entity.Patient;
import com.telemed.model.repository.AsrQualityIssueRepository;
import com.telemed.model.repository.AsrQualityReportRepository;
import com.telemed.model.repository.ConsultationAsrTranscriptRepository;
import com.telemed.model.repository.ConsultationRepository;
import com.telemed.model.repository.DoctorRepository;
import com.telemed.model.repository.PatientRepository;
import com.telemed.service.AsrQualityService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class AsrQualityServiceImpl implements AsrQualityService {

    private final AsrQualityReportRepository qualityReportRepository;
    private final AsrQualityIssueRepository qualityIssueRepository;
    private final ConsultationAsrTranscriptRepository transcriptRepository;
    private final ConsultationRepository consultationRepository;
    private final DoctorRepository doctorRepository;
    private final PatientRepository patientRepository;
    private final ObjectMapper objectMapper;

    @Value("${nlp.mock.enabled:true}")
    private boolean mockEnabled;

    @Value("${nlp.model.version:mock-nlp-v1.0}")
    private String nlpModelVersion;

    private static final DateTimeFormatter DTF = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private static final List<String> STANDARD_KEY_INDICATORS = Arrays.asList(
            "主诉询问", "现病史", "既往史", "过敏史", "体格检查",
            "鉴别诊断", "治疗方案", "用药说明", "注意事项", "复诊建议"
    );

    private static final Map<String, Set<String>> KEY_INDICATOR_KEYWORDS = new HashMap<>();
    static {
        KEY_INDICATOR_KEYWORDS.put("主诉询问", new HashSet<>(Arrays.asList("哪里不舒服", "怎么不舒服", "哪里难受", "什么症状", "什么问题")));
        KEY_INDICATOR_KEYWORDS.put("现病史", new HashSet<>(Arrays.asList("多久了", "多长时间", "什么时候开始", "加重", "缓解", "伴随症状")));
        KEY_INDICATOR_KEYWORDS.put("既往史", new HashSet<>(Arrays.asList("以前有没有", "既往史", "之前得过", "有什么病史", "以前有过")));
        KEY_INDICATOR_KEYWORDS.put("过敏史", new HashSet<>(Arrays.asList("过敏", "药物过敏", "有没有过敏", "对什么过敏")));
        KEY_INDICATOR_KEYWORDS.put("体格检查", new HashSet<>(Arrays.asList("体格检查", "给您检查一下", "做个检查", "放松", "查体")));
        KEY_INDICATOR_KEYWORDS.put("鉴别诊断", new HashSet<>(Arrays.asList("可能是", "初步判断", "考虑是", "需要排除", "鉴别")));
        KEY_INDICATOR_KEYWORDS.put("治疗方案", new HashSet<>(Arrays.asList("建议您", "先做个", "辅助检查", "治疗方案", "住院", "门诊")));
        KEY_INDICATOR_KEYWORDS.put("用药说明", new HashSet<>(Arrays.asList("服药", "吃药", "用药", "按说明吃", "按时服药", "剂量")));
        KEY_INDICATOR_KEYWORDS.put("注意事项", new HashSet<>(Arrays.asList("注意休息", "饮食清淡", "别吃", "不要", "避免", "注意事项")));
        KEY_INDICATOR_KEYWORDS.put("复诊建议", new HashSet<>(Arrays.asList("复诊", "复查", "下次来", "一周后", "症状加重", "及时就医")));
    }

    private static final List<String> SAFETY_RISK_PATTERNS = Arrays.asList(
            "过敏未询问", "用药禁忌未说明", "剂量不明确",
            "禁忌症未提及", "高危症状未处理"
    );

    @Override
    public NlpAnalysisResponse analyzeTranscript(NlpAnalysisRequest request) {
        if (mockEnabled) {
            return runMockNlpAnalysis(request);
        }
        throw new BusinessException("NLP服务未配置，请开启mock模式");
    }

    @Override
    @Transactional
    public AsrQualityReportVO generateQualityReport(Long consultationId, NlpAnalysisResponse nlpResponse) {
        log.info("生成质检报告，consultationId: {}", consultationId);

        Consultation consultation = consultationRepository.findById(consultationId)
                .orElseThrow(() -> new BusinessException("会诊不存在"));

        AsrQualityReport existing = qualityReportRepository.findByConsultationId(consultationId).orElse(null);
        AsrQualityReport report;
        if (existing != null) {
            report = existing;
            qualityIssueRepository.deleteByReportId(existing.getId());
        } else {
            report = new AsrQualityReport();
        }

        Doctor doctor = doctorRepository.findById(consultation.getDoctorId()).orElse(null);
        Patient patient = patientRepository.findById(consultation.getPatientId()).orElse(null);

        report.setConsultationId(consultationId);
        report.setConsultationNo(consultation.getConsultationNo());
        report.setDoctorId(consultation.getDoctorId());
        report.setPatientId(consultation.getPatientId());
        if (doctor != null) {
            report.setDoctorName(doctor.getTitle() != null ? doctor.getTitle() : "医生");
        }
        if (patient != null) {
            report.setPatientName(patient.getName());
        }

        List<ConsultationAsrTranscript> transcripts = transcriptRepository
                .findByConsultationIdOrderByCreateTimeAsc(consultationId);

        StringBuilder sb = new StringBuilder();
        int doctorTalk = 0, patientTalk = 0;
        for (ConsultationAsrTranscript t : transcripts) {
            if ("DOCTOR".equals(t.getSpeakerRole())) {
                sb.append("【医生】").append(t.getTranscriptText()).append("\n");
                if (t.getDurationSeconds() != null) doctorTalk += t.getDurationSeconds();
            } else {
                sb.append("【患者】").append(t.getTranscriptText()).append("\n");
                if (t.getDurationSeconds() != null) patientTalk += t.getDurationSeconds();
            }
        }

        report.setFullTranscript(sb.toString());
        report.setTotalDuration(doctorTalk + patientTalk);
        report.setDoctorTalkTime(doctorTalk);
        report.setPatientTalkTime(patientTalk);
        report.setKeyIndicatorScore(nlpResponse.getKeyIndicatorScore());
        report.setSafetyScore(nlpResponse.getSafetyScore());
        report.setOverallScore(nlpResponse.getOverallScore());
        report.setSummary(nlpResponse.getSummary());
        report.setRecommendations(nlpResponse.getRecommendations());
        report.setSafetyRisksDetected(nlpResponse.getSafetyRisksDetected());
        report.setAsrProvider(nlpResponse.getNlpModelVersion() != null ? nlpResponse.getNlpModelVersion() : "MOCK");
        report.setStatus("COMPLETED");

        try {
            Map<String, Object> meta = new HashMap<>();
            meta.put("mentioned", nlpResponse.getMentionedKeyIndicators());
            meta.put("missing", nlpResponse.getMissingKeyIndicators());
            meta.put("modelVersion", nlpResponse.getNlpModelVersion());
            report.setNlpMetadata(objectMapper.writeValueAsString(meta));
        } catch (JsonProcessingException e) {
            log.warn("序列化NLP metadata失败", e);
        }

        AsrQualityReport saved = qualityReportRepository.save(report);

        List<AsrQualityIssue> issues = new ArrayList<>();
        for (NlpAnalysisResponse.QualityIssueDetail d : nlpResponse.getIssues()) {
            AsrQualityIssue issue = new AsrQualityIssue();
            issue.setReportId(saved.getId());
            issue.setConsultationId(consultationId);
            issue.setIssueType(d.getIssueType());
            issue.setSeverity(d.getSeverity());
            issue.setDescription(d.getDescription());
            issue.setRelatedText(d.getRelatedText());
            issue.setSuggestion(d.getSuggestion());
            issue.setTimelineStart(d.getTimelineStart());
            issue.setTimelineEnd(d.getTimelineEnd());
            issue.setResolved(false);
            issues.add(issue);
        }
        qualityIssueRepository.saveAll(issues);

        log.info("质检报告已生成，reportId: {}, overallScore: {}, 问题数: {}",
                saved.getId(), saved.getOverallScore(), issues.size());

        return convertReportToVO(saved, transcripts, nlpResponse);
    }

    @Override
    public AsrQualityReportVO getQualityReportByConsultationId(Long consultationId) {
        AsrQualityReport report = qualityReportRepository.findByConsultationId(consultationId)
                .orElseThrow(() -> new BusinessException("质检报告不存在"));
        List<ConsultationAsrTranscript> transcripts = transcriptRepository
                .findByConsultationIdOrderByCreateTimeAsc(consultationId);
        NlpAnalysisResponse nlp = parseNlpMetadata(report);
        return convertReportToVO(report, transcripts, nlp);
    }

    @Override
    public List<AsrQualityReportVO> getDoctorQualityReports(Long doctorId) {
        return qualityReportRepository.findScoredByDoctorId(doctorId).stream()
                .map(r -> {
                    NlpAnalysisResponse nlp = parseNlpMetadata(r);
                    return convertReportToVO(r, null, nlp);
                })
                .sorted(Comparator.comparing(AsrQualityReportVO::getCreateTime).reversed())
                .collect(Collectors.toList());
    }

    @Override
    public List<AsrQualityReportVO> getPatientQualityReports(Long patientId) {
        return qualityReportRepository.findByPatientIdOrderByCreateTimeDesc(patientId).stream()
                .map(r -> convertReportToVO(r, null, parseNlpMetadata(r)))
                .collect(Collectors.toList());
    }

    @Override
    public List<AsrQualityIssueVO> getQualityIssues(Long reportId) {
        return qualityIssueRepository.findByReportIdOrderBySeverityDesc(reportId).stream()
                .map(this::convertIssueToVO)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public AsrQualityIssueVO resolveIssue(Long issueId, Long operatorId) {
        AsrQualityIssue issue = qualityIssueRepository.findById(issueId)
                .orElseThrow(() -> new BusinessException("问题不存在"));
        issue.setResolved(true);
        return convertIssueToVO(qualityIssueRepository.save(issue));
    }

    private NlpAnalysisResponse runMockNlpAnalysis(NlpAnalysisRequest request) {
        List<NlpAnalysisRequest.SegmentUtterance> doctorUtterances = request.getUtterances().stream()
                .filter(u -> "DOCTOR".equals(u.getSpeakerRole()))
                .toList();

        String doctorText = doctorUtterances.stream()
                .map(NlpAnalysisRequest.SegmentUtterance::getText)
                .collect(Collectors.joining(" "));

        List<String> mentioned = new ArrayList<>();
        List<String> missing = new ArrayList<>();

        for (Map.Entry<String, Set<String>> entry : KEY_INDICATOR_KEYWORDS.entrySet()) {
            String indicator = entry.getKey();
            Set<String> keywords = entry.getValue();
            boolean found = keywords.stream().anyMatch(kw ->
                    doctorText.contains(kw) || doctorText.contains(kw.substring(0, Math.min(2, kw.length()))));
            if (found) mentioned.add(indicator);
            else missing.add(indicator);
        }

        if (mentioned.size() < 4) mentioned.add(STANDARD_KEY_INDICATORS.get(0));

        int keyScore = Math.min(100, mentioned.size() * 10);
        boolean safetyRisks = !mentioned.contains("过敏史") || !mentioned.contains("用药说明");
        int safetyScore = safetyRisks ? 70 : 95;
        int overall = (keyScore * 60 + safetyScore * 40) / 100;

        List<NlpAnalysisResponse.QualityIssueDetail> issues = new ArrayList<>();

        for (int i = 0; i < missing.size() && i < 3; i++) {
            String miss = missing.get(i);
            issues.add(NlpAnalysisResponse.QualityIssueDetail.builder()
                    .issueType("MISSING_KEY_INDICATOR")
                    .severity("MEDIUM")
                    .description("未提及关键诊疗指标：" + miss)
                    .relatedText("")
                    .suggestion("建议在下一次问诊中明确询问/说明「" + miss + "」相关内容")
                    .timelineStart(0)
                    .timelineEnd(0)
                    .build());
        }

        if (safetyRisks) {
            if (!mentioned.contains("过敏史")) {
                issues.add(NlpAnalysisResponse.QualityIssueDetail.builder()
                        .issueType("SAFETY_RISK")
                        .severity("HIGH")
                        .description("安全隐患：未询问患者药物过敏史")
                        .relatedText("")
                        .suggestion("开具处方前必须明确患者过敏史，以避免过敏反应风险")
                        .build());
            }
            if (!mentioned.contains("用药说明")) {
                issues.add(NlpAnalysisResponse.QualityIssueDetail.builder()
                        .issueType("SAFETY_RISK")
                        .severity("HIGH")
                        .description("安全隐患：未明确说明用药剂量与禁忌")
                        .relatedText("")
                        .suggestion("开药时必须明确说明用法用量、禁忌症和不良反应")
                        .build());
            }
        }

        StringBuilder summary = new StringBuilder();
        summary.append("本次会诊共检测到").append(request.getUtterances().size()).append("段对话。");
        summary.append("关键诊疗指标覆盖率").append(mentioned.size()).append("/").append(STANDARD_KEY_INDICATORS.size()).append("。");
        if (safetyRisks) summary.append("检测到潜在安全隐患，请关注。");
        else summary.append("未发现明显安全隐患。");

        StringBuilder recommendations = new StringBuilder();
        recommendations.append("综合评分 ").append(overall).append(" 分。");
        if (!missing.isEmpty()) {
            recommendations.append("建议加强以下方面的规范：").append(String.join("、", missing)).append("。");
        }
        recommendations.append("请重视安全用药相关规范。");

        return NlpAnalysisResponse.builder()
                .consultationId(request.getConsultationId())
                .keyIndicatorScore(keyScore)
                .safetyScore(safetyScore)
                .overallScore(overall)
                .summary(summary.toString())
                .recommendations(recommendations.toString())
                .safetyRisksDetected(safetyRisks)
                .mentionedKeyIndicators(mentioned)
                .missingKeyIndicators(missing)
                .issues(issues)
                .nlpModelVersion(nlpModelVersion)
                .build();
    }

    private NlpAnalysisResponse parseNlpMetadata(AsrQualityReport report) {
        List<String> mentioned = new ArrayList<>();
        List<String> missing = new ArrayList<>();
        String modelVersion = nlpModelVersion;

        if (report.getNlpMetadata() != null && !report.getNlpMetadata().isEmpty()) {
            try {
                Map<String, Object> meta = objectMapper.readValue(report.getNlpMetadata(), Map.class);
                if (meta.get("mentioned") instanceof List) {
                    mentioned = (List<String>) meta.get("mentioned");
                }
                if (meta.get("missing") instanceof List) {
                    missing = (List<String>) meta.get("missing");
                }
                if (meta.get("modelVersion") != null) {
                    modelVersion = meta.get("modelVersion").toString();
                }
            } catch (Exception e) {
                log.warn("解析NLP metadata失败", e);
            }
        }

        return NlpAnalysisResponse.builder()
                .consultationId(report.getConsultationId())
                .keyIndicatorScore(report.getKeyIndicatorScore())
                .safetyScore(report.getSafetyScore())
                .overallScore(report.getOverallScore())
                .summary(report.getSummary())
                .recommendations(report.getRecommendations())
                .safetyRisksDetected(report.getSafetyRisksDetected())
                .mentionedKeyIndicators(mentioned)
                .missingKeyIndicators(missing)
                .issues(new ArrayList<>())
                .nlpModelVersion(modelVersion)
                .build();
    }

    private AsrQualityReportVO convertReportToVO(AsrQualityReport report,
                                                  List<ConsultationAsrTranscript> transcripts,
                                                  NlpAnalysisResponse nlp) {
        AsrQualityReportVO vo = AsrQualityReportVO.builder()
                .id(report.getId())
                .consultationId(report.getConsultationId())
                .consultationNo(report.getConsultationNo())
                .doctorId(report.getDoctorId())
                .doctorName(report.getDoctorName())
                .patientId(report.getPatientId())
                .patientName(report.getPatientName())
                .status(report.getStatus())
                .fullTranscript(report.getFullTranscript())
                .totalDuration(report.getTotalDuration())
                .doctorTalkTime(report.getDoctorTalkTime())
                .patientTalkTime(report.getPatientTalkTime())
                .keyIndicatorScore(report.getKeyIndicatorScore())
                .safetyScore(report.getSafetyScore())
                .overallScore(report.getOverallScore())
                .summary(report.getSummary())
                .recommendations(report.getRecommendations())
                .safetyRisksDetected(report.getSafetyRisksDetected())
                .asrProvider(report.getAsrProvider())
                .mentionedKeyIndicators(nlp.getMentionedKeyIndicators())
                .missingKeyIndicators(nlp.getMissingKeyIndicators())
                .issues(new ArrayList<>())
                .utterances(new ArrayList<>())
                .createTime(report.getCreateTime() != null ? report.getCreateTime().format(DTF) : null)
                .updateTime(report.getUpdateTime() != null ? report.getUpdateTime().format(DTF) : null)
                .build();

        if (report.getId() != null) {
            vo.setIssues(qualityIssueRepository.findByReportIdOrderBySeverityDesc(report.getId())
                    .stream().map(this::convertIssueToVO).collect(Collectors.toList()));
        }

        if (transcripts != null) {
            for (ConsultationAsrTranscript t : transcripts) {
                vo.getUtterances().add(AsrQualityReportVO.TranscriptUtteranceVO.builder()
                        .id(t.getId())
                        .speakerRole(t.getSpeakerRole())
                        .speakerName(t.getSpeakerName())
                        .text(t.getTranscriptText())
                        .duration(t.getDurationSeconds())
                        .createTime(t.getCreateTime() != null ? t.getCreateTime().format(DTF) : null)
                        .build());
            }
        }

        return vo;
    }

    private AsrQualityIssueVO convertIssueToVO(AsrQualityIssue issue) {
        return AsrQualityIssueVO.builder()
                .id(issue.getId())
                .reportId(issue.getReportId())
                .issueType(issue.getIssueType())
                .severity(issue.getSeverity())
                .description(issue.getDescription())
                .relatedText(issue.getRelatedText())
                .suggestion(issue.getSuggestion())
                .timelineStart(issue.getTimelineStart())
                .timelineEnd(issue.getTimelineEnd())
                .resolved(issue.getResolved())
                .createTime(issue.getCreateTime() != null ? issue.getCreateTime().format(DTF) : null)
                .build();
    }
}
