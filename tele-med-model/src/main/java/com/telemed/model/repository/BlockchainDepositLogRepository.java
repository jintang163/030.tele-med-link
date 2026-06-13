package com.telemed.model.repository;

import com.telemed.model.entity.BlockchainDepositLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface BlockchainDepositLogRepository extends JpaRepository<BlockchainDepositLog, Long> {

    Optional<BlockchainDepositLog> findByConsultationId(Long consultationId);

    Optional<BlockchainDepositLog> findByConsultationNo(String consultationNo);

    Optional<BlockchainDepositLog> findByFileHash(String fileHash);

    Optional<BlockchainDepositLog> findByTxHash(String txHash);

    List<BlockchainDepositLog> findByDepositStatus(Integer depositStatus);

    List<BlockchainDepositLog> findByDepositStatusAndRetryCountLessThan(Integer depositStatus, Integer maxRetryCount);

    List<BlockchainDepositLog> findByConsultationIdOrderByCreateTimeDesc(Long consultationId);
}
