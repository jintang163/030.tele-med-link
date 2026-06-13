package com.telemed.service;

import com.telemed.common.vo.blockchain.BlockchainDepositVO;

public interface BlockchainDepositService {

    BlockchainDepositVO depositPdf(Long consultationId);

    BlockchainDepositVO getDepositInfo(Long consultationId);

    BlockchainDepositVO getDepositInfoByConsultationNo(String consultationNo);

    BlockchainDepositVO getDepositInfoByTxHash(String txHash);

    BlockchainDepositVO getDepositInfoByFileHash(String fileHash);

    boolean verifyFileHash(String fileHash);

    void retryFailedDeposits();

    String generateVerifyUrl(String fileHash);
}
