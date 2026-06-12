package com.telemed.service;

import com.telemed.common.dto.signature.ConsultationSignDTO;
import com.telemed.common.dto.signature.PdfGenerateDTO;

import java.io.InputStream;

public interface PdfService {

    byte[] generateConsultationPdf(PdfGenerateDTO dto);

    byte[] addSignatureImage(byte[] pdfBytes, byte[] signatureImage, float x, float y, float width, float height, int pageNum);

    byte[] addDigitalSignature(byte[] pdfBytes, String privateKeyHex, String publicKeyHex,
                                String reason, String location, String signerName,
                                float x, float y, float width, float height, int pageNum);

    byte[] addDoctorSignature(byte[] pdfBytes, ConsultationSignDTO signDTO, String doctorName);

    InputStream getPdfInputStream(String bucketName, String objectName);

    String savePdfToMinio(byte[] pdfBytes, String consultationNo);
}
