package com.telemed.service;

import com.telemed.common.dto.SignalingMessage;
import com.telemed.common.dto.mediasoup.ConsumerCreateDTO;
import com.telemed.common.dto.mediasoup.ProducerCreateDTO;
import com.telemed.common.dto.mediasoup.QualityReportDTO;
import com.telemed.common.dto.mediasoup.TransportCreateDTO;
import com.telemed.common.vo.mediasoup.ConsumerVO;
import com.telemed.common.vo.mediasoup.NearestNodeVO;
import com.telemed.common.vo.mediasoup.ProducerVO;
import com.telemed.common.vo.mediasoup.QualityAdviceVO;
import com.telemed.common.vo.mediasoup.RouterRtpCapabilitiesVO;
import com.telemed.common.vo.mediasoup.TransportConnectVO;
import com.telemed.common.vo.mediasoup.TurnServerVO;
import org.springframework.web.socket.WebSocketSession;

public interface SignalingService {

    String createJanusRoom(String roomName);

    void destroyJanusRoom(String roomId);

    void relaySignaling(SignalingMessage message);

    void registerSession(String userId, WebSocketSession session);

    void removeSession(String userId);

    WebSocketSession getSession(String userId);

    NearestNodeVO getNearestMediasoupNode(String clientIp, String clientRegion, String preferredRegion);

    TurnServerVO getTurnServerConfig();

    RouterRtpCapabilitiesVO getRouterRtpCapabilities(Long nodeId);

    TransportConnectVO createWebRtcTransport(Long nodeId, TransportCreateDTO dto);

    ProducerVO createProducer(Long nodeId, ProducerCreateDTO dto);

    ConsumerVO createConsumer(Long nodeId, ConsumerCreateDTO dto);

    void closeProducer(Long nodeId, String producerId);

    void closeConsumer(Long nodeId, String consumerId);

    QualityAdviceVO reportQualityAndGetAdvice(QualityReportDTO dto);

    String createMediasoupRoom(String roomName);

    void destroyMediasoupRoom(String roomId);
}
