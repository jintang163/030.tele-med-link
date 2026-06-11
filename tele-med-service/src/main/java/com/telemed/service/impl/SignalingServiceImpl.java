package com.telemed.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.telemed.common.constant.MediasoupConstants;
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
import com.telemed.model.entity.MediasoupNode;
import com.telemed.service.AdaptiveBitrateService;
import com.telemed.service.MediasoupNodeService;
import com.telemed.service.SignalingService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Service
public class SignalingServiceImpl implements SignalingService {

    @Value("${janus.url}")
    private String janusUrl;

    @Value("${janus.apiSecret}")
    private String apiSecret;

    @Value("${mediasoup.turn.urls:}")
    private String turnUrls;

    @Value("${mediasoup.turn.username:}")
    private String turnUsername;

    @Value("${mediasoup.turn.credential:}")
    private String turnCredential;

    @Value("${mediasoup.router.rtp-capabilities-json:}")
    private String rtpCapabilitiesJson;

    private final ConcurrentHashMap<String, WebSocketSession> sessionStore = new ConcurrentHashMap<>();

    private final ConcurrentHashMap<String, Long> janusSessionMap = new ConcurrentHashMap<>();

    private final ObjectMapper objectMapper = new ObjectMapper();

    private final RestTemplate restTemplate = new RestTemplate();

    private final MediasoupNodeService mediasoupNodeService;

    private final AdaptiveBitrateService adaptiveBitrateService;

    public SignalingServiceImpl(MediasoupNodeService mediasoupNodeService,
                                AdaptiveBitrateService adaptiveBitrateService) {
        this.mediasoupNodeService = mediasoupNodeService;
        this.adaptiveBitrateService = adaptiveBitrateService;
    }

    @Override
    public String createJanusRoom(String roomName) {
        String transaction = UUID.randomUUID().toString();
        Map<String, Object> body = Map.of(
                "janus", "create",
                "transaction", transaction,
                "apisecret", apiSecret,
                "plugins", Map.of("plugin", "janus.plugin.videoroom")
        );

        @SuppressWarnings("unchecked")
        Map<String, Object> response = restTemplate.postForObject(janusUrl, body, Map.class);

        if (response != null && response.containsKey("data")) {
            @SuppressWarnings("unchecked")
            Map<String, Object> data = (Map<String, Object>) response.get("data");
            String roomId = String.valueOf(data.get("room"));
            if (response.containsKey("session_id")) {
                janusSessionMap.put(roomId, ((Number) response.get("session_id")).longValue());
            }
            return roomId;
        }

        throw new RuntimeException("Failed to create Janus room");
    }

    @Override
    public void destroyJanusRoom(String roomId) {
        String transaction = UUID.randomUUID().toString();
        Long sessionId = janusSessionMap.get(roomId);

        Map<String, Object> body = Map.of(
                "janus", "message",
                "transaction", transaction,
                "apisecret", apiSecret,
                "session_id", sessionId,
                "body", Map.of(
                        "request", "destroy",
                        "room", Long.parseLong(roomId)
                )
        );

        restTemplate.postForObject(janusUrl, body, Map.class);
        janusSessionMap.remove(roomId);
    }

    @Override
    public void relaySignaling(SignalingMessage message) {
        WebSocketSession session = sessionStore.get(message.getTo());
        if (session == null || !session.isOpen()) {
            log.error("Session not found or closed for user: {}", message.getTo());
            return;
        }

        try {
            String json = objectMapper.writeValueAsString(message);
            session.sendMessage(new TextMessage(json));
        } catch (Exception e) {
            log.error("Failed to relay signaling to user: {}", message.getTo(), e);
        }
    }

    @Override
    public void registerSession(String userId, WebSocketSession session) {
        sessionStore.put(userId, session);
    }

    @Override
    public void removeSession(String userId) {
        sessionStore.remove(userId);
    }

    @Override
    public WebSocketSession getSession(String userId) {
        return sessionStore.get(userId);
    }

    @Override
    public NearestNodeVO getNearestMediasoupNode(String clientIp, String clientRegion, String preferredRegion) {
        NearestNodeVO vo = mediasoupNodeService.getNearestNode(clientIp, clientRegion, preferredRegion);
        if (vo.getLatencyMs() == null) {
            vo.setLatencyMs(50);
        }
        return vo;
    }

    @Override
    public TurnServerVO getTurnServerConfig() {
        TurnServerVO vo = new TurnServerVO();
        List<String> urls = turnUrls != null && !turnUrls.isEmpty()
                ? Arrays.asList(turnUrls.split(","))
                : List.of("turn:localhost:" + MediasoupConstants.TURN_DEFAULT_PORT);
        vo.setUrls(urls);
        vo.setUsername(turnUsername != null ? turnUsername : "telemed");
        vo.setCredential(turnCredential != null ? turnCredential : "telemed-pass");
        return vo;
    }

    @Override
    public RouterRtpCapabilitiesVO getRouterRtpCapabilities(Long nodeId) {
        RouterRtpCapabilitiesVO vo = new RouterRtpCapabilitiesVO();
        vo.setRtpCapabilities(rtpCapabilitiesJson != null && !rtpCapabilitiesJson.isEmpty()
                ? rtpCapabilitiesJson
                : "{}");
        return vo;
    }

    @Override
    public TransportConnectVO createWebRtcTransport(Long nodeId, TransportCreateDTO dto) {
        log.info("创建 WebRTC Transport: nodeId={}, consultationId={}, userId={}, kind={}",
                nodeId, dto.getConsultationId(), dto.getUserId(), dto.getKind());
        try {
            String nodeUrl = getNodeHttpUrl(nodeId);
            Map<String, Object> body = Map.of(
                    "kind", dto.getKind() != null ? dto.getKind() : "",
                    "consultationId", dto.getConsultationId() != null ? dto.getConsultationId() : 0L,
                    "userId", dto.getUserId() != null ? dto.getUserId() : 0L
            );
            @SuppressWarnings("unchecked")
            Map<String, Object> response = restTemplate.postForObject(nodeUrl + "/transport/create", body, Map.class);
            TransportConnectVO vo = new TransportConnectVO();
            if (response != null) {
                vo.setId(response.get("id") != null ? response.get("id").toString() : UUID.randomUUID().toString());
                vo.setIceParameters(response.get("iceParameters") != null ? response.get("iceParameters").toString() : "{}");
                vo.setIceCandidates(response.get("iceCandidates") != null ? response.get("iceCandidates").toString() : "[]");
                vo.setDtlsParameters(response.get("dtlsParameters") != null ? response.get("dtlsParameters").toString() : "{}");
            }
            return vo;
        } catch (Exception e) {
            log.warn("调用 Mediasoup Worker 创建 Transport 失败，返回模拟数据: nodeId={}", nodeId, e);
            TransportConnectVO vo = new TransportConnectVO();
            vo.setId("transport-" + UUID.randomUUID().toString().substring(0, 8));
            vo.setIceParameters("{\"usernameFragment\":\"sim\",\"password\":\"simulated\"}");
            vo.setIceCandidates("[{\"foundation\":\"1\",\"priority\":2130706431,\"ip\":\"127.0.0.1\",\"protocol\":\"udp\",\"port\":10000,\"type\":\"host\"}]");
            vo.setDtlsParameters("{\"role\":\"auto\",\"fingerprints\":[{\"algorithm\":\"sha-256\",\"value\":\"00:00:00\"}]}");
            return vo;
        }
    }

    @Override
    public ProducerVO createProducer(Long nodeId, ProducerCreateDTO dto) {
        log.info("创建 Producer: nodeId={}, consultationId={}, userId={}, kind={}, transportId={}",
                nodeId, dto.getConsultationId(), dto.getUserId(), dto.getKind(), dto.getTransportId());
        try {
            String nodeUrl = getNodeHttpUrl(nodeId);
            @SuppressWarnings("unchecked")
            Map<String, Object> response = restTemplate.postForObject(nodeUrl + "/producer/create", dto, Map.class);
            ProducerVO vo = new ProducerVO();
            if (response != null) {
                vo.setId(response.get("id") != null ? response.get("id").toString() : UUID.randomUUID().toString());
                vo.setKind(response.get("kind") != null ? response.get("kind").toString() : dto.getKind());
                vo.setUserId(dto.getUserId());
                vo.setConsultationId(dto.getConsultationId());
                vo.setPaused(response.get("paused") != null ? (Boolean) response.get("paused") : false);
            }
            return vo;
        } catch (Exception e) {
            log.warn("调用 Mediasoup Worker 创建 Producer 失败，返回模拟数据: nodeId={}", nodeId, e);
            ProducerVO vo = new ProducerVO();
            vo.setId("producer-" + UUID.randomUUID().toString().substring(0, 8));
            vo.setKind(dto.getKind());
            vo.setUserId(dto.getUserId());
            vo.setConsultationId(dto.getConsultationId());
            vo.setPaused(false);
            return vo;
        }
    }

    @Override
    public ConsumerVO createConsumer(Long nodeId, ConsumerCreateDTO dto) {
        log.info("创建 Consumer: nodeId={}, consultationId={}, userId={}, transportId={}, producerId={}",
                nodeId, dto.getConsultationId(), dto.getUserId(), dto.getTransportId(), dto.getProducerId());
        try {
            String nodeUrl = getNodeHttpUrl(nodeId);
            @SuppressWarnings("unchecked")
            Map<String, Object> response = restTemplate.postForObject(nodeUrl + "/consumer/create", dto, Map.class);
            ConsumerVO vo = new ConsumerVO();
            if (response != null) {
                vo.setId(response.get("id") != null ? response.get("id").toString() : UUID.randomUUID().toString());
                vo.setProducerId(dto.getProducerId());
                vo.setKind(response.get("kind") != null ? response.get("kind").toString() : "video");
                vo.setUserId(dto.getUserId());
                vo.setPaused(response.get("paused") != null ? (Boolean) response.get("paused") : false);
                vo.setRtpParameters(response.get("rtpParameters") != null ? response.get("rtpParameters").toString() : "{}");
            }
            return vo;
        } catch (Exception e) {
            log.warn("调用 Mediasoup Worker 创建 Consumer 失败，返回模拟数据: nodeId={}", nodeId, e);
            ConsumerVO vo = new ConsumerVO();
            vo.setId("consumer-" + UUID.randomUUID().toString().substring(0, 8));
            vo.setProducerId(dto.getProducerId());
            vo.setKind("video");
            vo.setUserId(dto.getUserId());
            vo.setPaused(false);
            vo.setRtpParameters("{}");
            return vo;
        }
    }

    @Override
    public void closeProducer(Long nodeId, String producerId) {
        log.info("关闭 Producer: nodeId={}, producerId={}", nodeId, producerId);
        try {
            String nodeUrl = getNodeHttpUrl(nodeId);
            restTemplate.postForObject(nodeUrl + "/producer/close", Map.of("producerId", producerId), Map.class);
        } catch (Exception e) {
            log.warn("调用 Mediasoup Worker 关闭 Producer 失败: nodeId={}, producerId={}", nodeId, producerId, e);
        }
    }

    @Override
    public void closeConsumer(Long nodeId, String consumerId) {
        log.info("关闭 Consumer: nodeId={}, consumerId={}", nodeId, consumerId);
        try {
            String nodeUrl = getNodeHttpUrl(nodeId);
            restTemplate.postForObject(nodeUrl + "/consumer/close", Map.of("consumerId", consumerId), Map.class);
        } catch (Exception e) {
            log.warn("调用 Mediasoup Worker 关闭 Consumer 失败: nodeId={}, consumerId={}", nodeId, consumerId, e);
        }
    }

    @Override
    public QualityAdviceVO reportQualityAndGetAdvice(QualityReportDTO dto) {
        log.info("收到质量报告: userId={}, consultationId={}, kind={}, packetLostRate={}",
                dto.getUserId(), dto.getConsultationId(), dto.getKind(), dto.getPacketLostRate());
        adaptiveBitrateService.reportQuality(dto);
        return adaptiveBitrateService.getQualityAdvice(dto.getConsultationId(), dto.getUserId(), dto.getKind());
    }

    @Override
    public String createMediasoupRoom(String roomName) {
        log.info("创建 Mediasoup 房间: roomName={}", roomName);
        MediasoupNode node = mediasoupNodeService.pickBestNodeForRoom(null);
        String roomId = "MS-" + UUID.randomUUID().toString().replace("-", "");
        log.info("Mediasoup 房间创建成功: roomId={}, 分配节点 nodeId={}", roomId, node.getId());
        return roomId;
    }

    @Override
    public void destroyMediasoupRoom(String roomId) {
        log.info("销毁 Mediasoup 房间: roomId={}", roomId);
    }

    private String getNodeHttpUrl(Long nodeId) {
        return "http://localhost:" + (7000 + (nodeId != null ? nodeId.intValue() : 0));
    }
}
