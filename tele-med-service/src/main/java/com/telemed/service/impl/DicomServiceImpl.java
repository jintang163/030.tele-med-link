package com.telemed.service.impl;

import cn.hutool.core.util.IdUtil;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.telemed.common.constant.DicomConstants;
import com.telemed.common.dto.dicom.DicomUploadDTO;
import com.telemed.common.exception.BusinessException;
import com.telemed.common.vo.dicom.DicomImageVO;
import com.telemed.common.vo.dicom.DicomTokenVO;
import com.telemed.model.entity.DicomImage;
import com.telemed.model.repository.DicomImageRepository;
import com.telemed.service.DicomService;
import com.telemed.service.MinioService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class DicomServiceImpl implements DicomService {

    private final MinioService minioService;
    private final DicomImageRepository dicomImageRepository;
    private final RedisTemplate<String, Object> redisTemplate;
    private final ObjectMapper objectMapper;

    @Override
    public DicomImageVO uploadDicom(MultipartFile file, DicomUploadDTO uploadDTO) {
        String objectName = buildObjectName(uploadDTO.getConsultationId(), file.getOriginalFilename());

        String storedName = minioService.uploadFile(DicomConstants.DICOM_BUCKET_NAME, objectName, file);

        DicomImage image = new DicomImage();
        image.setConsultationId(uploadDTO.getConsultationId());
        image.setObjectName(storedName);
        image.setFileName(file.getOriginalFilename());
        image.setPatientName(uploadDTO.getPatientName());
        image.setStudyUid(uploadDTO.getStudyUid());
        image.setSeriesUid(uploadDTO.getSeriesUid());
        image.setInstanceUid(uploadDTO.getInstanceUid());
        image.setModality(uploadDTO.getModality());
        image.setStudyDescription(uploadDTO.getStudyDescription());
        image.setSeriesDescription(uploadDTO.getSeriesDescription());
        image.setSliceIndex(uploadDTO.getSliceIndex());
        image.setUploaderId(uploadDTO.getUploaderId());
        image.setUploaderName(uploadDTO.getUploaderName());
        image.setFileSize(file.getSize());

        DicomImage saved = dicomImageRepository.save(image);

        cacheImageInfo(saved);

        return convertToVO(saved);
    }

    @Override
    public List<DicomImageVO> getConsultationImages(Long consultationId) {
        List<DicomImage> images = dicomImageRepository.findByConsultationIdOrderByUploadTimeAsc(consultationId);
        return images.stream().map(this::convertToVO).collect(Collectors.toList());
    }

    @Override
    public DicomTokenVO generateToken(Long consultationId, Long imageId, Long userId, String userName) {
        DicomImage image = dicomImageRepository.findById(imageId)
                .orElseThrow(() -> new BusinessException("影像不存在"));

        if (!image.getConsultationId().equals(consultationId)) {
            throw new BusinessException("影像不属于当前会诊");
        }

        String token = IdUtil.simpleUUID();
        String tokenKey = DicomConstants.DICOM_TOKEN_PREFIX + token;

        Map<String, Object> tokenData = new HashMap<>();
        tokenData.put("token", token);
        tokenData.put("imageId", imageId);
        tokenData.put("consultationId", consultationId);
        tokenData.put("userId", userId);
        tokenData.put("userName", userName);
        tokenData.put("imageInfo", convertToVO(image));
        tokenData.put("expireTime", System.currentTimeMillis() + DicomConstants.DICOM_TOKEN_EXPIRE_MINUTES * 60 * 1000);

        redisTemplate.opsForValue().set(tokenKey, tokenData, DicomConstants.DICOM_TOKEN_EXPIRE_MINUTES, TimeUnit.MINUTES);

        DicomTokenVO vo = new DicomTokenVO();
        vo.setToken(token);
        vo.setImageId(imageId);
        vo.setConsultationId(consultationId);
        vo.setExpireMinutes(DicomConstants.DICOM_TOKEN_EXPIRE_MINUTES);
        vo.setExpireTime(LocalDateTime.now().plusMinutes(DicomConstants.DICOM_TOKEN_EXPIRE_MINUTES));
        vo.setImageInfo(convertToVO(image));

        return vo;
    }

    @Override
    public DicomTokenVO validateToken(String token) {
        String tokenKey = DicomConstants.DICOM_TOKEN_PREFIX + token;
        Object cached = redisTemplate.opsForValue().get(tokenKey);

        if (cached == null) {
            throw new BusinessException("令牌无效或已过期");
        }

        try {
            @SuppressWarnings("unchecked")
            Map<String, Object> tokenData = objectMapper.convertValue(cached, Map.class);

            Long imageId = Long.valueOf(tokenData.get("imageId").toString());
            Long consultationId = Long.valueOf(tokenData.get("consultationId").toString());

            DicomTokenVO vo = new DicomTokenVO();
            vo.setToken(token);
            vo.setImageId(imageId);
            vo.setConsultationId(consultationId);
            vo.setExpireMinutes(DicomConstants.DICOM_TOKEN_EXPIRE_MINUTES);

            Long expireTs = Long.valueOf(tokenData.get("expireTime").toString());
            vo.setExpireTime(LocalDateTime.now().plusSeconds((expireTs - System.currentTimeMillis()) / 1000));

            if (tokenData.get("imageInfo") != null) {
                DicomImageVO imageVO = objectMapper.convertValue(tokenData.get("imageInfo"), DicomImageVO.class);
                vo.setImageInfo(imageVO);
            }

            return vo;
        } catch (Exception e) {
            log.error("解析令牌数据失败", e);
            throw new BusinessException("令牌数据异常");
        }
    }

    @Override
    public String getImageUrlByToken(String token) {
        DicomTokenVO tokenVO = validateToken(token);
        DicomImage image = dicomImageRepository.findById(tokenVO.getImageId())
                .orElseThrow(() -> new BusinessException("影像不存在"));
        return minioService.getFileUrl(DicomConstants.DICOM_BUCKET_NAME, image.getObjectName());
    }

    @Override
    public String getImageUrlByTokenAndImageId(String token, Long imageId) {
        DicomTokenVO tokenVO = validateToken(token);

        Boolean consultationLevel = false;
        String tokenKey = DicomConstants.DICOM_TOKEN_PREFIX + token;
        Object cached = redisTemplate.opsForValue().get(tokenKey);
        if (cached != null) {
            try {
                @SuppressWarnings("unchecked")
                Map<String, Object> tokenData = objectMapper.convertValue(cached, Map.class);
                consultationLevel = Boolean.TRUE.equals(tokenData.get("consultationLevel"));
            } catch (Exception e) {
                log.warn("解析令牌级别失败", e);
            }
        }

        if (!consultationLevel && !imageId.equals(tokenVO.getImageId())) {
            throw new BusinessException("令牌无权访问该影像");
        }

        DicomImage image = dicomImageRepository.findById(imageId)
                .orElseThrow(() -> new BusinessException("影像不存在"));

        if (!image.getConsultationId().equals(tokenVO.getConsultationId())) {
            throw new BusinessException("影像不属于当前会诊");
        }

        return minioService.getFileUrl(DicomConstants.DICOM_BUCKET_NAME, image.getObjectName());
    }

    @Override
    public DicomImageVO getImageInfoByToken(String token) {
        DicomTokenVO tokenVO = validateToken(token);
        return tokenVO.getImageInfo();
    }

    @Override
    public void deleteImage(Long imageId, Long operatorId) {
        DicomImage image = dicomImageRepository.findById(imageId)
                .orElseThrow(() -> new BusinessException("影像不存在"));

        try {
            minioService.deleteFile(DicomConstants.DICOM_BUCKET_NAME, image.getObjectName());
        } catch (Exception e) {
            log.warn("删除MinIO文件失败: {}", image.getObjectName(), e);
        }

        dicomImageRepository.deleteById(imageId);
        redisTemplate.delete(DicomConstants.DICOM_IMAGE_PREFIX + imageId);
    }

    @Override
    public DicomTokenVO generateConsultationToken(Long consultationId, Long userId, String userName) {
        List<DicomImage> images = dicomImageRepository.findByConsultationIdOrderByUploadTimeAsc(consultationId);

        String token = IdUtil.simpleUUID();
        String tokenKey = DicomConstants.DICOM_TOKEN_PREFIX + token;

        List<DicomImageVO> imageVOs = images.stream().map(this::convertToVO).collect(Collectors.toList());

        Map<String, Object> tokenData = new HashMap<>();
        tokenData.put("token", token);
        tokenData.put("consultationId", consultationId);
        tokenData.put("userId", userId);
        tokenData.put("userName", userName);
        tokenData.put("imageIds", images.stream().map(DicomImage::getId).collect(Collectors.toList()));
        tokenData.put("images", imageVOs);
        tokenData.put("consultationLevel", true);
        tokenData.put("expireTime", System.currentTimeMillis() + DicomConstants.DICOM_TOKEN_EXPIRE_MINUTES * 60 * 1000);

        redisTemplate.opsForValue().set(tokenKey, tokenData, DicomConstants.DICOM_TOKEN_EXPIRE_MINUTES, TimeUnit.MINUTES);

        DicomTokenVO vo = new DicomTokenVO();
        vo.setToken(token);
        vo.setConsultationId(consultationId);
        vo.setExpireMinutes(DicomConstants.DICOM_TOKEN_EXPIRE_MINUTES);
        vo.setExpireTime(LocalDateTime.now().plusMinutes(DicomConstants.DICOM_TOKEN_EXPIRE_MINUTES));

        return vo;
    }

    @Override
    public List<DicomImageVO> getImagesByToken(String token) {
        String tokenKey = DicomConstants.DICOM_TOKEN_PREFIX + token;
        Object cached = redisTemplate.opsForValue().get(tokenKey);

        if (cached == null) {
            throw new BusinessException("令牌无效或已过期");
        }

        try {
            @SuppressWarnings("unchecked")
            Map<String, Object> tokenData = objectMapper.convertValue(cached, Map.class);

            Boolean consultationLevel = (Boolean) tokenData.get("consultationLevel");
            if (consultationLevel != null && consultationLevel) {
                Object imagesObj = tokenData.get("images");
                if (imagesObj != null) {
                    @SuppressWarnings("unchecked")
                    List<Object> imageList = (List<Object>) imagesObj;
                    List<DicomImageVO> result = new ArrayList<>();
                    for (Object img : imageList) {
                        result.add(objectMapper.convertValue(img, DicomImageVO.class));
                    }
                    return result;
                }
            }

            Long imageId = Long.valueOf(tokenData.get("imageId").toString());
            DicomImage image = dicomImageRepository.findById(imageId)
                    .orElseThrow(() -> new BusinessException("影像不存在"));
            return List.of(convertToVO(image));

        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.error("解析令牌影像数据失败", e);
            throw new BusinessException("令牌数据异常");
        }
    }

    private String buildObjectName(Long consultationId, String originalFilename) {
        String timestamp = String.valueOf(System.currentTimeMillis());
        String extension = "";
        if (originalFilename != null && originalFilename.contains(".")) {
            extension = originalFilename.substring(originalFilename.lastIndexOf("."));
        }
        return "consultation/" + consultationId + "/" + timestamp + "_" + IdUtil.fastSimpleUUID() + extension;
    }

    private void cacheImageInfo(DicomImage image) {
        try {
            String key = DicomConstants.DICOM_IMAGE_PREFIX + image.getId();
            redisTemplate.opsForValue().set(key, convertToVO(image), 2, TimeUnit.HOURS);
        } catch (Exception e) {
            log.warn("缓存影像信息失败", e);
        }
    }

    private DicomImageVO convertToVO(DicomImage image) {
        DicomImageVO vo = new DicomImageVO();
        BeanUtils.copyProperties(image, vo);
        return vo;
    }
}
