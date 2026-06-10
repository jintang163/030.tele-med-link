package com.telemed.service;

import com.telemed.model.entity.ChatMessage;

import java.util.List;

public interface ChatService {

    ChatMessage sendMessage(Long consultationId, Long senderId, String senderRole, String content);

    List<ChatMessage> getConsultationMessages(Long consultationId);
}
