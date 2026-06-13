package com.telemed.service.impl;

import com.telemed.common.dto.asr.DiagnosisRequestDTO;
import com.telemed.common.exception.BusinessException;
import com.telemed.common.vo.asr.DiagnosisSuggestionVO;
import com.telemed.model.entity.DiagnosisSuggestion;
import com.telemed.model.entity.KnowledgeDisease;
import com.telemed.model.repository.DiagnosisSuggestionRepository;
import com.telemed.model.repository.KnowledgeDiseaseRepository;
import com.telemed.service.DiagnosisAssistService;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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
public class DiagnosisAssistServiceImpl implements DiagnosisAssistService {

    private final DiagnosisSuggestionRepository suggestionRepository;
    private final KnowledgeDiseaseRepository knowledgeRepository;

    private static final DateTimeFormatter DTF = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private static final List<KnowledgeDisease> SEED_KNOWLEDGE = Arrays.asList(
            buildDisease("上呼吸道感染", "J06.900", "呼吸内科",
                    "感冒,发烧,咳嗽,流涕,咽痛,鼻塞,上感,受凉",
                    "咳嗽、咳痰、鼻塞、流涕、咽痛、发热、头痛、乏力、肌肉酸痛",
                    "咽部充血、扁桃体肿大、体温升高、双肺呼吸音粗",
                    "肺纹理增粗、未见实质性浸润",
                    "血常规、C反应蛋白、胸部X线",
                    "对症治疗、休息、多饮水、必要时抗病毒或抗菌",
                    "下呼吸道感染、过敏性鼻炎、流感"),

            buildDisease("高血压", "I10.x00", "心血管内科",
                    "血压高,头晕,头痛,头胀,眩晕,高血压,降压",
                    "头晕、头胀、头痛、眩晕、颈项板紧、心悸、视物模糊",
                    "血压≥140/90mmHg、心率增快",
                    "左心室肥厚（ECG/超声）",
                    "血压监测、血脂、血糖、肾功能、心电图",
                    "生活方式干预、ACEI/ARB/CCB/利尿剂等降压治疗",
                    "继发性高血压、嗜铬细胞瘤、原发性醛固酮增多症"),

            buildDisease("2型糖尿病", "E11.900", "内分泌科",
                    "血糖高,糖尿病,多饮,多尿,多食,体重下降,口渴",
                    "多饮、多尿、多食、体重下降、乏力、口渴、伤口愈合慢",
                    "空腹血糖≥7.0mmol/L、糖化≥6.5%",
                    "—",
                    "空腹血糖、餐后2h血糖、糖化血红蛋白、C肽、胰岛素",
                    "饮食控制、运动、二甲双胍/磺脲类/胰岛素等",
                    "1型糖尿病、继发性糖尿病、糖耐量异常"),

            buildDisease("急性胃肠炎", "K52.903", "消化内科",
                    "腹痛,腹泻,呕吐,恶心,拉肚子,肠胃,胃痛,反酸,烧心",
                    "腹痛、腹泻、恶心、呕吐、腹胀、反酸、烧心、食欲不振",
                    "腹部压痛、肠鸣音亢进、体温可升高",
                    "—",
                    "血常规、粪便常规+潜血、电解质、淀粉酶",
                    "补液、止泻、止吐、益生菌、必要时抗生素",
                    "急性阑尾炎、急性胰腺炎、细菌性痢疾"),

            buildDisease("偏头痛", "G43.900", "神经内科",
                    "头痛,偏头疼,偏头痛,太阳穴痛,搏动,畏光,恶心",
                    "单侧搏动性头痛、中重度疼痛、畏光畏声、活动后加重、伴恶心呕吐",
                    "神经系统查体正常",
                    "头颅CT/MRI排除器质性病变",
                    "头颅CT/MRI、脑电图、必要时腰椎穿刺",
                    "急性期曲坦类/NSAIDs、预防性β受体阻滞剂/钙通道阻滞剂",
                    "紧张性头痛、丛集性头痛、颅内占位、蛛网膜下腔出血"),

            buildDisease("颈椎病", "M47.821", "骨科,脊柱外科",
                    "颈椎,脖子痛,颈痛,肩痛,手麻,头晕,上肢麻木,颈部僵硬",
                    "颈肩疼痛、颈部活动受限、上肢麻木/放射痛、头晕、视物模糊、恶心",
                    "颈椎压痛、压头试验+、臂丛牵拉试验+、肌力下降",
                    "颈椎退行性变、椎间盘突出、生理曲度变直",
                    "颈椎X线、CT、MRI、肌电图",
                    "物理治疗、颈托、NSAIDs、神经营养、严重时手术",
                    "肩周炎、腕管综合征、胸廓出口综合征、颈椎结核"),

            buildDisease("腰椎间盘突出症", "M51.203", "骨科,脊柱外科",
                    "腰痛,腰椎,腰突,腿麻,坐骨神经,下肢放射痛,腰背痛",
                    "腰痛、下肢放射痛（坐骨神经分布）、麻木、间歇性跛行",
                    "腰椎压痛、直腿抬高试验+、加强试验+、肌力/反射减退",
                    "腰椎间盘突出、硬膜囊受压",
                    "腰椎X线、CT、MRI",
                    "卧床休息、理疗、NSAIDs、神经营养、无效时微创/手术",
                    "腰肌劳损、腰椎管狭窄、腰椎结核、强直性脊柱炎"),

            buildDisease("支气管哮喘", "J45.900", "呼吸内科",
                    "哮喘,喘息,胸闷,气短,喘鸣,哮鸣,呼吸困难,过敏",
                    "反复发作的喘息、胸闷、气短、咳嗽、夜间/凌晨加重",
                    "双肺可闻及哮鸣音、呼气相延长",
                    "发作期过度充气",
                    "肺功能（FEV1、PEF、支气管舒张/激发试验）、过敏原检测",
                    "吸入性糖皮质激素+长效β2激动剂、急性发作SABA+激素",
                    "慢性阻塞性肺疾病、心源性哮喘、上气道阻塞"),

            buildDisease("冠心病-心绞痛", "I20.900", "心血管内科",
                    "胸闷,胸痛,心绞痛,冠心病,胸口痛,压榨,放射痛,心前区",
                    "胸骨后/心前区压榨性疼痛、活动后加重、休息/硝酸甘油缓解、向左肩臂放射",
                    "发作时心率增快、血压升高、S4奔马律",
                    "发作时ST段压低、T波倒置；冠脉CTA/造影见狭窄",
                    "心电图、心肌酶、冠脉CTA、冠脉造影、负荷试验",
                    "抗血小板、调脂、β受体阻滞剂、硝酸酯类、PCI/CABG",
                    "心肌梗死、主动脉夹层、心包炎、肋软骨炎、胃食管反流")
    );

    private static KnowledgeDisease buildDisease(String name, String icd, String dept,
                                                 String keywords, String symptoms, String signs,
                                                 String imaging, String tests, String treatments, String diff) {
        KnowledgeDisease d = new KnowledgeDisease();
        d.setDiseaseName(name);
        d.setIcdCode(icd);
        d.setDepartment(dept);
        d.setKeywords(keywords);
        d.setSymptoms(symptoms);
        d.setCommonSigns(signs);
        d.setRelatedImagingFeatures(imaging);
        d.setRecommendedTests(tests);
        d.setTypicalTreatments(treatments);
        d.setDifferentialFrom(diff);
        d.setSeverityWeight(50.0);
        return d;
    }

    @PostConstruct
    public void init() {
        seedKnowledgeBaseIfEmpty();
    }

    @Override
    public void seedKnowledgeBaseIfEmpty() {
        long count = knowledgeRepository.count();
        if (count == 0) {
            log.info("知识库为空，初始化种子疾病数据，共{}条", SEED_KNOWLEDGE.size());
            knowledgeRepository.saveAll(SEED_KNOWLEDGE);
        }
    }

    @Override
    @Transactional
    public DiagnosisSuggestionVO generateSuggestion(DiagnosisRequestDTO request) {
        log.info("生成辅助诊断建议，consultationId: {}, department: {}",
                request.getConsultationId(), request.getDepartment());

        if (!request.getGenerateSuggestions()) {
            throw new BusinessException("未请求生成建议");
        }

        String complaint = request.getPatientComplaint() != null ? request.getPatientComplaint() : "";
        String medicalHistory = request.getMedicalHistory() != null ? request.getMedicalHistory() : "";
        String imaging = request.getImagingFindings() != null ? request.getImagingFindings() : "";
        String vitals = request.getVitalSigns() != null ? request.getVitalSigns() : "";
        String labs = request.getLabResults() != null ? request.getLabResults() : "";

        String combined = complaint + " " + medicalHistory + " " + imaging + " " + vitals + " " + labs;
        String dept = request.getDepartment() != null && !request.getDepartment().isEmpty()
                ? request.getDepartment() : "内科";

        List<KnowledgeDisease> candidates = findCandidates(combined, dept);
        if (candidates.isEmpty()) {
            candidates = knowledgeRepository.findByDepartmentAndStatus(splitDepartment(dept).get(0), "ACTIVE");
        }
        if (candidates.isEmpty()) {
            candidates = knowledgeRepository.findByStatus("ACTIVE");
        }

        List<ScoredDisease> scored = candidates.stream()
                .map(d -> scoreDisease(d, combined, complaint, imaging))
                .sorted(Comparator.comparingDouble(ScoredDisease::score).reversed())
                .limit(4)
                .toList();

        DiagnosisSuggestion suggestion = new DiagnosisSuggestion();
        suggestion.setConsultationId(request.getConsultationId());
        suggestion.setPatientId(request.getPatientId());
        suggestion.setDoctorId(request.getDoctorId());
        suggestion.setDepartment(dept);
        suggestion.setPatientComplaint(complaint);
        suggestion.setImagingFindings(imaging);
        suggestion.setStatus("COMPLETED");

        if (!scored.isEmpty()) {
            ScoredDisease primary = scored.get(0);
            suggestion.setPrimaryDisease(primary.disease().getDiseaseName());
            suggestion.setPrimaryConfidence(Math.round(primary.score() * 100.0) / 100.0);
            suggestion.setPrimaryEvidence(buildEvidence(primary, complaint, imaging));

            if (scored.size() > 1) {
                suggestion.setSecondaryDisease1(scored.get(1).disease().getDiseaseName());
                suggestion.setSecondaryConfidence1(Math.round(scored.get(1).score() * 100.0) / 100.0);
            }
            if (scored.size() > 2) {
                suggestion.setSecondaryDisease2(scored.get(2).disease().getDiseaseName());
                suggestion.setSecondaryConfidence2(Math.round(scored.get(2).score() * 100.0) / 100.0);
            }
            if (scored.size() > 3) {
                suggestion.setSecondaryDisease3(scored.get(3).disease().getDiseaseName());
                suggestion.setSecondaryConfidence3(Math.round(scored.get(3).score() * 100.0) / 100.0);
            }

            Set<String> symptoms = new HashSet<>();
            Set<String> tests = new HashSet<>();
            for (ScoredDisease s : scored) {
                symptoms.addAll(parseCommaList(s.disease().getSymptoms()));
                tests.addAll(parseCommaList(s.disease().getRecommendedTests()));
            }
            suggestion.setRelatedSymptoms(String.join("、", symptoms.stream().limit(8).toList()));
            suggestion.setRecommendedTests(String.join("、", tests.stream().limit(6).toList()));

            List<String> diffs = scored.stream()
                    .map(s -> s.disease().getDifferentialFrom())
                    .filter(s -> s != null && !s.isEmpty())
                    .limit(2)
                    .toList();
            suggestion.setDifferentialDiagnosis(String.join("；", diffs));
        }

        DiagnosisSuggestion saved = suggestionRepository.save(suggestion);
        log.info("辅助诊断建议已生成，suggestionId: {}, primary: {}, confidence: {}",
                saved.getId(), saved.getPrimaryDisease(), saved.getPrimaryConfidence());

        return convertToVO(saved);
    }

    @Override
    public DiagnosisSuggestionVO getSuggestionByConsultationId(Long consultationId) {
        DiagnosisSuggestion s = suggestionRepository.findTopByConsultationIdOrderByCreateTimeDesc(consultationId)
                .orElseThrow(() -> new BusinessException("辅助诊断建议不存在"));
        return convertToVO(s);
    }

    @Override
    public List<DiagnosisSuggestionVO> getPatientSuggestions(Long patientId) {
        return suggestionRepository.findByPatientIdOrderByCreateTimeDesc(patientId).stream()
                .map(this::convertToVO)
                .collect(Collectors.toList());
    }

    @Override
    public List<DiagnosisSuggestionVO> getDoctorSuggestions(Long doctorId) {
        return suggestionRepository.findByDoctorIdOrderByCreateTimeDesc(doctorId).stream()
                .map(this::convertToVO)
                .collect(Collectors.toList());
    }

    private List<KnowledgeDisease> findCandidates(String combined, String department) {
        Set<KnowledgeDisease> result = new HashSet<>();
        List<String> depts = splitDepartment(department);
        for (String dept : depts) {
            result.addAll(knowledgeRepository.findByDepartmentAndStatus(dept, "ACTIVE"));
        }
        for (String kw : extractKeywords(combined)) {
            if (kw.length() >= 2) {
                result.addAll(knowledgeRepository.searchByKeyword(kw));
            }
        }
        return new ArrayList<>(result);
    }

    private List<String> splitDepartment(String dept) {
        if (dept == null || dept.isEmpty()) return List.of("内科");
        return Arrays.asList(dept.split("[,，、/]"));
    }

    private ScoredDisease scoreDisease(KnowledgeDisease d, String combined, String complaint, String imaging) {
        double score = 0.0;
        String combinedLower = combined.toLowerCase();
        int matches = 0;
        int total = 0;

        Set<String> keywordHits = new HashSet<>();
        for (String kw : parseCommaList(d.getKeywords())) {
            total++;
            if (!kw.isEmpty() && combinedLower.contains(kw.toLowerCase())) {
                matches++;
                keywordHits.add(kw);
            }
        }
        if (total > 0) {
            score += (matches * 1.0 / total) * 55;
        }

        int symptomMatches = 0;
        int symptomTotal = 0;
        for (String s : parseCommaList(d.getSymptoms())) {
            symptomTotal++;
            if (combinedLower.contains(s.substring(0, Math.min(2, s.length())))) {
                symptomMatches++;
            }
        }
        if (symptomTotal > 0) {
            score += (symptomMatches * 1.0 / symptomTotal) * 30;
        }

        if (imaging != null && !imaging.isEmpty() && d.getRelatedImagingFeatures() != null) {
            String imagingLower = imaging.toLowerCase();
            for (String feature : parseCommaList(d.getRelatedImagingFeatures())) {
                if (!feature.equals("—") && !feature.isEmpty() && imagingLower.contains(feature.substring(0, Math.min(2, feature.length())))) {
                    score += 15;
                    break;
                }
            }
        }

        if (score < 5.0) {
            score = 25.0 + (matches + symptomMatches) * 2.0;
        }

        score = Math.min(98.0, Math.max(15.0, score));
        return new ScoredDisease(d, score, keywordHits);
    }

    private String buildEvidence(ScoredDisease scored, String complaint, String imaging) {
        KnowledgeDisease d = scored.disease();
        StringBuilder sb = new StringBuilder();
        if (!scored.matchedKeywords().isEmpty()) {
            sb.append("关键词匹配: ").append(String.join("、", scored.matchedKeywords().stream().limit(5).toList())).append("。");
        }
        if (complaint != null && !complaint.isEmpty()) {
            sb.append("主诉「").append(complaint.length() > 50 ? complaint.substring(0, 50) + "…" : complaint)
                    .append("」与「").append(d.getDiseaseName()).append("」常见症状高度相关。");
        }
        if (imaging != null && !imaging.isEmpty()) {
            sb.append("结合影像学特征综合判断。");
        }
        if (sb.length() == 0) {
            sb.append("基于主诉及疾病知识图谱的相关性匹配。");
        }
        return sb.toString();
    }

    private List<String> parseCommaList(String str) {
        if (str == null || str.isEmpty()) return List.of();
        return Arrays.stream(str.split("[,，、;；、\\s]+"))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .toList();
    }

    private List<String> extractKeywords(String text) {
        if (text == null || text.isEmpty()) return List.of();
        return parseCommaList(text.replaceAll("[，。？！、；：\\s]+", ","));
    }

    private record ScoredDisease(KnowledgeDisease disease, double score, Set<String> matchedKeywords) { }

    private DiagnosisSuggestionVO convertToVO(DiagnosisSuggestion s) {
        DiagnosisSuggestionVO vo = DiagnosisSuggestionVO.builder()
                .id(s.getId())
                .consultationId(s.getConsultationId())
                .patientId(s.getPatientId())
                .doctorId(s.getDoctorId())
                .department(s.getDepartment())
                .patientComplaint(s.getPatientComplaint())
                .imagingFindings(s.getImagingFindings())
                .primaryDisease(s.getPrimaryDisease())
                .primaryConfidence(s.getPrimaryConfidence())
                .primaryEvidence(s.getPrimaryEvidence())
                .secondaryDisease1(s.getSecondaryDisease1())
                .secondaryConfidence1(s.getSecondaryConfidence1())
                .secondaryDisease2(s.getSecondaryDisease2())
                .secondaryConfidence2(s.getSecondaryConfidence2())
                .secondaryDisease3(s.getSecondaryDisease3())
                .secondaryConfidence3(s.getSecondaryConfidence3())
                .relatedSymptoms(parseCommaList(s.getRelatedSymptoms()))
                .recommendedTests(parseCommaList(s.getRecommendedTests()))
                .differentialDiagnosis(s.getDifferentialDiagnosis())
                .status(s.getStatus())
                .disclaimer(s.getDisclaimer())
                .createTime(s.getCreateTime() != null ? s.getCreateTime().format(DTF) : null)
                .build();
        return vo;
    }
}
