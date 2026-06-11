package com.telemed.service.impl;

import com.telemed.common.constant.MediasoupConstants;
import com.telemed.common.constant.VideoResolution;
import com.telemed.common.dto.mediasoup.QualityReportDTO;
import com.telemed.common.vo.mediasoup.QualityAdviceVO;
import com.telemed.model.entity.QualityReport;
import com.telemed.model.repository.QualityReportRepository;
import com.telemed.service.AdaptiveBitrateService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Deque;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedDeque;

@Service
@RequiredArgsConstructor
@Slf4j
public class AdaptiveBitrateServiceImpl implements AdaptiveBitrateService {

    private final QualityReportRepository qualityReportRepository;

    private final ConcurrentHashMap<String, StreamStats> streamStatsMap = new ConcurrentHashMap<>();

    private static class StreamStats {
        final Deque<Double> recentPacketLostRates = new ConcurrentLinkedDeque<>();
        volatile long lastStableTimestamp = 0L;
        volatile String lastAdviceResolution = VideoResolution.P_720.getLabel();
    }

    private String buildKey(Long consultationId, Long userId, String kind) {
        return consultationId + "_" + userId + "_" + kind;
    }

    private StreamStats getOrCreateStats(String key) {
        return streamStatsMap.computeIfAbsent(key, k -> new StreamStats());
    }

    @Override
    @Async
    @Transactional
    public void reportQuality(QualityReportDTO dto) {
        QualityReport report = new QualityReport();
        report.setConsultationId(dto.getConsultationId());
        report.setUserId(dto.getUserId());
        report.setUserRole(dto.getUserRole());
        report.setTransportId(dto.getTransportId());
        report.setKind(dto.getKind());
        report.setPacketLostRate(dto.getPacketLostRate());
        report.setJitter(dto.getJitter());
        report.setRoundTripTime(dto.getRoundTripTime());
        report.setBitrate(dto.getBitrate());
        report.setResolution(dto.getResolution());
        qualityReportRepository.save(report);

        String key = buildKey(dto.getConsultationId(), dto.getUserId(), dto.getKind());
        StreamStats stats = getOrCreateStats(key);

        if (dto.getPacketLostRate() != null) {
            stats.recentPacketLostRates.addLast(dto.getPacketLostRate());
            while (stats.recentPacketLostRates.size() > MediasoupConstants.RECENT_REPORT_COUNT) {
                stats.recentPacketLostRates.pollFirst();
            }

            double avgLoss = calculateAvgPacketLostRate(stats);
            long now = System.currentTimeMillis();
            if (avgLoss < MediasoupConstants.PACKET_LOSS_MEDIUM_THRESHOLD) {
                if (stats.lastStableTimestamp == 0L) {
                    stats.lastStableTimestamp = now;
                }
            } else {
                stats.lastStableTimestamp = 0L;
            }

            QualityAdviceVO advice = buildAdvice(dto.getKind(), avgLoss, stats);
            stats.lastAdviceResolution = advice.getResolution();

            log.debug("Quality report saved: consultationId={}, userId={}, kind={}, avgLoss={}, advice={}",
                    dto.getConsultationId(), dto.getUserId(), dto.getKind(), avgLoss, advice);
        }
    }

    private double calculateAvgPacketLostRate(StreamStats stats) {
        if (stats.recentPacketLostRates.isEmpty()) {
            return 0.0;
        }
        double sum = 0.0;
        for (Double rate : stats.recentPacketLostRates) {
            sum += rate;
        }
        return sum / stats.recentPacketLostRates.size();
    }

    @Override
    public QualityAdviceVO getQualityAdvice(Long consultationId, Long userId, String kind) {
        String key = buildKey(consultationId, userId, kind);
        StreamStats stats = streamStatsMap.get(key);

        if (stats == null) {
            Pageable pageable = PageRequest.of(0, MediasoupConstants.RECENT_REPORT_COUNT);
            List<QualityReport> recentReports = qualityReportRepository
                    .findByConsultationIdAndUserIdAndKindOrderByCreateTimeDesc(consultationId, userId, kind, pageable);

            stats = getOrCreateStats(key);
            for (int i = recentReports.size() - 1; i >= 0; i--) {
                QualityReport r = recentReports.get(i);
                if (r.getPacketLostRate() != null) {
                    stats.recentPacketLostRates.addLast(r.getPacketLostRate());
                }
            }
        }

        double avgLoss = calculateAvgPacketLostRate(stats);
        return buildAdvice(kind, avgLoss, stats);
    }

    private QualityAdviceVO buildAdvice(String kind, double avgLoss, StreamStats stats) {
        QualityAdviceVO.QualityAdviceVOBuilder builder = QualityAdviceVO.builder()
                .avgPacketLostRate(avgLoss);

        if (MediasoupConstants.KIND_AUDIO.equals(kind)) {
            return builder
                    .shouldDowngrade(false)
                    .resolution(null)
                    .bitrate(null)
                    .fps(null)
                    .reason("音频优先，不调整")
                    .build();
        }

        VideoResolution recommended;
        String reason;
        boolean shouldDowngrade;

        if (avgLoss > MediasoupConstants.PACKET_LOSS_HIGH_THRESHOLD) {
            recommended = VideoResolution.P_360;
            reason = "丢包率过高";
            shouldDowngrade = true;
        } else if (avgLoss > MediasoupConstants.PACKET_LOSS_MEDIUM_THRESHOLD) {
            recommended = VideoResolution.P_540;
            reason = "网络轻微波动";
            shouldDowngrade = true;
        } else {
            long now = System.currentTimeMillis();
            if (stats.lastStableTimestamp == 0L) {
                stats.lastStableTimestamp = now;
            }
            boolean stableLongEnough = (now - stats.lastStableTimestamp) >= MediasoupConstants.STABLE_DURATION_MS;
            if (stableLongEnough) {
                VideoResolution current = fromLabel(stats.lastAdviceResolution);
                recommended = VideoResolution.upgrade(current);
                reason = "网络稳定，可以升级";
            } else {
                recommended = fromLabel(stats.lastAdviceResolution);
                reason = "网络趋于稳定，观察中";
            }
            shouldDowngrade = false;
        }

        return builder
                .shouldDowngrade(shouldDowngrade)
                .resolution(recommended.getLabel())
                .bitrate(recommended.getBitrate())
                .fps(recommended.getFps())
                .reason(reason)
                .build();
    }

    private VideoResolution fromLabel(String label) {
        if (label == null) {
            return VideoResolution.P_720;
        }
        for (VideoResolution res : VideoResolution.values()) {
            if (res.getLabel().equals(label)) {
                return res;
            }
        }
        return VideoResolution.P_720;
    }

    @Override
    public VideoResolution recommendResolution(Long consultationId, Long userId) {
        QualityAdviceVO advice = getQualityAdvice(consultationId, userId, MediasoupConstants.KIND_VIDEO);
        return fromLabel(advice.getResolution());
    }

    @Override
    @Transactional
    public void cleanupOldReports() {
        LocalDateTime cutoff = LocalDateTime.now().minusHours(MediasoupConstants.REPORT_EXPIRE_HOURS);
        qualityReportRepository.deleteByCreateTimeBefore(cutoff);
        log.info("Cleaned up quality reports older than {} hours", MediasoupConstants.REPORT_EXPIRE_HOURS);
    }
}
