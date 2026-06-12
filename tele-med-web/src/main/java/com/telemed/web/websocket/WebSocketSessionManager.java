package com.telemed.web.websocket;

import com.telemed.service.SignalingService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@Component
@RequiredArgsConstructor
public class WebSocketSessionManager {

    private final SignalingService signalingService;

    private final ConcurrentHashMap<String, String> userSessionMap = new ConcurrentHashMap<>();

    private final ConcurrentHashMap<String, Set<String>> roomUsersMap = new ConcurrentHashMap<>();

    public void register(String userId, String sessionId) {
        userSessionMap.put(userId, sessionId);
    }

    public void remove(String userId) {
        userSessionMap.remove(userId);
        roomUsersMap.values().forEach(set -> set.remove(userId));
    }

    public String getSessionId(String userId) {
        return userSessionMap.get(userId);
    }

    public boolean isOnline(String userId) {
        return userSessionMap.containsKey(userId);
    }

    public void joinRoom(String roomId, String userId) {
        if (roomId == null || userId == null) return;
        roomUsersMap.computeIfAbsent(roomId, k -> ConcurrentHashMap.newKeySet()).add(userId);
    }

    public void leaveRoom(String roomId, String userId) {
        if (roomId == null || userId == null) return;
        Set<String> users = roomUsersMap.get(roomId);
        if (users != null) {
            users.remove(userId);
            if (users.isEmpty()) {
                roomUsersMap.remove(roomId);
            }
        }
    }

    public Set<String> getRoomUsers(String roomId) {
        if (roomId == null) return Collections.emptySet();
        Set<String> users = roomUsersMap.get(roomId);
        return users != null ? Collections.unmodifiableSet(users) : Collections.emptySet();
    }
}
