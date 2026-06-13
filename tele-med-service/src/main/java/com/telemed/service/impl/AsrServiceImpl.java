package com.telemed.service.impl;

import com.telemed.common.constant.ConsultationStatus;
import com.telemed.common.dto.asr.AsrResultDTO;
import com.telemed.common.dto.asr.AsrTaskDTO;
import com.telemed.common.dto.asr.NlpAnalysisRequest;
import com.telemed.common.dto.asr.NlpAnalysisResponse;
import com.telemed.common.exception.BusinessException;
import com.telemed.common.vo.asr.AsrQualityReportVO;
import com.telemed.model.entity.Consultation;
import com.telemed.model.entity.ConsultationAsrTranscript;
import com.telemed.model.entity.Doctor;
import com.telemed.model.entity.Patient;
import com.telemed.model.repository.ConsultationAsrTranscriptRepository;
import com.telemed.model.repository.ConsultationRepository;
import com.telemed.model.repository.DoctorRepository;
import com.telemed.model.repository.PatientRepository;
import com.telemed.service.AsrQualityService;
import com.telemed.service.AsrService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;

@Slf4j
@Service
@RequiredArgsConstructor
public class AsrServiceImpl implements AsrService {

    private final ConsultationRepository consultationRepository;
    private final ConsultationAsrTranscriptRepository asrTranscriptRepository;
    private final DoctorRepository doctorRepository;
    private final PatientRepository patientRepository;
    private final AsrQualityService asrQualityService;

    @Value("${asr.mock.enabled:true}")
    private boolean mockEnabled;

    @Value("${asr.provider:MOCK_WHISPER}")
    private String asrProvider;

    private static final List<String> MOCK_DOCTOR_UTTERANCES = Arrays.asList(
            "您好，请问您是哪里不舒服呢？",
            "这种症状出现多久了？有没有加重或者缓解的因素？",
            "除了您说的这些，还有没有其他伴随症状，比如发热、恶心之类的？",
            "您之前有没有做过什么检查？用过什么药物吗？",
            "您的既往病史我已经了解了，有没有药物过敏史？",
            "让我给您做一下体格检查，您先放松。",
            "根据您的症状和体征，我建议您先做几个辅助检查。",
            "初步判断可能是这个方向，但需要检查结果进一步确认。",
            "请您一定要按时服药，注意休息，饮食清淡。",
            "一周后请复诊，如果症状加重请及时就医。"
    );

    private static final List<String> MOCK_PATIENT_UTTERANCES = Arrays.asList(
            "医生您好，我最近头特别疼，有时候还恶心想吐。",
            "大概有三天了吧，昨天好像更严重了一些。",
            "有时候会觉得有点晕，但没有发烧。",
            "之前在社区医院看过，说是感冒，但吃了药没什么效果。",
            "我以前有高血压病史，一直在吃药控制。",
            "药物过敏的话，好像对青霉素有点过敏。",
            "好的，我配合您检查。",
            "好的，都需要做哪些检查呢？",
            "好的，我会按时吃药的，谢谢您。",
            "好的，我一定按时来复诊。"
    );

    @Override
    @Transactional
    public void createAsrTask(AsrTaskDTO taskDTO) {
        log.info("创建ASR任务，consultationId: {}", taskDTO.getConsultationId());
        AsrResultDTO result = executeAsrTranscription(taskDTO);
        saveTranscriptResult(taskDTO.getConsultationId(), result);
    }

    @Override
    public AsrResultDTO executeAsrTranscription(AsrTaskDTO taskDTO) {
        log.info("执行ASR转写，consultationId: {}, mockEnabled: {}", taskDTO.getConsultationId(), mockEnabled);
        if (mockEnabled) {
            return getMockAsrResult(taskDTO.getConsultationId(), taskDTO.getDurationSeconds() != null ? taskDTO.getDurationSeconds() : 600);
        }
        throw new BusinessException("生产ASR服务未配置，请开启mock模式");
    }

    @Override
    @Transactional
    public AsrQualityReportVO processConsultationAsrAndQuality(Long consultationId) {
        log.info("开始会诊ASR+质检流程，consultationId: {}", consultationId);

        Consultation consultation = consultationRepository.findById(consultationId)
                .orElseThrow(() -> new BusinessException("会诊不存在"));

        if (consultation.getStatus() != ConsultationStatus.FINISHED.getCode()
                && consultation.getStatus() != ConsultationStatus.ONGOING.getCode()) {
            throw new BusinessException("会诊状态不允许生成质检报告");
        }

        Integer duration = consultation.getDuration() != null ? consultation.getDuration() : 600;

        AsrTaskDTO taskDTO = AsrTaskDTO.builder()
                .consultationId(consultationId)
                .consultationNo(consultation.getConsultationNo())
                .doctorId(consultation.getDoctorId())
                .patientId(consultation.getPatientId())
                .durationSeconds(duration)
                .source("POST_CONSULTATION")
                .build();

        AsrResultDTO asrResult = executeAsrTranscription(taskDTO);
        saveTranscriptResult(consultationId, asrResult);

        Doctor doctor = doctorRepository.findById(consultation.getDoctorId()).orElse(null);
        String department = doctor != null ? doctor.getDepartment() : "内科";

        NlpAnalysisRequest nlpRequest = buildNlpRequest(consultationId, asrResult, department);
        NlpAnalysisResponse nlpResponse = asrQualityService.analyzeTranscript(nlpRequest);

        return asrQualityService.generateQualityReport(consultationId, nlpResponse);
    }

    @Override
    public AsrResultDTO getMockAsrResult(Long consultationId, Integer durationSeconds) {
        int totalSeconds = durationSeconds != null ? durationSeconds : 600;
        int utteranceCount = Math.min(20, Math.max(8, totalSeconds / 30));

        List<AsrResultDTO.TranscriptSegment> segments = new ArrayList<>();
        StringBuilder fullTranscript = new StringBuilder();
        Random random = new Random(consultationId != null ? consultationId : 42L);

        int currentTime = 0;
        int timePerUtterance = Math.max(5, totalSeconds / utteranceCount);

        for (int i = 0; i < utteranceCount; i++) {
            boolean isDoctor = (i % 2 == 0);
            String role = isDoctor ? "DOCTOR" : "PATIENT";
            List<String> pool = isDoctor ? MOCK_DOCTOR_UTTERANCES : MOCK_PATIENT_UTTERANCES;
            String text = pool.get(random.nextInt(pool.size()));
            int duration = Math.max(3, timePerUtterance / 2 + random.nextInt(timePerUtterance / 2));

            segments.add(AsrResultDTO.TranscriptSegment.builder()
                    .speakerRole(role)
                    .speakerId(isDoctor ? 1L : 2L)
                    .speakerName(isDoctor ? "医生" : "患者")
                    .text(text)
                    .startTime(currentTime)
                    .endTime(currentTime + duration)
                    .confidence(0.85 + random.nextDouble() * 0.14)
                    .build());

            if (isDoctor) {
                fullTranscript.append("【医生】").append(text).append("\n");
            } else {
                fullTranscript.append("【患者】").append(text).append("\n");
            }

            currentTime += duration;
        }

        return AsrResultDTO.builder()
                .consultationId(consultationId)
                .status("SUCCESS")
                .asrProvider(asrProvider)
                .totalDuration(currentTime)
                .fullTranscript(fullTranscript.toString())
                .segments(segments)
                .build();
    }

    private void saveTranscriptResult(Long consultationId, AsrResultDTO result) {
        asrTranscriptRepository.deleteByConsultationId(consultationId);

        Consultation consultation = consultationRepository.findById(consultationId).orElse(null);
        String doctorName = null;
        String patientName = null;
        if (consultation != null) {
            Doctor doctor = doctorRepository.findById(consultation.getDoctorId()).orElse(null);
            Patient patient = patientRepository.findById(consultation.getPatientId()).orElse(null);
            if (doctor != null) {
                doctorName = doctor.getTitle() != null ? doctor.getTitle() : "医生";
            }
            if (patient != null) {
                patientName = patient.getName();
            }
        }

        List<ConsultationAsrTranscript> transcripts = new ArrayList<>();
        for (AsrResultDTO.TranscriptSegment seg : result.getSegments()) {
            ConsultationAsrTranscript t = new ConsultationAsrTranscript();
            t.setConsultationId(consultationId);
            t.setSpeakerRole(seg.getSpeakerRole());
            t.setSpeakerId(seg.getSpeakerId());
            t.setSpeakerName("DOCTOR".equals(seg.getSpeakerRole()) ? doctorName : patientName);
            if (t.getSpeakerName() == null) {
                t.setSpeakerName(seg.getSpeakerName());
            }
            t.setTranscriptText(seg.getText());
            int dur = seg.getEndTime() != null && seg.getStartTime() != null
                    ? seg.getEndTime() - seg.getStartTime() : 0;
            t.setDurationSeconds(dur);
            transcripts.add(t);
        }

        asrTranscriptRepository.saveAll(transcripts);
        log.info("ASR转写结果已保存，consultationId: {}, segments: {}", consultationId, transcripts.size());
    }

    private NlpAnalysisRequest buildNlpRequest(Long consultationId, AsrResultDTO asrResult, String department) {
        List<NlpAnalysisRequest.SegmentUtterance> utterances = new ArrayList<>();
        for (AsrResultDTO.TranscriptSegment seg : asrResult.getSegments()) {
            utterances.add(NlpAnalysisRequest.SegmentUtterance.builder()
                    .speakerRole(seg.getSpeakerRole())
                    .text(seg.getText())
                    .startTime(seg.getStartTime())
                    .endTime(seg.getEndTime())
                    .build());
        }

        return NlpAnalysisRequest.builder()
                .consultationId(consultationId)
                .fullTranscript(asrResult.getFullTranscript())
                .utterances(utterances)
                .department(department)
                .build();
    }
}
