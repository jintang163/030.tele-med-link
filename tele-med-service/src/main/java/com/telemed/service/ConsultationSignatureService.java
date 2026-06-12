package com.telemed.service;

import com.telemed.common.dto.signature.ConsultationSignDTO;
import com.telemed.common.dto.signature.PdfGenerateDTO;
import com.telemed.common.vo.signature.ConsultationSignatureVO;
import com.telemed.model.entity.ConsultationSignature;

import java.util.List;

public interface ConsultationSignatureService {

    byte[] generateDraftPdf(Long consultationId, String conclusionContent, List<String> imageUrls);

    ConsultationSignatureVO doctorSign(ConsultationSignDTO signDTO);

    List<ConsultationSignatureVO> getConsultationSignatures(Long consultationId);

    ConsultationSignatureVO getCurrentSigner(Long consultationId);

    String getFinalPdfUrl(Long consultationId);

    byte[] getFinalPdf(Long consultationId);

    boolean isAllSigned(Long consultationId);

    void initSignatureWorkflow(Long consultationId, List<Long> doctorIds);
}
