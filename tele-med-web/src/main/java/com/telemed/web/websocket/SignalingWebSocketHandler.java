package com.telemed.web.websocket;

import com.fasterxml.jackson.databind.ObjectMapper;
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
import com.telemed.model.entity.ChatMessage;
import com.telemed.model.repository.ChatMessageRepository;
import com.telemed.service.SignalingService;
import com.telemed.service.WhiteboardService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.net.URI;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Component
@RequiredArgsConstructor
public class SignalingWebSocketHandler extends TextWebSocketHandler {

    private final SignalingService signalingService;
    private final WebSocketSessionManager sessionManager;
    private final ChatMessageRepository chatMessageRepository;
    private final WhiteboardService whiteboardService;
    private final ObjectMapper objectMapper;

    private final ConcurrentHashMap<String, WebSocketSession> connectedSessions = new ConcurrentHashMap<>();

    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        String userId = extractUserId(session);
        if (userId == null) {
            session.close(CloseStatus.BAD_DATA);
            return;
        }

        signalingService.registerSession(userId, session);
        sessionManager.register(userId, session.getId());
        connectedSessions.put(userId, session);

        SignalingMessage onlineMsg = new SignalingMessage();
        onlineMsg.setType("user-online");
        onlineMsg.setFrom(userId);
        onlineMsg.setTimestamp(System.currentTimeMillis());

        broadcastToAll(onlineMsg);
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
        SignalingMessage signalingMessage = objectMapper.readValue(message.getPayload(), SignalingMessage.class);
        String userId = extractUserId(session);
        signalingMessage.setFrom(userId);

        switch (signalingMessage.getType()) {
            case "offer":
            case "answer":
            case "ice-candidate":
                signalingService.relaySignaling(signalingMessage);
                break;

            case "join":
                sessionManager.register(userId, session.getId());
                sessionManager.joinRoom(signalingMessage.getRoomId(), userId);
                SignalingMessage joinNotify = new SignalingMessage();
                joinNotify.setType("user-joined");
                joinNotify.setFrom(userId);
                joinNotify.setRoomId(signalingMessage.getRoomId());
                joinNotify.setTimestamp(System.currentTimeMillis());
                broadcastToRoom(signalingMessage.getRoomId(), joinNotify);
                break;

            case "leave":
                sessionManager.remove(userId);
                sessionManager.leaveRoom(signalingMessage.getRoomId(), userId);
                SignalingMessage leaveNotify = new SignalingMessage();
                leaveNotify.setType("user-left");
                leaveNotify.setFrom(userId);
                leaveNotify.setRoomId(signalingMessage.getRoomId());
                leaveNotify.setTimestamp(System.currentTimeMillis());
                broadcastToRoom(signalingMessage.getRoomId(), leaveNotify);
                break;

            case "chat":
                ChatMessage chatMessage = new ChatMessage();
                chatMessage.setSenderId(Long.parseLong(userId));
                chatMessage.setContent(signalingMessage.getPayload() != null ? signalingMessage.getPayload().toString() : null);
                chatMessage.setConsultationId(signalingMessage.getRoomId() != null ? Long.parseLong(signalingMessage.getRoomId()) : null);
                chatMessage.setCreateTime(LocalDateTime.now());
                chatMessageRepository.save(chatMessage);

                broadcastToRoom(signalingMessage.getRoomId(), signalingMessage);
                break;

            case "mediasoup-transport-create":
                handleMediasoupTransportCreate(session, signalingMessage, userId);
                break;

            case "mediasoup-transport-connect":
                sendResult(session, signalingMessage.getType(), Map.of("connected", true));
                break;

            case "mediasoup-produce":
                handleMediasoupProduce(session, signalingMessage, userId);
                break;

            case "mediasoup-consume":
                handleMediasoupConsume(session, signalingMessage, userId);
                break;

            case "mediasoup-close-producer":
                handleMediasoupCloseProducer(session, signalingMessage, userId);
                break;

            case "mediasoup-close-consumer":
                handleMediasoupCloseConsumer(session, signalingMessage, userId);
                break;

            case "quality-report":
                handleQualityReport(session, signalingMessage, userId);
                break;

            case "dicom-annotation":
                handleDicomAnnotation(signalingMessage);
                break;

            case "dicom-viewport":
                handleDicomViewport(signalingMessage);
                break;

            case "dicom-image-added":
                handleDicomImageAdded(signalingMessage);
                break;

            case "whiteboard-op":
                handleWhiteboardOp(signalingMessage, userId);
                break;

            case "whiteboard-clear":
                handleWhiteboardClear(signalingMessage, userId);
                break;

            default:
                log.warn("Unknown signaling type: {}", signalingMessage.getType());
        }
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) throws Exception {
        String userId = extractUserId(session);
        if (userId != null) {
            signalingService.removeSession(userId);
            sessionManager.remove(userId);
            connectedSessions.remove(userId);

            SignalingMessage offlineMsg = new SignalingMessage();
            offlineMsg.setType("user-offline");
            offlineMsg.setFrom(userId);
            offlineMsg.setTimestamp(System.currentTimeMillis());
            broadcastToAll(offlineMsg);
        }
    }

    @Override
    public void handleTransportError(WebSocketSession session, Throwable exception) throws Exception {
        log.error("WebSocket transport error for session: {}", session.getId(), exception);
        String userId = extractUserId(session);
        if (userId != null) {
            signalingService.removeSession(userId);
            sessionManager.remove(userId);
            connectedSessions.remove(userId);
        }
        if (session.isOpen()) {
            session.close(CloseStatus.SERVER_ERROR);
        }
    }

    private String extractUserId(WebSocketSession session) {
        URI uri = session.getUri();
        if (uri == null || uri.getQuery() == null) {
            return null;
        }
        String query = uri.getQuery();
        for (String param : query.split("&")) {
            String[] kv = param.split("=", 2);
            if ("userId".equals(kv[0]) && kv.length == 2) {
                return kv[1];
            }
        }
        return null;
    }

    private void broadcastToAll(SignalingMessage message) {
        try {
            String json = objectMapper.writeValueAsString(message);
            for (WebSocketSession s : connectedSessions.values()) {
                if (s.isOpen()) {
                    s.sendMessage(new TextMessage(json));
                }
            }
        } catch (Exception e) {
            log.error("Failed to broadcast message", e);
        }
    }

    private void broadcastToRoom(String roomId, SignalingMessage message) {
        if (roomId == null || roomId.isEmpty()) {
            broadcastToAll(message);
            return;
        }
        try {
            String json = objectMapper.writeValueAsString(message);
            java.util.Set<String> roomUsers = sessionManager.getRoomUsers(roomId);
            if (roomUsers.isEmpty()) {
                for (WebSocketSession s : connectedSessions.values()) {
                    if (s.isOpen()) {
                        s.sendMessage(new TextMessage(json));
                    }
                }
                return;
            }
            for (String uid : roomUsers) {
                WebSocketSession s = connectedSessions.get(uid);
                if (s != null && s.isOpen()) {
                    s.sendMessage(new TextMessage(json));
                }
            }
        } catch (Exception e) {
            log.error("Failed to broadcast message to room: {}", roomId, e);
        }
    }

    @SuppressWarnings("unchecked")
    private void handleMediasoupTransportCreate(WebSocketSession session, SignalingMessage message, String userId) {
        try {
            Map<String, Object> payload = (Map<String, Object>) message.getPayload();
            Long nodeId = payload.get("nodeId") != null ? Long.valueOf(payload.get("nodeId").toString()) : null;
            TransportCreateDTO dto = new TransportCreateDTO();
            dto.setConsultationId(payload.get("consultationId") != null ? Long.valueOf(payload.get("consultationId").toString()) : null);
            dto.setUserId(userId != null ? Long.valueOf(userId) : null);
            dto.setKind(payload.get("kind") != null ? payload.get("kind").toString() : null);
            TransportConnectVO vo = signalingService.createWebRtcTransport(nodeId, dto);
            sendResult(session, message.getType(), vo);
        } catch (Exception e) {
            log.error("处理 mediasoup-transport-create 失败", e);
            sendError(session, message.getType(), e.getMessage());
        }
    }

    @SuppressWarnings("unchecked")
    private void handleMediasoupProduce(WebSocketSession session, SignalingMessage message, String userId) {
        try {
            Map<String, Object> payload = (Map<String, Object>) message.getPayload();
            Long nodeId = payload.get("nodeId") != null ? Long.valueOf(payload.get("nodeId").toString()) : null;
            ProducerCreateDTO dto = new ProducerCreateDTO();
            dto.setConsultationId(payload.get("consultationId") != null ? Long.valueOf(payload.get("consultationId").toString()) : null);
            dto.setUserId(userId != null ? Long.valueOf(userId) : null);
            dto.setTransportId(payload.get("transportId") != null ? payload.get("transportId").toString() : null);
            dto.setKind(payload.get("kind") != null ? payload.get("kind").toString() : null);
            dto.setRtpParameters(payload.get("rtpParameters") != null ? payload.get("rtpParameters").toString() : null);
            ProducerVO vo = signalingService.createProducer(nodeId, dto);
            sendResult(session, message.getType(), vo);
        } catch (Exception e) {
            log.error("处理 mediasoup-produce 失败", e);
            sendError(session, message.getType(), e.getMessage());
        }
    }

    @SuppressWarnings("unchecked")
    private void handleMediasoupConsume(WebSocketSession session, SignalingMessage message, String userId) {
        try {
            Map<String, Object> payload = (Map<String, Object>) message.getPayload();
            Long nodeId = payload.get("nodeId") != null ? Long.valueOf(payload.get("nodeId").toString()) : null;
            ConsumerCreateDTO dto = new ConsumerCreateDTO();
            dto.setConsultationId(payload.get("consultationId") != null ? Long.valueOf(payload.get("consultationId").toString()) : null);
            dto.setUserId(userId != null ? Long.valueOf(userId) : null);
            dto.setTransportId(payload.get("transportId") != null ? payload.get("transportId").toString() : null);
            dto.setProducerId(payload.get("producerId") != null ? payload.get("producerId").toString() : null);
            dto.setRtpCapabilities(payload.get("rtpCapabilities") != null ? payload.get("rtpCapabilities").toString() : null);
            ConsumerVO vo = signalingService.createConsumer(nodeId, dto);
            sendResult(session, message.getType(), vo);
        } catch (Exception e) {
            log.error("处理 mediasoup-consume 失败", e);
            sendError(session, message.getType(), e.getMessage());
        }
    }

    @SuppressWarnings("unchecked")
    private void handleMediasoupCloseProducer(WebSocketSession session, SignalingMessage message, String userId) {
        try {
            Map<String, Object> payload = (Map<String, Object>) message.getPayload();
            Long nodeId = payload.get("nodeId") != null ? Long.valueOf(payload.get("nodeId").toString()) : null;
            String producerId = payload.get("producerId") != null ? payload.get("producerId").toString() : null;
            signalingService.closeProducer(nodeId, producerId);
            sendResult(session, message.getType(), Map.of("closed", true));
        } catch (Exception e) {
            log.error("处理 mediasoup-close-producer 失败", e);
            sendError(session, message.getType(), e.getMessage());
        }
    }

    @SuppressWarnings("unchecked")
    private void handleMediasoupCloseConsumer(WebSocketSession session, SignalingMessage message, String userId) {
        try {
            Map<String, Object> payload = (Map<String, Object>) message.getPayload();
            Long nodeId = payload.get("nodeId") != null ? Long.valueOf(payload.get("nodeId").toString()) : null;
            String consumerId = payload.get("consumerId") != null ? payload.get("consumerId").toString() : null;
            signalingService.closeConsumer(nodeId, consumerId);
            sendResult(session, message.getType(), Map.of("closed", true));
        } catch (Exception e) {
            log.error("处理 mediasoup-close-consumer 失败", e);
            sendError(session, message.getType(), e.getMessage());
        }
    }

    @SuppressWarnings("unchecked")
    private void handleQualityReport(WebSocketSession session, SignalingMessage message, String userId) {
        try {
            Map<String, Object> payload = (Map<String, Object>) message.getPayload();
            QualityReportDTO dto = new QualityReportDTO();
            dto.setUserId(userId != null ? Long.valueOf(userId) : null);
            dto.setConsultationId(payload.get("consultationId") != null ? Long.valueOf(payload.get("consultationId").toString()) : null);
            dto.setTransportId(payload.get("transportId") != null ? payload.get("transportId").toString() : null);
            dto.setKind(payload.get("kind") != null ? payload.get("kind").toString() : null);
            dto.setPacketLostRate(payload.get("packetLostRate") != null ? Double.valueOf(payload.get("packetLostRate").toString()) : null);
            dto.setJitter(payload.get("jitter") != null ? Long.valueOf(payload.get("jitter").toString()) : null);
            dto.setRoundTripTime(payload.get("roundTripTime") != null ? Long.valueOf(payload.get("roundTripTime").toString()) : null);
            dto.setBitrate(payload.get("bitrate") != null ? Long.valueOf(payload.get("bitrate").toString()) : null);
            dto.setResolution(payload.get("resolution") != null ? payload.get("resolution").toString() : null);
            QualityAdviceVO vo = signalingService.reportQualityAndGetAdvice(dto);
            sendResult(session, message.getType(), vo);
        } catch (Exception e) {
            log.error("处理 quality-report 失败", e);
            sendError(session, message.getType(), e.getMessage());
        }
    }

    private void sendResult(WebSocketSession session, String type, Object data) {
        try {
            SignalingMessage response = new SignalingMessage();
            response.setType(type + "-response");
            response.setPayload(data);
            response.setTimestamp(System.currentTimeMillis());
            String json = objectMapper.writeValueAsString(response);
            session.sendMessage(new TextMessage(json));
        } catch (Exception e) {
            log.error("发送 WebSocket 响应失败", e);
        }
    }

    private void sendError(WebSocketSession session, String type, String errorMsg) {
        try {
            SignalingMessage response = new SignalingMessage();
            response.setType(type + "-error");
            response.setPayload(Map.of("error", errorMsg != null ? errorMsg : "Unknown error"));
            response.setTimestamp(System.currentTimeMillis());
            String json = objectMapper.writeValueAsString(response);
            session.sendMessage(new TextMessage(json));
        } catch (Exception e) {
            log.error("发送 WebSocket 错误响应失败", e);
        }
    }

    private void handleDicomAnnotation(SignalingMessage message) {
        try {
            log.info("处理DICOM标注同步: roomId={}, type={}, operator={}",
                    message.getRoomId(),
                    message.getPayload() != null ? ((Map<?, ?>) message.getPayload()).get("annotationType") : null,
                    message.getFrom());
            broadcastToRoom(message.getRoomId(), message);
        } catch (Exception e) {
            log.error("处理DICOM标注同步失败", e);
        }
    }

    private void handleDicomViewport(SignalingMessage message) {
        try {
            log.info("处理DICOM视口同步: roomId={}, operator={}",
                    message.getRoomId(), message.getFrom());
            broadcastToRoom(message.getRoomId(), message);
        } catch (Exception e) {
            log.error("处理DICOM视口同步失败", e);
        }
    }

    private void handleDicomImageAdded(SignalingMessage message) {
        try {
            log.info("处理DICOM影像添加通知: roomId={}, uploader={}",
                    message.getRoomId(), message.getFrom());
            broadcastToRoom(message.getRoomId(), message);
        } catch (Exception e) {
            log.error("处理DICOM影像添加通知失败", e);
        }
    }

    @SuppressWarnings("unchecked")
    private void handleWhiteboardOp(SignalingMessage message, String userId) {
        try {
            Map<String, Object> payload = message.getPayload() != null
                    ? (Map<String, Object>) message.getPayload()
                    : null;

            String roomId = message.getRoomId();
            String source = payload != null && payload.get("source") != null
                    ? payload.get("source").toString()
                    : "BLANK";
            Long imageId = payload != null && payload.get("imageId") != null
                    ? Long.valueOf(payload.get("imageId").toString())
                    : null;

            log.debug("处理白板操作: roomId={}, source={}, imageId={}, operator={}",
                    roomId, source, imageId, userId);

            if (payload != null) {
                com.telemed.common.dto.whiteboard.WhiteboardOpDTO opDTO =
                        objectMapper.convertValue(payload, com.telemed.common.dto.whiteboard.WhiteboardOpDTO.class);
                opDTO.setRoomId(roomId);
                opDTO.setOperatorId(Long.parseLong(userId));
                if (opDTO.getTimestamp() == null) {
                    opDTO.setTimestamp(System.currentTimeMillis());
                }
                whiteboardService.recordOp(opDTO);
            }

            broadcastToRoom(roomId, message);
        } catch (Exception e) {
            log.error("处理白板操作失败", e);
        }
    }

    @SuppressWarnings("unchecked")
    private void handleWhiteboardClear(SignalingMessage message, String userId) {
        try {
            Map<String, Object> payload = message.getPayload() != null
                    ? (Map<String, Object>) message.getPayload()
                    : null;

            String roomId = message.getRoomId();
            String source = payload != null && payload.get("source") != null
                    ? payload.get("source").toString()
                    : "BLANK";
            Long imageId = payload != null && payload.get("imageId") != null
                    ? Long.valueOf(payload.get("imageId").toString())
                    : null;

            log.info("清除白板: roomId={}, source={}, imageId={}, operator={}",
                    roomId, source, imageId, userId);

            whiteboardService.clearHistory(roomId, source, imageId, Long.parseLong(userId));
            broadcastToRoom(roomId, message);
        } catch (Exception e) {
            log.error("清除白板失败", e);
        }
    }
}
