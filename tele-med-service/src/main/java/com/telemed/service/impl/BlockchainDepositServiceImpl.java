package com.telemed.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.telemed.common.constant.DepositStatus;
import com.telemed.common.dto.blockchain.BlockchainDepositRequestDTO;
import com.telemed.common.dto.blockchain.BlockchainDepositResponseDTO;
import com.telemed.common.exception.BusinessException;
import com.telemed.common.util.HashUtil;
import com.telemed.common.vo.blockchain.BlockchainDepositVO;
import com.telemed.model.entity.BlockchainDepositLog;
import com.telemed.model.entity.Consultation;
import com.telemed.model.entity.ConsultationConclusion;
import com.telemed.model.entity.ConsultationSignature;
import com.telemed.model.entity.Doctor;
import com.telemed.model.entity.Patient;
import com.telemed.model.entity.User;
import com.telemed.model.repository.BlockchainDepositLogRepository;
import com.telemed.model.repository.ConsultationConclusionRepository;
import com.telemed.model.repository.ConsultationRepository;
import com.telemed.model.repository.ConsultationSignatureRepository;
import com.telemed.model.repository.DoctorRepository;
import com.telemed.model.repository.PatientRepository;
import com.telemed.model.repository.UserRepository;
import com.telemed.service.BlockchainDepositService;
import com.telemed.service.BlockchainDepositStrategy;
import com.telemed.service.ConsultationSignatureService;
import com.telemed.service.MinioService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.ByteArrayInputStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class BlockchainDepositServiceImpl implements BlockchainDepositService {

    private final BlockchainDepositLogRepository depositLogRepository;
    private final ConsultationRepository consultationRepository;
    private final ConsultationConclusionRepository conclusionRepository;
    private final ConsultationSignatureRepository signatureRepository;
    private final PatientRepository patientRepository;
    private final DoctorRepository doctorRepository;
    private final UserRepository userRepository;
    private final MinioService minioService;
    private final ConsultationSignatureService signatureService;
    private final List<BlockchainDepositStrategy> depositStrategies;

    @Value("${blockchain.hash-algorithm:SHA-256}")
    private String hashAlgorithm;

    @Value("${blockchain.max-retry-count:3}")
    private int maxRetryCount;

    @Value("${blockchain.retry-interval-seconds:10}")
    private int retryIntervalSeconds;

    @Value("${blockchain.pdf-bucket:tele-med-pdf}")
    private String pdfBucket;

    @Value("${blockchain.verify-base-url:http://localhost:8080/api/blockchain/verify}")
    private String verifyBaseUrl;

    private final ObjectMapper objectMapper = new ObjectMapper();

    private BlockchainDepositStrategy getActiveStrategy() {
        if (depositStrategies == null || depositStrategies.isEmpty()) {
            throw new BusinessException("未配置区块链存证策略");
        }
        return depositStrategies.get(0);
    }

    @Override
    @Transactional
    public BlockchainDepositVO depositPdf(Long consultationId) {
        log.info("开始区块链存证, consultationId: {}", consultationId);

        Consultation consultation = consultationRepository.findById(consultationId)
                .orElseThrow(() -> new BusinessException("会诊不存在"));

        if (!signatureService.isAllSigned(consultationId)) {
            throw new BusinessException("签名未完成，无法进行存证");
        }

        byte[] pdfBytes = signatureService.getFinalPdf(consultationId);
        String fileHash = calculateFileHash(pdfBytes);

        Optional<BlockchainDepositLog> existingLog = depositLogRepository.findByFileHash(fileHash);
        if (existingLog.isPresent() && existingLog.get().getDepositStatus() == DepositStatus.SUCCESS.getCode()) {
            log.info("该文件已存证, fileHash: {}, consultationId: {}", fileHash, consultationId);
            return convertToVO(existingLog.get());
        }

        BlockchainDepositLog depositLog = createDepositLog(consultation, pdfBytes, fileHash);

        return executeDepositWithRetry(depositLog, pdfBytes, consultation);
    }

    @Async
    @Transactional
    public void depositPdfAsync(Long consultationId) {
        try {
            depositPdf(consultationId);
        } catch (Exception e) {
            log.error("异步存证异常, consultationId: {}", consultationId, e);
        }
    }

    private BlockchainDepositLog createDepositLog(Consultation consultation, byte[] pdfBytes, String fileHash) {
        BlockchainDepositLog depositLog = new BlockchainDepositLog();
        depositLog.setConsultationId(consultation.getId());
        depositLog.setConsultationNo(consultation.getConsultationNo());
        depositLog.setPdfUrl("conclusion-pdf/" + consultation.getConsultationNo() + ".pdf");
        depositLog.setFileHash(fileHash);
        depositLog.setHashAlgorithm(hashAlgorithm);
        depositLog.setDepositStatus(DepositStatus.PENDING.getCode());
        depositLog.setRetryCount(0);
        depositLog.setDepositProvider(getActiveStrategy().getProviderName());

        try {
            depositLog.setDepositData(objectMapper.writeValueAsString(
                    java.util.Map.of(
                            "fileSize", pdfBytes.length,
                            "consultationNo", consultation.getConsultationNo(),
                            "createTime", LocalDateTime.now().toString()
                    )
            ));
        } catch (Exception e) {
            log.warn("序列化存证数据失败", e);
        }

        return depositLogRepository.save(depositLog);
    }

    private BlockchainDepositVO executeDepositWithRetry(BlockchainDepositLog depositLog, byte[] pdfBytes, Consultation consultation) {
        BlockchainDepositStrategy strategy = getActiveStrategy();
        int attempt = 0;
        Exception lastException = null;

        while (attempt < maxRetryCount) {
            try {
                depositLog.setDepositStatus(attempt == 0 ? DepositStatus.DEPOSITING.getCode() : DepositStatus.RETRYING.getCode());
                depositLog.setRetryCount(attempt);
                depositLog.setUpdateTime(LocalDateTime.now());
                depositLogRepository.save(depositLog);

                BlockchainDepositRequestDTO request = buildDepositRequest(consultation, pdfBytes, depositLog.getFileHash());
                BlockchainDepositResponseDTO response = strategy.deposit(request);

                if (response.isSuccess()) {
                    return handleSuccess(depositLog, response, consultation);
                } else {
                    lastException = new BusinessException(response.getErrorMessage());
                    log.warn("存证失败, 第{}次尝试, consultationId: {}, error: {}",
                            attempt + 1, consultation.getId(), response.getErrorMessage());
                }
            } catch (Exception e) {
                lastException = e;
                log.error("存证异常, 第{}次尝试, consultationId: {}", attempt + 1, consultation.getId(), e);
            }

            attempt++;
            if (attempt < maxRetryCount) {
                try {
                    Thread.sleep(retryIntervalSeconds * 1000L);
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
        }

        return handleFailure(depositLog, lastException, consultation);
    }

    private BlockchainDepositVO handleSuccess(BlockchainDepositLog depositLog, BlockchainDepositResponseDTO response, Consultation consultation) {
        depositLog.setDepositStatus(DepositStatus.SUCCESS.getCode());
        depositLog.setTxHash(response.getTxHash());
        depositLog.setBlockHeight(response.getBlockHeight());
        depositLog.setBlockTime(response.getBlockTime());
        depositLog.setDepositTimestamp(response.getDepositTimestamp());
        depositLog.setDepositVerifyUrl(response.getVerifyUrl());
        depositLog.setDepositProvider(response.getProvider());
        depositLog.setUpdateTime(LocalDateTime.now());
        depositLogRepository.save(depositLog);

        updateConsultationConclusion(consultation.getId(), depositLog);

        log.info("区块链存证成功, consultationId: {}, txHash: {}", consultation.getId(), response.getTxHash());
        return convertToVO(depositLog);
    }

    private BlockchainDepositVO handleFailure(BlockchainDepositLog depositLog, Exception exception, Consultation consultation) {
        depositLog.setDepositStatus(DepositStatus.FAILED.getCode());
        depositLog.setErrorMessage(exception != null ? exception.getMessage() : "未知错误");
        if (exception != null) {
            StringWriter sw = new StringWriter();
            PrintWriter pw = new PrintWriter(sw);
            exception.printStackTrace(pw);
            depositLog.setErrorStackTrace(sw.toString());
        }
        depositLog.setUpdateTime(LocalDateTime.now());
        depositLogRepository.save(depositLog);

        updateConsultationConclusion(consultation.getId(), depositLog);

        log.error("区块链存证最终失败, consultationId: {}, 已重试{}次", consultation.getId(), maxRetryCount, exception);
        return convertToVO(depositLog);
    }

    private void updateConsultationConclusion(Long consultationId, BlockchainDepositLog depositLog) {
        ConsultationConclusion conclusion = conclusionRepository.findByConsultationId(consultationId).orElse(null);
        if (conclusion != null) {
            conclusion.setDepositHash(depositLog.getFileHash());
            conclusion.setDepositTimestamp(depositLog.getDepositTimestamp());
            conclusion.setDepositTxHash(depositLog.getTxHash());
            conclusion.setDepositStatus(depositLog.getDepositStatus());
            conclusion.setDepositRetryCount(depositLog.getRetryCount());
            conclusion.setDepositProvider(depositLog.getDepositProvider());
            conclusion.setDepositBlockHeight(depositLog.getBlockHeight());
            conclusion.setDepositBlockTime(depositLog.getBlockTime());
            conclusion.setDepositVerifyUrl(depositLog.getDepositVerifyUrl());
            conclusionRepository.save(conclusion);
        }
    }

    private BlockchainDepositRequestDTO buildDepositRequest(Consultation consultation, byte[] pdfBytes, String fileHash) {
        BlockchainDepositRequestDTO request = new BlockchainDepositRequestDTO();
        request.setConsultationId(consultation.getId());
        request.setConsultationNo(consultation.getConsultationNo());
        request.setFileHash(fileHash);
        request.setHashAlgorithm(hashAlgorithm);
        request.setPdfUrl("conclusion-pdf/" + consultation.getConsultationNo() + ".pdf");

        Patient patient = patientRepository.findById(consultation.getPatientId()).orElse(null);
        if (patient != null) {
            request.setPatientName(patient.getName());
            request.setPatientIdCard(patient.getIdCard());
        }

        List<ConsultationSignature> signatures = signatureRepository
                .findByConsultationIdOrderBySignOrderAsc(consultation.getId());
        if (!signatures.isEmpty()) {
            ConsultationSignature lastSignature = signatures.stream()
                    .filter(s -> s.getSignTime() != null)
                    .max(Comparator.comparing(ConsultationSignature::getSignTime))
                    .orElse(signatures.get(signatures.size() - 1));

            Doctor doctor = doctorRepository.findById(lastSignature.getDoctorId()).orElse(null);
            if (doctor != null) {
                User user = userRepository.findById(doctor.getUserId()).orElse(null);
                if (user != null) {
                    request.setDoctorName(user.getRealName());
                }
            }
            request.setSignTime(lastSignature.getSignTime());
        }

        request.setExtraData(java.util.Map.of(
                "fileSize", pdfBytes.length,
                "signatureCount", signatures.size()
        ));

        return request;
    }

    private String calculateFileHash(byte[] pdfBytes) {
        try (ByteArrayInputStream inputStream = new ByteArrayInputStream(pdfBytes)) {
            return HashUtil.calculateHash(pdfBytes, hashAlgorithm);
        } catch (Exception e) {
            throw new BusinessException("计算文件哈希失败: " + e.getMessage());
        }
    }

    @Override
    public BlockchainDepositVO getDepositInfo(Long consultationId) {
        BlockchainDepositLog log = depositLogRepository.findByConsultationId(consultationId)
                .orElseThrow(() -> new BusinessException("存证记录不存在"));
        return convertToVO(log);
    }

    @Override
    public BlockchainDepositVO getDepositInfoByConsultationNo(String consultationNo) {
        BlockchainDepositLog log = depositLogRepository.findByConsultationNo(consultationNo)
                .orElseThrow(() -> new BusinessException("存证记录不存在"));
        return convertToVO(log);
    }

    @Override
    public BlockchainDepositVO getDepositInfoByTxHash(String txHash) {
        BlockchainDepositLog log = depositLogRepository.findByTxHash(txHash)
                .orElseThrow(() -> new BusinessException("存证记录不存在"));
        return convertToVO(log);
    }

    @Override
    public BlockchainDepositVO getDepositInfoByFileHash(String fileHash) {
        BlockchainDepositLog log = depositLogRepository.findByFileHash(fileHash)
                .orElseThrow(() -> new BusinessException("存证记录不存在"));
        return convertToVO(log);
    }

    @Override
    public boolean verifyFileHash(String fileHash) {
        Optional<BlockchainDepositLog> logOpt = depositLogRepository.findByFileHash(fileHash);
        if (logOpt.isEmpty()) {
            return false;
        }

        BlockchainDepositLog log = logOpt.get();
        if (log.getDepositStatus() != DepositStatus.SUCCESS.getCode()) {
            return false;
        }

        try {
            BlockchainDepositStrategy strategy = getActiveStrategy();
            BlockchainDepositResponseDTO response = strategy.queryByFileHash(fileHash);
            return response.isSuccess();
        } catch (Exception e) {
            log.warn("链上查询失败，使用本地记录验证, fileHash: {}", fileHash);
            return true;
        }
    }

    @Override
    @Transactional
    public void retryFailedDeposits() {
        log.info("开始批量重试失败的存证");
        List<BlockchainDepositLog> failedLogs = depositLogRepository
                .findByDepositStatusAndRetryCountLessThan(DepositStatus.FAILED.getCode(), maxRetryCount);

        for (BlockchainDepositLog log : failedLogs) {
            try {
                log.info("重试存证, consultationId: {}, 已重试{}次", log.getConsultationId(), log.getRetryCount());
                Consultation consultation = consultationRepository.findById(log.getConsultationId()).orElse(null);
                if (consultation == null) {
                    log.warn("会诊不存在,跳过重试, consultationId: {}", log.getConsultationId());
                    continue;
                }

                byte[] pdfBytes = signatureService.getFinalPdf(log.getConsultationId());
                executeDepositWithRetry(log, pdfBytes, consultation);
            } catch (Exception e) {
                log.error("重试存证失败, consultationId: {}", log.getConsultationId(), e);
            }
        }
        log.info("批量重试存证完成, 共处理{}条", failedLogs.size());
    }

    @Override
    public String generateVerifyUrl(String fileHash) {
        return verifyBaseUrl + "?hash=" + fileHash;
    }

    private BlockchainDepositVO convertToVO(BlockchainDepositLog log) {
        BlockchainDepositVO vo = new BlockchainDepositVO();
        vo.setId(log.getId());
        vo.setConsultationId(log.getConsultationId());
        vo.setConsultationNo(log.getConsultationNo());
        vo.setPdfUrl(log.getPdfUrl());
        vo.setFileHash(log.getFileHash());
        vo.setHashAlgorithm(log.getHashAlgorithm());
        vo.setDepositProvider(log.getDepositProvider());
        vo.setDepositStatus(log.getDepositStatus());
        vo.setDepositStatusText(DepositStatus.getDescriptionByCode(log.getDepositStatus()));
        vo.setTxHash(log.getTxHash());
        vo.setBlockHeight(log.getBlockHeight());
        vo.setBlockTime(log.getBlockTime());
        vo.setDepositTimestamp(log.getDepositTimestamp());
        vo.setDepositVerifyUrl(log.getDepositVerifyUrl());
        vo.setRetryCount(log.getRetryCount());
        vo.setErrorMessage(log.getErrorMessage());
        vo.setCreateTime(log.getCreateTime());
        vo.setUpdateTime(log.getUpdateTime());
        return vo;
    }
}
