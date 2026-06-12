package com.telemed.service.impl;

import cn.hutool.core.codec.Base64;
import com.telemed.common.constant.SignatureStatus;
import com.telemed.common.dto.signature.ConsultationSignDTO;
import com.telemed.common.dto.signature.PdfGenerateDTO;
import com.telemed.common.exception.BusinessException;
import com.telemed.common.vo.signature.ConsultationSignatureVO;
import com.telemed.model.entity.Consultation;
import com.telemed.model.entity.ConsultationConclusion;
import com.telemed.model.entity.ConsultationDoctor;
import com.telemed.model.entity.ConsultationSignature;
import com.telemed.model.entity.Doctor;
import com.telemed.model.entity.Hospital;
import com.telemed.model.entity.Patient;
import com.telemed.model.entity.User;
import com.telemed.model.repository.ConsultationConclusionRepository;
import com.telemed.model.repository.ConsultationDoctorRepository;
import com.telemed.model.repository.ConsultationRepository;
import com.telemed.model.repository.ConsultationSignatureRepository;
import com.telemed.model.repository.DoctorRepository;
import com.telemed.model.repository.HospitalRepository;
import com.telemed.model.repository.PatientRepository;
import com.telemed.model.repository.UserRepository;
import com.telemed.service.ConsultationSignatureService;
import com.telemed.service.MinioService;
import com.telemed.service.PdfService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.ByteArrayInputStream;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ConsultationSignatureServiceImpl implements ConsultationSignatureService {

    private final ConsultationRepository consultationRepository;
    private final ConsultationSignatureRepository consultationSignatureRepository;
    private final ConsultationDoctorRepository consultationDoctorRepository;
    private final ConsultationConclusionRepository consultationConclusionRepository;
    private final DoctorRepository doctorRepository;
    private final PatientRepository patientRepository;
    private final UserRepository userRepository;
    private final HospitalRepository hospitalRepository;
    private final PdfService pdfService;
    private final MinioService minioService;

    @Value("${minio.bucketName}")
    private String defaultBucket;

    @Value("${signature.pdf-bucket:tele-med-pdf}")
    private String pdfBucket;

    @Override
    public byte[] generateDraftPdf(Long consultationId, String conclusionContent, List<String> imageUrls) {
        Consultation consultation = consultationRepository.findById(consultationId)
                .orElseThrow(() -> new BusinessException("会诊不存在"));

        Patient patient = patientRepository.findById(consultation.getPatientId())
                .orElseThrow(() -> new BusinessException("患者信息不存在"));

        Doctor doctor = doctorRepository.findById(consultation.getDoctorId())
                .orElse(null);

        String doctorName = "";
        String doctorTitle = "";
        if (doctor != null) {
            doctorTitle = doctor.getTitle() != null ? doctor.getTitle() : "";
            User user = userRepository.findById(doctor.getUserId()).orElse(null);
            if (user != null) {
                doctorName = user.getRealName();
            }
        }

        Hospital hospital = hospitalRepository.findById(consultation.getHospitalId()).orElse(null);
        String hospitalName = hospital != null ? hospital.getName() : "";

        PdfGenerateDTO pdfDTO = new PdfGenerateDTO();
        pdfDTO.setConsultationId(consultationId);
        pdfDTO.setConsultationNo(consultation.getConsultationNo());
        pdfDTO.setConclusionContent(conclusionContent);
        pdfDTO.setImageUrls(imageUrls);
        pdfDTO.setPatientName(patient.getName());
        pdfDTO.setPatientIdCard(patient.getIdCard());
        pdfDTO.setPatientMedicalCardNo(patient.getMedicalCardNo());
        pdfDTO.setPatientGender(patient.getGender());
        pdfDTO.setPatientAge(patient.getAge());
        pdfDTO.setDoctorName(doctorName);
        pdfDTO.setDoctorTitle(doctorTitle);
        pdfDTO.setDepartment(doctor != null ? doctor.getDepartment() : "");
        pdfDTO.setHospitalName(hospitalName);

        return pdfService.generateConsultationPdf(pdfDTO);
    }

    @Override
    @Transactional
    public ConsultationSignatureVO doctorSign(ConsultationSignDTO signDTO) {
        Consultation consultation = consultationRepository.findById(signDTO.getConsultationId())
                .orElseThrow(() -> new BusinessException("会诊不存在"));

        ConsultationSignature signature = consultationSignatureRepository
                .findByConsultationIdAndDoctorId(signDTO.getConsultationId(), signDTO.getDoctorId())
                .orElseThrow(() -> new BusinessException("该医生无签名权限"));

        if (signature.getSignStatus() != null && signature.getSignStatus() == SignatureStatus.SIGNED.getCode()) {
            throw new BusinessException("该医生已签名，请勿重复签名");
        }

        List<ConsultationSignature> allSignatures = consultationSignatureRepository
                .findByConsultationIdOrderBySignOrderAsc(signDTO.getConsultationId());

        ConsultationSignature currentSigner = getCurrentSignerInternal(allSignatures);
        if (currentSigner == null || !currentSigner.getId().equals(signature.getId())) {
            throw new BusinessException("当前不是您的签名顺序，请等待前一位医生签名");
        }

        byte[] currentPdf;
        if (signDTO.getPdfBase64() != null && !signDTO.getPdfBase64().isEmpty()) {
            currentPdf = decodeBase64ToBytes(signDTO.getPdfBase64());
        } else {
            ConsultationConclusion conclusion = consultationConclusionRepository
                    .findByConsultationId(signDTO.getConsultationId()).orElse(null);
            String conclusionContent = conclusion != null ? conclusion.getContent() : "";
            currentPdf = loadCurrentPdf(signDTO.getConsultationId(), consultation.getConsultationNo(),
                    conclusionContent, allSignatures, signature.getSignOrder());
        }

        Doctor doctor = doctorRepository.findById(signDTO.getDoctorId())
                .orElseThrow(() -> new BusinessException("医生不存在"));
        User user = userRepository.findById(doctor.getUserId()).orElse(null);
        String doctorName = user != null ? user.getRealName() : "医生";

        currentPdf = pdfService.addDoctorSignature(currentPdf, signDTO, doctorName);

        if (signDTO.getSignatureData() != null && !signDTO.getSignatureData().isEmpty()) {
            String signatureImageName = "signatures/" + consultation.getConsultationNo() + "_" + signDTO.getDoctorId() + ".png";
            byte[] imageBytes = decodeBase64Image(signDTO.getSignatureData());
            String imageUrl = minioService.uploadBytes(defaultBucket, signatureImageName, imageBytes, "image/png");
            signature.setSignatureImageUrl(imageUrl);
        }

        signature.setSignStatus(SignatureStatus.SIGNED.getCode());
        signature.setSignTime(LocalDateTime.now());
        signature.setSignPositionX(signDTO.getSignPositionX());
        signature.setSignPositionY(signDTO.getSignPositionY());
        signature.setSignWidth(signDTO.getSignWidth());
        signature.setSignHeight(signDTO.getSignHeight());
        signature.setSignPage(signDTO.getSignPage());
        signature.setSignReason(signDTO.getSignReason());
        signature.setSignLocation(signDTO.getSignLocation());
        signature.setSm2PublicKey(signDTO.getSm2PublicKey());
        signature.setSm2Signature(signDTO.getSm2Signature());
        signature.setSignatureData(signDTO.getSignatureData());
        consultationSignatureRepository.save(signature);

        boolean allSigned = isAllSignedInternal(allSignatures);
        if (allSigned) {
            String pdfObjectName = pdfService.savePdfToMinio(currentPdf, consultation.getConsultationNo());

            ConsultationConclusion conclusion = consultationConclusionRepository
                    .findByConsultationId(signDTO.getConsultationId()).orElse(null);
            if (conclusion == null) {
                conclusion = new ConsultationConclusion();
                conclusion.setConsultationId(signDTO.getConsultationId());
                conclusion.setDoctorId(consultation.getDoctorId());
                conclusion.setPatientId(consultation.getPatientId());
            }
            conclusion.setFileUrl(pdfObjectName);
            consultationConclusionRepository.save(conclusion);
        } else {
            saveIntermediatePdf(currentPdf, consultation.getConsultationNo());
        }

        return convertToVO(signature);
    }

    @Override
    public List<ConsultationSignatureVO> getConsultationSignatures(Long consultationId) {
        List<ConsultationSignature> signatures = consultationSignatureRepository
                .findByConsultationIdOrderBySignOrderAsc(consultationId);
        return signatures.stream()
                .map(this::convertToVO)
                .collect(Collectors.toList());
    }

    @Override
    public ConsultationSignatureVO getCurrentSigner(Long consultationId) {
        List<ConsultationSignature> signatures = consultationSignatureRepository
                .findByConsultationIdOrderBySignOrderAsc(consultationId);
        ConsultationSignature current = getCurrentSignerInternal(signatures);
        return current != null ? convertToVO(current) : null;
    }

    @Override
    public String getFinalPdfUrl(Long consultationId) {
        Consultation consultation = consultationRepository.findById(consultationId)
                .orElseThrow(() -> new BusinessException("会诊不存在"));

        if (!isAllSigned(consultationId)) {
            throw new BusinessException("签名未完成，无法获取最终PDF");
        }

        String objectName = "conclusion-pdf/" + consultation.getConsultationNo() + ".pdf";
        return minioService.getFileUrl(pdfBucket, objectName);
    }

    @Override
    public byte[] getFinalPdf(Long consultationId) {
        Consultation consultation = consultationRepository.findById(consultationId)
                .orElseThrow(() -> new BusinessException("会诊不存在"));

        String objectName = "conclusion-pdf/" + consultation.getConsultationNo() + ".pdf";
        try {
            return minioService.downloadFile(pdfBucket, objectName);
        } catch (Exception e) {
            ConsultationConclusion conclusion = consultationConclusionRepository
                    .findByConsultationId(consultationId).orElse(null);
            if (conclusion != null && conclusion.getFileUrl() != null) {
                return minioService.downloadFile(defaultBucket, conclusion.getFileUrl());
            }
            throw new BusinessException("PDF文件不存在");
        }
    }

    @Override
    public boolean isAllSigned(Long consultationId) {
        List<ConsultationSignature> signatures = consultationSignatureRepository
                .findByConsultationIdOrderBySignOrderAsc(consultationId);
        return isAllSignedInternal(signatures);
    }

    @Override
    @Transactional
    public void initSignatureWorkflow(Long consultationId, List<Long> doctorIds) {
        if (doctorIds == null || doctorIds.isEmpty()) {
            throw new BusinessException("签名医生列表不能为空");
        }

        consultationSignatureRepository.findByConsultationIdOrderBySignOrderAsc(consultationId)
                .forEach(signature -> consultationSignatureRepository.delete(signature));

        for (int i = 0; i < doctorIds.size(); i++) {
            ConsultationSignature signature = new ConsultationSignature();
            signature.setConsultationId(consultationId);
            signature.setDoctorId(doctorIds.get(i));
            signature.setSignOrder(i + 1);
            signature.setSignStatus(SignatureStatus.PENDING.getCode());
            consultationSignatureRepository.save(signature);
        }
    }

    private ConsultationSignature getCurrentSignerInternal(List<ConsultationSignature> signatures) {
        return signatures.stream()
                .filter(s -> s.getSignStatus() == null || s.getSignStatus() == SignatureStatus.PENDING.getCode())
                .min(Comparator.comparingInt(ConsultationSignature::getSignOrder))
                .orElse(null);
    }

    private boolean isAllSignedInternal(List<ConsultationSignature> signatures) {
        if (signatures == null || signatures.isEmpty()) {
            return false;
        }
        return signatures.stream()
                .allMatch(s -> s.getSignStatus() != null && s.getSignStatus() == SignatureStatus.SIGNED.getCode());
    }

    private byte[] loadCurrentPdf(Long consultationId, String consultationNo, String conclusionContent,
                                   List<ConsultationSignature> allSignatures, Integer currentOrder) {
        String intermediateObjectName = "intermediate-pdf/" + consultationNo + ".pdf";
        try {
            return minioService.downloadFile(pdfBucket, intermediateObjectName);
        } catch (Exception e) {
            return generateDraftPdf(consultationId, conclusionContent, new ArrayList<>());
        }
    }

    private void saveIntermediatePdf(byte[] pdfBytes, String consultationNo) {
        String objectName = "intermediate-pdf/" + consultationNo + ".pdf";
        minioService.uploadBytes(pdfBucket, objectName, pdfBytes, "application/pdf");
    }

    private ConsultationSignatureVO convertToVO(ConsultationSignature signature) {
        ConsultationSignatureVO vo = new ConsultationSignatureVO();
        vo.setId(signature.getId());
        vo.setConsultationId(signature.getConsultationId());
        vo.setDoctorId(signature.getDoctorId());
        vo.setSignOrder(signature.getSignOrder());
        vo.setSignStatus(signature.getSignStatus());
        vo.setSignStatusText(getStatusText(signature.getSignStatus()));
        vo.setSignatureImageUrl(signature.getSignatureImageUrl());
        vo.setSm2PublicKey(signature.getSm2PublicKey());
        vo.setSignPositionX(signature.getSignPositionX());
        vo.setSignPositionY(signature.getSignPositionY());
        vo.setSignWidth(signature.getSignWidth());
        vo.setSignHeight(signature.getSignHeight());
        vo.setSignPage(signature.getSignPage());
        vo.setSignReason(signature.getSignReason());
        vo.setSignLocation(signature.getSignLocation());
        vo.setSignTime(signature.getSignTime());
        vo.setCreateTime(signature.getCreateTime());

        Doctor doctor = doctorRepository.findById(signature.getDoctorId()).orElse(null);
        if (doctor != null) {
            vo.setDoctorTitle(doctor.getTitle());
            vo.setDepartment(doctor.getDepartment());
            User user = userRepository.findById(doctor.getUserId()).orElse(null);
            if (user != null) {
                vo.setDoctorName(user.getRealName());
            }
        }

        return vo;
    }

    private String getStatusText(Integer status) {
        if (status == null) return "未知";
        for (SignatureStatus s : SignatureStatus.values()) {
            if (s.getCode() == status) {
                return s.getDescription();
            }
        }
        return "未知";
    }

    private byte[] decodeBase64ToBytes(String base64) {
        try {
            if (base64.contains(",")) {
                base64 = base64.substring(base64.indexOf(",") + 1);
            }
            return java.util.Base64.getDecoder().decode(base64);
        } catch (Exception e) {
            throw new BusinessException("Base64解码失败: " + e.getMessage());
        }
    }

    private byte[] decodeBase64Image(String base64Data) {
        try {
            if (base64Data.contains(",")) {
                base64Data = base64Data.substring(base64Data.indexOf(",") + 1);
            }
            return java.util.Base64.getDecoder().decode(base64Data);
        } catch (Exception e) {
            throw new BusinessException("解析签名图片失败: " + e.getMessage());
        }
    }
}
