package com.telemed.web.controller;

import com.telemed.common.dto.dicom.DicomUploadDTO;
import com.telemed.common.result.Result;
import com.telemed.common.vo.dicom.DicomImageVO;
import com.telemed.common.vo.dicom.DicomTokenVO;
import com.telemed.service.DicomService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/api/dicom")
@RequiredArgsConstructor
public class DicomController {

    private final DicomService dicomService;

    @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public Result<DicomImageVO> upload(
            @RequestPart("file") MultipartFile file,
            @RequestParam Long consultationId,
            @RequestParam Long uploaderId,
            @RequestParam(required = false) String uploaderName,
            @RequestParam(required = false) String patientName,
            @RequestParam(required = false) String studyUid,
            @RequestParam(required = false) String seriesUid,
            @RequestParam(required = false) String instanceUid,
            @RequestParam(required = false) String modality,
            @RequestParam(required = false) String studyDescription,
            @RequestParam(required = false) String seriesDescription,
            @RequestParam(required = false) Integer sliceIndex
    ) {
        DicomUploadDTO uploadDTO = new DicomUploadDTO();
        uploadDTO.setConsultationId(consultationId);
        uploadDTO.setUploaderId(uploaderId);
        uploadDTO.setUploaderName(uploaderName);
        uploadDTO.setPatientName(patientName);
        uploadDTO.setStudyUid(studyUid);
        uploadDTO.setSeriesUid(seriesUid);
        uploadDTO.setInstanceUid(instanceUid);
        uploadDTO.setModality(modality);
        uploadDTO.setStudyDescription(studyDescription);
        uploadDTO.setSeriesDescription(seriesDescription);
        uploadDTO.setSliceIndex(sliceIndex);

        DicomImageVO vo = dicomService.uploadDicom(file, uploadDTO);
        return Result.ok(vo);
    }

    @GetMapping("/consultation/{consultationId}/images")
    public Result<List<DicomImageVO>> getConsultationImages(@PathVariable Long consultationId) {
        List<DicomImageVO> images = dicomService.getConsultationImages(consultationId);
        return Result.ok(images);
    }

    @PostMapping("/token/generate")
    public Result<DicomTokenVO> generateToken(@RequestParam Long consultationId,
                                               @RequestParam Long imageId,
                                               @RequestParam Long userId,
                                               @RequestParam(required = false) String userName) {
        DicomTokenVO vo = dicomService.generateToken(consultationId, imageId, userId, userName);
        return Result.ok(vo);
    }

    @PostMapping("/token/generate-consultation")
    public Result<DicomTokenVO> generateConsultationToken(@RequestParam Long consultationId,
                                                           @RequestParam Long userId,
                                                           @RequestParam(required = false) String userName) {
        DicomTokenVO vo = dicomService.generateConsultationToken(consultationId, userId, userName);
        return Result.ok(vo);
    }

    @GetMapping("/token/validate")
    public Result<DicomTokenVO> validateToken(@RequestParam String token) {
        DicomTokenVO vo = dicomService.validateToken(token);
        return Result.ok(vo);
    }

    @GetMapping("/token/images")
    public Result<List<DicomImageVO>> getImagesByToken(@RequestParam String token) {
        List<DicomImageVO> images = dicomService.getImagesByToken(token);
        return Result.ok(images);
    }

    @GetMapping("/token/url")
    public Result<String> getImageUrlByToken(@RequestParam String token) {
        String url = dicomService.getImageUrlByToken(token);
        return Result.ok(url);
    }

    @GetMapping("/token/url-by-image")
    public Result<String> getImageUrlByTokenAndImageId(@RequestParam String token,
                                                        @RequestParam Long imageId) {
        String url = dicomService.getImageUrlByTokenAndImageId(token, imageId);
        return Result.ok(url);
    }

    @GetMapping("/token/info")
    public Result<DicomImageVO> getImageInfoByToken(@RequestParam String token) {
        DicomImageVO vo = dicomService.getImageInfoByToken(token);
        return Result.ok(vo);
    }

    @DeleteMapping("/image/{imageId}")
    public Result<Void> deleteImage(@PathVariable Long imageId, @RequestParam Long operatorId) {
        dicomService.deleteImage(imageId, operatorId);
        return Result.ok();
    }
}
