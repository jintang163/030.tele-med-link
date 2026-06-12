package com.telemed.service;

import com.telemed.common.dto.dicom.DicomUploadDTO;
import com.telemed.common.vo.dicom.DicomImageVO;
import com.telemed.common.vo.dicom.DicomTokenVO;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

public interface DicomService {

    DicomImageVO uploadDicom(MultipartFile file, DicomUploadDTO uploadDTO);

    List<DicomImageVO> getConsultationImages(Long consultationId);

    DicomTokenVO generateToken(Long consultationId, Long imageId, Long userId, String userName);

    DicomTokenVO validateToken(String token);

    String getImageUrlByToken(String token);

    String getImageUrlByTokenAndImageId(String token, Long imageId);

    DicomImageVO getImageInfoByToken(String token);

    void deleteImage(Long imageId, Long operatorId);

    DicomTokenVO generateConsultationToken(Long consultationId, Long userId, String userName);

    List<DicomImageVO> getImagesByToken(String token);
}
