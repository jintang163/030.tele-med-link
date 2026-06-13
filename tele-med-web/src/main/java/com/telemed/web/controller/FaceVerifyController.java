package com.telemed.web.controller;

import com.telemed.common.dto.faceverify.FaceVerifyRequestDTO;
import com.telemed.common.result.Result;
import com.telemed.common.vo.faceverify.FaceVerifyResultVO;
import com.telemed.common.vo.faceverify.FaceVerifyStatusVO;
import com.telemed.service.FaceVerifyService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/face-verify")
public class FaceVerifyController {

    @Autowired
    private FaceVerifyService faceVerifyService;

    @PostMapping("/verify")
    public Result<FaceVerifyResultVO> verify(@RequestBody FaceVerifyRequestDTO requestDTO,
                                             HttpServletRequest request) {
        if (requestDTO.getVerifySource() == null || requestDTO.getVerifySource().isEmpty()) {
            String ip = request.getHeader("X-Forwarded-For");
            if (ip == null || ip.isEmpty()) {
                ip = request.getRemoteAddr();
            }
            requestDTO.setVerifySource("patient-mini");
        }
        FaceVerifyResultVO result = faceVerifyService.verify(requestDTO);
        return Result.ok(result);
    }

    @GetMapping("/status/{patientId}")
    public Result<FaceVerifyStatusVO> getStatus(@PathVariable Long patientId) {
        FaceVerifyStatusVO status = faceVerifyService.getStatus(patientId);
        return Result.ok(status);
    }

    @PostMapping("/unlock")
    public Result<FaceVerifyResultVO> unlockPatient(@RequestBody Map<String, Object> params) {
        Long patientId = Long.valueOf(params.get("patientId").toString());
        Long operatorId = params.get("operatorId") != null
                ? Long.valueOf(params.get("operatorId").toString()) : null;
        String reason = params.get("reason") != null ? params.get("reason").toString() : null;
        FaceVerifyResultVO result = faceVerifyService.unlockPatient(patientId, operatorId, reason);
        return Result.ok(result);
    }
}
