package com.telemed.web.controller;

import cn.hutool.core.codec.Base64;
import com.telemed.common.dto.signature.ConsultationSignDTO;
import com.telemed.common.exception.BusinessException;
import com.telemed.common.result.Result;
import com.telemed.common.vo.signature.ConsultationSignatureVO;
import com.telemed.service.ConsultationSignatureService;
import com.telemed.service.Sm2SignatureService;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.io.OutputStream;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.security.KeyPair;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/signature")
public class SignatureController {

    @Autowired
    private ConsultationSignatureService consultationSignatureService;

    @Autowired
    private Sm2SignatureService sm2SignatureService;

    @PostMapping("/generate-key-pair")
    public Result<Map<String, String>> generateKeyPair() {
        KeyPair keyPair = sm2SignatureService.generateKeyPair();
        Map<String, String> result = new HashMap<>();
        result.put("publicKey", sm2SignatureService.getPublicKeyHex(keyPair));
        result.put("privateKey", sm2SignatureService.getPrivateKeyHex(keyPair));
        return Result.ok(result);
    }

    @PostMapping("/sign-data")
    public Result<Map<String, String>> signData(@RequestBody Map<String, String> params) {
        String data = params.get("data");
        String privateKey = params.get("privateKey");
        String signature = sm2SignatureService.sign(data, privateKey);
        Map<String, String> result = new HashMap<>();
        result.put("signature", signature);
        return Result.ok(result);
    }

    @PostMapping("/verify-data")
    public Result<Map<String, Boolean>> verifyData(@RequestBody Map<String, String> params) {
        String data = params.get("data");
        String publicKey = params.get("publicKey");
        String signature = params.get("signature");
        boolean valid = sm2SignatureService.verify(data, publicKey, signature);
        Map<String, Boolean> result = new HashMap<>();
        result.put("valid", valid);
        return Result.ok(result);
    }

    @PostMapping("/sm3-hash")
    public Result<Map<String, String>> sm3Hash(@RequestBody Map<String, String> params) {
        String data = params.get("data");
        String hash = sm2SignatureService.sm3HashHex(data);
        Map<String, String> result = new HashMap<>();
        result.put("hash", hash);
        return Result.ok(result);
    }

    @PostMapping("/draft-pdf")
    public Result<Map<String, String>> generateDraftPdf(@RequestBody Map<String, Object> params) {
        Long consultationId = Long.valueOf(params.get("consultationId").toString());
        String conclusionContent = params.get("conclusionContent") != null ?
                params.get("conclusionContent").toString() : "";
        @SuppressWarnings("unchecked")
        List<String> imageUrls = params.get("imageUrls") != null ?
                (List<String>) params.get("imageUrls") : null;

        byte[] pdfBytes = consultationSignatureService.generateDraftPdf(consultationId, conclusionContent, imageUrls);
        String pdfBase64 = Base64.encode(pdfBytes);

        Map<String, String> result = new HashMap<>();
        result.put("pdfBase64", pdfBase64);
        return Result.ok(result);
    }

    @GetMapping("/patient-pdf-url/{consultationId}")
    public Result<Map<String, Object>> getPatientPdfUrl(@PathVariable Long consultationId) {
        Map<String, Object> result = new HashMap<>();
        try {
            boolean allSigned = consultationSignatureService.isAllSigned(consultationId);
            result.put("allSigned", allSigned);
            if (allSigned) {
                String url = consultationSignatureService.getFinalPdfUrl(consultationId);
                result.put("url", url);
            }
            List<ConsultationSignatureVO> signatures = consultationSignatureService.getConsultationSignatures(consultationId);
            result.put("signatures", signatures);
        } catch (BusinessException e) {
            result.put("allSigned", false);
            result.put("signatures", List.of());
        }
        return Result.ok(result);
    }

    @PostMapping("/doctor-sign")
    public Result<ConsultationSignatureVO> doctorSign(@RequestBody ConsultationSignDTO signDTO) {
        ConsultationSignatureVO vo = consultationSignatureService.doctorSign(signDTO);
        return Result.ok(vo);
    }

    @GetMapping("/consultation/{consultationId}")
    public Result<List<ConsultationSignatureVO>> getConsultationSignatures(@PathVariable Long consultationId) {
        List<ConsultationSignatureVO> list = consultationSignatureService.getConsultationSignatures(consultationId);
        return Result.ok(list);
    }

    @GetMapping("/current-signer/{consultationId}")
    public Result<ConsultationSignatureVO> getCurrentSigner(@PathVariable Long consultationId) {
        ConsultationSignatureVO vo = consultationSignatureService.getCurrentSigner(consultationId);
        return Result.ok(vo);
    }

    @GetMapping("/all-signed/{consultationId}")
    public Result<Map<String, Boolean>> isAllSigned(@PathVariable Long consultationId) {
        boolean allSigned = consultationSignatureService.isAllSigned(consultationId);
        Map<String, Boolean> result = new HashMap<>();
        result.put("allSigned", allSigned);
        return Result.ok(result);
    }

    @GetMapping("/pdf-url/{consultationId}")
    public Result<Map<String, String>> getFinalPdfUrl(@PathVariable Long consultationId) {
        String url = consultationSignatureService.getFinalPdfUrl(consultationId);
        Map<String, String> result = new HashMap<>();
        result.put("url", url);
        return Result.ok(result);
    }

    @GetMapping("/download-pdf/{consultationId}")
    public void downloadPdf(@PathVariable Long consultationId, HttpServletResponse response) {
        try {
            byte[] pdfBytes = consultationSignatureService.getFinalPdf(consultationId);
            response.setContentType("application/pdf");
            response.setContentLength(pdfBytes.length);
            String fileName = "consultation_" + consultationId + ".pdf";
            response.setHeader("Content-Disposition", "attachment; filename=" +
                    URLEncoder.encode(fileName, StandardCharsets.UTF_8.name()));

            try (OutputStream out = response.getOutputStream()) {
                out.write(pdfBytes);
                out.flush();
            }
        } catch (Exception e) {
            throw new RuntimeException("下载PDF失败: " + e.getMessage(), e);
        }
    }

    @PostMapping("/init-workflow")
    public Result<Void> initSignatureWorkflow(@RequestBody Map<String, Object> params) {
        Long consultationId = Long.valueOf(params.get("consultationId").toString());
        @SuppressWarnings("unchecked")
        List<Long> doctorIds = (List<Long>) params.get("doctorIds");
        consultationSignatureService.initSignatureWorkflow(consultationId, doctorIds);
        return Result.ok();
    }
}
