package com.telemed.web.websocket;

import com.telemed.service.SignalingService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.concurrent.ConcurrentHashMap;

@Component
@RequiredArgsConstructor
public class WebSocketSessionManager {

    private final SignalingService signalingService;

    private final ConcurrentHashMap<String, String> userSessionMap = new ConcurrentHashMap<>();

    public void register(String userId, String sessionId) {
        userSessionMap.put(userId, sessionId);
    }

    public void remove(String userId) {
        userSessionMap.remove(userId);
    }

    public String getSessionId(String userId) {
        return userSessionMap.get(userId);
    }

    public boolean isOnline(String userId) {
        return userSessionMap.containsKey(userId);
    }
}
