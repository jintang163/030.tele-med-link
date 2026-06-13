package com.telemed.service;

import com.telemed.common.dto.blockchain.BlockchainDepositRequestDTO;
import com.telemed.common.dto.blockchain.BlockchainDepositResponseDTO;

public interface BlockchainDepositStrategy {

    String getProviderName();

    BlockchainDepositResponseDTO deposit(BlockchainDepositRequestDTO request);

    BlockchainDepositResponseDTO queryByTxHash(String txHash);

    BlockchainDepositResponseDTO queryByFileHash(String fileHash);
}
