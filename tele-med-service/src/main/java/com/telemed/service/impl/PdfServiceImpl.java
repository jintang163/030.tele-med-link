package com.telemed.service.impl;

import com.itextpdf.io.font.PdfEncodings;
import com.itextpdf.io.image.ImageData;
import com.itextpdf.io.image.ImageDataFactory;
import com.itextpdf.kernel.colors.ColorConstants;
import com.itextpdf.kernel.font.PdfFont;
import com.itextpdf.kernel.font.PdfFontFactory;
import com.itextpdf.kernel.geom.PageSize;
import com.itextpdf.kernel.geom.Rectangle;
import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfReader;
import com.itextpdf.kernel.pdf.PdfWriter;
import com.itextpdf.kernel.pdf.StampingProperties;
import com.itextpdf.kernel.pdf.canvas.PdfCanvas;
import com.itextpdf.layout.Document;
import com.itextpdf.layout.element.Image;
import com.itextpdf.layout.element.Paragraph;
import com.itextpdf.layout.element.Table;
import com.itextpdf.layout.element.Cell;
import com.itextpdf.layout.properties.TextAlignment;
import com.itextpdf.layout.properties.UnitValue;
import com.itextpdf.signatures.BouncyCastleDigest;
import com.itextpdf.signatures.DigestAlgorithms;
import com.itextpdf.signatures.IExternalSignature;
import com.itextpdf.signatures.PdfSignatureAppearance;
import com.itextpdf.signatures.PdfSigner;
import com.itextpdf.signatures.SignatureUtil;
import com.telemed.common.dto.signature.ConsultationSignDTO;
import com.telemed.common.dto.signature.PdfGenerateDTO;
import com.telemed.common.exception.BusinessException;
import com.telemed.service.MinioService;
import com.telemed.service.PdfService;
import com.telemed.service.Sm2SignatureService;
import lombok.RequiredArgsConstructor;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.security.GeneralSecurityException;
import java.security.PrivateKey;
import java.security.Security;
import java.security.cert.Certificate;
import java.security.cert.X509Certificate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Service
@RequiredArgsConstructor
public class PdfServiceImpl implements PdfService {

    private final Sm2SignatureService sm2SignatureService;
    private final MinioService minioService;

    @Value("${minio.bucketName}")
    private String defaultBucket;

    @Value("${signature.pdf-bucket:tele-med-pdf}")
    private String pdfBucket;

    private static final float PAGE_WIDTH = PageSize.A4.getWidth();
    private static final float PAGE_HEIGHT = PageSize.A4.getHeight();
    private static final float MARGIN_LEFT = 50;
    private static final float MARGIN_RIGHT = 50;
    private static final float MARGIN_TOP = 50;
    private static final float MARGIN_BOTTOM = 50;

    static {
        Security.addProvider(new BouncyCastleProvider());
    }

    @Override
    public byte[] generateConsultationPdf(PdfGenerateDTO dto) {
        try (ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
            PdfWriter writer = new PdfWriter(outputStream);
            PdfDocument pdfDoc = new PdfDocument(writer);
            pdfDoc.setDefaultPageSize(PageSize.A4);
            Document document = new Document(pdfDoc);
            document.setMargins(MARGIN_TOP, MARGIN_RIGHT, MARGIN_BOTTOM, MARGIN_LEFT);

            PdfFont font = getChineseFont();

            Paragraph title = new Paragraph("远程会诊诊断意见书")
                    .setFont(font)
                    .setFontSize(20)
                    .setBold()
                    .setTextAlignment(TextAlignment.CENTER)
                    .setMarginBottom(20);
            document.add(title);

            Table infoTable = new Table(UnitValue.createPercentArray(new float[]{1, 2, 1, 2}))
                    .setWidth(UnitValue.createPercentValue(100))
                    .setMarginBottom(15);

            infoTable.addCell(createCell("会诊编号:", font, true));
            infoTable.addCell(createCell(dto.getConsultationNo(), font, false));
            infoTable.addCell(createCell("日期:", font, true));
            infoTable.addCell(createCell(LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy年MM月dd日")), font, false));

            infoTable.addCell(createCell("患者姓名:", font, true));
            infoTable.addCell(createCell(dto.getPatientName(), font, false));
            infoTable.addCell(createCell("性别:", font, true));
            infoTable.addCell(createCell(dto.getPatientGender() != null && dto.getPatientGender() == 1 ? "男" : "女", font, false));

            infoTable.addCell(createCell("年龄:", font, true));
            infoTable.addCell(createCell(dto.getPatientAge() != null ? dto.getPatientAge() + "岁" : "", font, false));
            infoTable.addCell(createCell("就诊卡号:", font, true));
            infoTable.addCell(createCell(dto.getPatientMedicalCardNo(), font, false));

            infoTable.addCell(createCell("会诊医院:", font, true));
            infoTable.addCell(createCell(dto.getHospitalName(), font, false));
            infoTable.addCell(createCell("科室:", font, true));
            infoTable.addCell(createCell(dto.getDepartment(), font, false));

            infoTable.addCell(createCell("会诊医生:", font, true));
            infoTable.addCell(createCell(dto.getDoctorName(), font, false));
            infoTable.addCell(createCell("职称:", font, true));
            infoTable.addCell(createCell(dto.getDoctorTitle(), font, false));

            document.add(infoTable);

            Paragraph diagnosisTitle = new Paragraph("诊断结论")
                    .setFont(font)
                    .setFontSize(14)
                    .setBold()
                    .setMarginTop(10)
                    .setMarginBottom(10);
            document.add(diagnosisTitle);

            Paragraph conclusionContent = new Paragraph(dto.getConclusionContent())
                    .setFont(font)
                    .setFontSize(11)
                    .setFirstLineIndent(22)
                    .setLineSpacing(1.5f);
            document.add(conclusionContent);

            if (dto.getImageUrls() != null && !dto.getImageUrls().isEmpty()) {
                document.add(new Paragraph("\n影像截图")
                        .setFont(font)
                        .setFontSize(14)
                        .setBold()
                        .setMarginTop(10)
                        .setMarginBottom(10));

                addImageSection(document, dto.getImageUrls(), font, defaultBucket);
            }

            if (dto.getWhiteboardImageUrls() != null && !dto.getWhiteboardImageUrls().isEmpty()) {
                document.add(new Paragraph("\n白板截图")
                        .setFont(font)
                        .setFontSize(14)
                        .setBold()
                        .setMarginTop(10)
                        .setMarginBottom(10));

                addImageSection(document, dto.getWhiteboardImageUrls(), font, "tele-med-whiteboard");
            }

            float currentY = pdfDoc.getLastPage().getPageSize().getHeight() - 100;
            PdfCanvas canvas = new PdfCanvas(pdfDoc.getLastPage());
            canvas.beginText();
            canvas.setFontAndSize(font, 10);
            canvas.setColor(ColorConstants.BLACK, false);

            float signatureY = 120;

            Paragraph signTitle = new Paragraph("医生签名:")
                    .setFont(font)
                    .setFontSize(11)
                    .setFixedPosition(MARGIN_LEFT, signatureY + 60, 200);
            document.add(signTitle);

            canvas.endText();

            document.close();
            return outputStream.toByteArray();
        } catch (Exception e) {
            throw new BusinessException("生成PDF失败: " + e.getMessage());
        }
    }

    @Override
    public byte[] addSignatureImage(byte[] pdfBytes, byte[] signatureImage, float x, float y, float width, float height, int pageNum) {
        try (ByteArrayInputStream inputStream = new ByteArrayInputStream(pdfBytes);
             ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {

            PdfReader reader = new PdfReader(inputStream);
            PdfWriter writer = new PdfWriter(outputStream);
            PdfDocument pdfDoc = new PdfDocument(reader, writer, new StampingProperties().useAppendMode());

            if (pageNum <= 0 || pageNum > pdfDoc.getNumberOfPages()) {
                pageNum = pdfDoc.getNumberOfPages();
            }

            ImageData imageData = ImageDataFactory.create(signatureImage);
            Image img = new Image(imageData);
            img.scaleAbsolute(width, height);
            img.setFixedPosition(pageNum, x, y);

            Document document = new Document(pdfDoc);
            document.add(img);
            document.close();

            return outputStream.toByteArray();
        } catch (Exception e) {
            throw new BusinessException("添加签名图片失败: " + e.getMessage());
        }
    }

    @Override
    public byte[] addDigitalSignature(byte[] pdfBytes, String privateKeyHex, String publicKeyHex,
                                       String reason, String location, String signerName,
                                       float x, float y, float width, float height, int pageNum) {
        try (ByteArrayInputStream inputStream = new ByteArrayInputStream(pdfBytes);
             ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {

            PdfReader reader = new PdfReader(inputStream);
            PdfSigner signer = new PdfSigner(reader, outputStream, new StampingProperties().useAppendMode());

            PdfSignatureAppearance appearance = signer.getSignatureAppearance();
            appearance.setReason(reason);
            appearance.setLocation(location);
            appearance.setSignerName(signerName);

            Rectangle rect = new Rectangle(x, y, width, height);
            appearance.setPageRect(rect);
            if (pageNum > 0) {
                appearance.setPageNumber(pageNum);
            }

            IExternalSignature externalSignature = new Sm2ExternalSignature(privateKeyHex);

            signer.signDetached(new BouncyCastleDigest(), externalSignature, new Certificate[]{},
                    null, null, null, 0, PdfSigner.CryptoStandard.CMS);

            return outputStream.toByteArray();
        } catch (Exception e) {
            throw new BusinessException("添加数字签名失败: " + e.getMessage());
        }
    }

    @Override
    public byte[] addDoctorSignature(byte[] pdfBytes, ConsultationSignDTO signDTO, String doctorName) {
        byte[] currentPdf = pdfBytes;

        if (signDTO.getSignatureData() != null && !signDTO.getSignatureData().isEmpty()) {
            byte[] signatureImage = decodeBase64Image(signDTO.getSignatureData());
            float x = signDTO.getSignPositionX() != null ? signDTO.getSignPositionX() : 400;
            float y = signDTO.getSignPositionY() != null ? signDTO.getSignPositionY() : 100;
            float width = signDTO.getSignWidth() != null ? signDTO.getSignWidth() : 150;
            float height = signDTO.getSignHeight() != null ? signDTO.getSignHeight() : 60;
            int page = signDTO.getSignPage() != null ? signDTO.getSignPage() : 1;

            currentPdf = addSignatureImage(currentPdf, signatureImage, x, y, width, height, page);
        }

        if (signDTO.getSm2PrivateKey() != null && !signDTO.getSm2PrivateKey().isEmpty()) {
            float x = signDTO.getSignPositionX() != null ? signDTO.getSignPositionX() : 400;
            float y = signDTO.getSignPositionY() != null ? signDTO.getSignPositionY() : 100;
            float width = signDTO.getSignWidth() != null ? signDTO.getSignWidth() : 150;
            float height = signDTO.getSignHeight() != null ? signDTO.getSignHeight() : 60;
            int page = signDTO.getSignPage() != null ? signDTO.getSignPage() : 1;

            currentPdf = addDigitalSignature(currentPdf,
                    signDTO.getSm2PrivateKey(),
                    signDTO.getSm2PublicKey(),
                    signDTO.getSignReason() != null ? signDTO.getSignReason() : "医生电子签名",
                    signDTO.getSignLocation() != null ? signDTO.getSignLocation() : "远程会诊系统",
                    doctorName,
                    x, y, width, height, page);
        }

        return currentPdf;
    }

    @Override
    public InputStream getPdfInputStream(String bucketName, String objectName) {
        byte[] pdfBytes = minioService.downloadFile(bucketName, objectName);
        return new ByteArrayInputStream(pdfBytes);
    }

    @Override
    public String savePdfToMinio(byte[] pdfBytes, String consultationNo) {
        String objectName = "conclusion-pdf/" + consultationNo + ".pdf";
        return minioService.uploadBytes(pdfBucket, objectName, pdfBytes, "application/pdf");
    }

    private PdfFont getChineseFont() {
        try {
            return PdfFontFactory.createFont("STSong-Light", "UniGB-UCS2-H", PdfEncodings.IDENTITY_H);
        } catch (Exception e) {
            try {
                return PdfFontFactory.createFont();
            } catch (Exception ex) {
                throw new BusinessException("创建字体失败: " + ex.getMessage());
            }
        }
    }

    private Cell createCell(String content, PdfFont font, boolean isHeader) {
        Cell cell = new Cell()
                .add(new Paragraph(content != null ? content : "").setFont(font).setFontSize(10))
                .setPadding(5);
        if (isHeader) {
            cell.setBackgroundColor(ColorConstants.LIGHT_GRAY);
        }
        return cell;
    }

    private void addImageSection(Document document, List<String> imageUrls, PdfFont font, String bucketName) {
        float maxWidth = PAGE_WIDTH - MARGIN_LEFT - MARGIN_RIGHT;
        float maxHeight = 300;
        for (int i = 0; i < imageUrls.size(); i++) {
            String imageUrl = imageUrls.get(i);
            try {
                byte[] imageBytes = minioService.downloadFile(bucketName, imageUrl);
                ImageData imageData = ImageDataFactory.create(imageBytes);
                Image img = new Image(imageData);
                if (img.getImageWidth() > maxWidth) {
                    img.scaleToFit(maxWidth, maxHeight);
                }
                img.setMarginBottom(10);
                document.add(img);

                Paragraph imgDesc = new Paragraph("图" + (i + 1))
                        .setFont(font)
                        .setFontSize(10)
                        .setTextAlignment(TextAlignment.CENTER)
                        .setMarginBottom(15);
                document.add(imgDesc);
            } catch (Exception e) {
            }
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

    private static class Sm2ExternalSignature implements IExternalSignature {

        private final String privateKeyHex;

        public Sm2ExternalSignature(String privateKeyHex) {
            this.privateKeyHex = privateKeyHex;
        }

        @Override
        public String getHashAlgorithm() {
            return "SM3";
        }

        @Override
        public String getEncryptionAlgorithm() {
            return "SM2";
        }

        @Override
        public byte[] sign(byte[] message) throws GeneralSecurityException {
            try {
                String signatureHex = signSm2(message, privateKeyHex);
                return org.bouncycastle.util.encoders.Hex.decode(signatureHex);
            } catch (Exception e) {
                throw new GeneralSecurityException("SM2签名失败", e);
            }
        }

        private String signSm2(byte[] data, String privateKeyHex) {
            try {
                org.bouncycastle.crypto.params.ECDomainParameters domainParams = getSm2DomainParams();
                BigInteger d = new BigInteger(privateKeyHex, 16);
                org.bouncycastle.crypto.params.ECPrivateKeyParameters privateKeyParameters =
                        new org.bouncycastle.crypto.params.ECPrivateKeyParameters(d, domainParams);

                org.bouncycastle.crypto.signers.SM2Signer signer = new org.bouncycastle.crypto.signers.SM2Signer();
                signer.init(true, privateKeyParameters);
                signer.update(data, 0, data.length);
                byte[] signature = signer.generateSignature();

                return org.bouncycastle.util.encoders.Hex.toHexString(signature);
            } catch (Exception e) {
                throw new RuntimeException("SM2签名失败: " + e.getMessage());
            }
        }

        private org.bouncycastle.crypto.params.ECDomainParameters getSm2DomainParams() {
            org.bouncycastle.math.ec.custom.gm.SM2P256V1Curve curve = new org.bouncycastle.math.ec.custom.gm.SM2P256V1Curve();
            BigInteger gx = new BigInteger("32C4AE2C1F1981195F9904466A39C9948FE30BBFF2660BE1715A4589334C74C7", 16);
            BigInteger gy = new BigInteger("BC3736A2F4F6779C59BDCEE36B692153D0A9877CC62A474002DF32E52139F0A0", 16);
            BigInteger n = new BigInteger("FFFFFFFEFFFFFFFFFFFFFFFFFFFFFFFF7203DF6B21C6052B53BBF40939D54123", 16);
            org.bouncycastle.math.ec.ECPoint g = curve.createPoint(gx, gy);
            return new org.bouncycastle.crypto.params.ECDomainParameters(curve, g, n);
        }
    }
}
