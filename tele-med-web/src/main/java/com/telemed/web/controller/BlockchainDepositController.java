package com.telemed.web.controller;

import com.telemed.common.result.Result;
import com.telemed.common.util.HashUtil;
import com.telemed.common.vo.blockchain.BlockchainDepositVO;
import com.telemed.service.BlockchainDepositService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.HashMap;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/blockchain")
@RequiredArgsConstructor
public class BlockchainDepositController {

    private final BlockchainDepositService blockchainDepositService;

    @PostMapping("/deposit/{consultationId}")
    public Result<BlockchainDepositVO> depositPdf(@PathVariable Long consultationId) {
        log.info("手动触发区块链存证, consultationId: {}", consultationId);
        BlockchainDepositVO vo = blockchainDepositService.depositPdf(consultationId);
        return Result.ok(vo);
    }

    @GetMapping("/info/consultation/{consultationId}")
    public Result<BlockchainDepositVO> getDepositInfoByConsultationId(@PathVariable Long consultationId) {
        BlockchainDepositVO vo = blockchainDepositService.getDepositInfo(consultationId);
        return Result.ok(vo);
    }

    @GetMapping("/info/consultation-no/{consultationNo}")
    public Result<BlockchainDepositVO> getDepositInfoByConsultationNo(@PathVariable String consultationNo) {
        BlockchainDepositVO vo = blockchainDepositService.getDepositInfoByConsultationNo(consultationNo);
        return Result.ok(vo);
    }

    @GetMapping("/info/tx/{txHash}")
    public Result<BlockchainDepositVO> getDepositInfoByTxHash(@PathVariable String txHash) {
        BlockchainDepositVO vo = blockchainDepositService.getDepositInfoByTxHash(txHash);
        return Result.ok(vo);
    }

    @GetMapping("/verify")
    public Result<Map<String, Object>> verifyByHash(@RequestParam String hash) {
        log.info("区块链核验, fileHash: {}", hash);
        Map<String, Object> result = new HashMap<>();
        result.put("fileHash", hash);

        boolean verified = blockchainDepositService.verifyFileHash(hash);
        result.put("verified", verified);

        if (verified) {
            try {
                BlockchainDepositVO depositInfo = blockchainDepositService.getDepositInfoByFileHash(hash);
                result.put("depositInfo", depositInfo);
                result.put("message", "文件核验通过，该文件已进行区块链存证");
            } catch (Exception e) {
                result.put("message", "核验通过，但获取详细信息失败");
            }
        } else {
            result.put("message", "核验失败，未找到该文件的存证记录或存证未成功");
        }

        return Result.ok(result);
    }

    @PostMapping("/verify-file")
    public Result<Map<String, Object>> verifyFile(@RequestParam("file") MultipartFile file) {
        log.info("上传文件核验, fileName: {}, size: {}", file.getOriginalFilename(), file.getSize());

        try {
            byte[] fileBytes = file.getBytes();
            String fileHash = HashUtil.sha256Hex(fileBytes);
            log.info("上传文件哈希计算完成, fileHash: {}", fileHash);
            return verifyByHash(fileHash);
        } catch (Exception e) {
            log.error("文件核验失败", e);
            throw new RuntimeException("文件核验失败: " + e.getMessage());
        }
    }

    @PostMapping("/retry-failed")
    public Result<Void> retryFailedDeposits() {
        log.info("手动触发失败存证重试");
        blockchainDepositService.retryFailedDeposits();
        return Result.ok();
    }

    @GetMapping("/generate-hash")
    public Result<Map<String, String>> generateHash(@RequestParam String content) {
        Map<String, String> result = new HashMap<>();
        result.put("sha256", HashUtil.sha256Hex(content));
        result.put("sm3", HashUtil.sm3Hex(content));
        result.put("md5", HashUtil.md5Hex(content.getBytes()));
        return Result.ok(result);
    }
}
