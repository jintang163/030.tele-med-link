package com.telemed.service;

import com.telemed.common.dto.SignalingMessage;
import org.springframework.web.socket.WebSocketSession;

public interface SignalingService {

    String createJanusRoom(String roomName);

    void destroyJanusRoom(String roomId);

    void relaySignaling(SignalingMessage message);

    void registerSession(String userId, WebSocketSession session);

    void removeSession(String userId);

    WebSocketSession getSession(String userId);
}
