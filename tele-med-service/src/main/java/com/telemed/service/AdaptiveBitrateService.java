package com.telemed.service;

import com.telemed.common.constant.VideoResolution;
import com.telemed.common.dto.mediasoup.QualityReportDTO;
import com.telemed.common.vo.mediasoup.QualityAdviceVO;

public interface AdaptiveBitrateService {

    void reportQuality(QualityReportDTO dto);

    QualityAdviceVO getQualityAdvice(Long consultationId, Long userId, String kind);

    VideoResolution recommendResolution(Long consultationId, Long userId);

    void cleanupOldReports();
}
