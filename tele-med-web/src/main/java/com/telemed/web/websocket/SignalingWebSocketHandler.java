package com.telemed.web.websocket;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.telemed.common.dto.SignalingMessage;
import com.telemed.model.entity.ChatMessage;
import com.telemed.model.repository.ChatMessageRepository;
import com.telemed.service.SignalingService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.net.URI;
import java.time.LocalDateTime;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Component
@RequiredArgsConstructor
public class SignalingWebSocketHandler extends TextWebSocketHandler {

    private final SignalingService signalingService;
    private final WebSocketSessionManager sessionManager;
    private final ChatMessageRepository chatMessageRepository;
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
                SignalingMessage joinNotify = new SignalingMessage();
                joinNotify.setType("user-joined");
                joinNotify.setFrom(userId);
                joinNotify.setRoomId(signalingMessage.getRoomId());
                joinNotify.setTimestamp(System.currentTimeMillis());
                broadcastToAll(joinNotify);
                break;

            case "leave":
                sessionManager.remove(userId);
                SignalingMessage leaveNotify = new SignalingMessage();
                leaveNotify.setType("user-left");
                leaveNotify.setFrom(userId);
                leaveNotify.setRoomId(signalingMessage.getRoomId());
                leaveNotify.setTimestamp(System.currentTimeMillis());
                broadcastToAll(leaveNotify);
                break;

            case "chat":
                ChatMessage chatMessage = new ChatMessage();
                chatMessage.setSenderId(Long.parseLong(userId));
                chatMessage.setContent(signalingMessage.getPayload() != null ? signalingMessage.getPayload().toString() : null);
                chatMessage.setConsultationId(signalingMessage.getRoomId() != null ? Long.parseLong(signalingMessage.getRoomId()) : null);
                chatMessage.setCreateTime(LocalDateTime.now());
                chatMessageRepository.save(chatMessage);

                signalingService.relaySignaling(signalingMessage);
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
}
