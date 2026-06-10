package com.telemed.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.telemed.common.dto.SignalingMessage;
import com.telemed.service.SignalingService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;

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

    private final ConcurrentHashMap<String, WebSocketSession> sessionStore = new ConcurrentHashMap<>();

    private final ConcurrentHashMap<String, Long> janusSessionMap = new ConcurrentHashMap<>();

    private final ObjectMapper objectMapper = new ObjectMapper();

    private final RestTemplate restTemplate = new RestTemplate();

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
}
