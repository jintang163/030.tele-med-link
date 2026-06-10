package com.telemed.service.impl;

import com.telemed.model.entity.ChatMessage;
import com.telemed.model.repository.ChatMessageRepository;
import com.telemed.service.ChatService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ChatServiceImpl implements ChatService {

    private final ChatMessageRepository chatMessageRepository;

    @Override
    public ChatMessage sendMessage(Long consultationId, Long senderId, String senderRole, String content) {
        ChatMessage message = new ChatMessage();
        message.setConsultationId(consultationId);
        message.setSenderId(senderId);
        message.setSenderRole(senderRole);
        message.setContent(content);
        message.setCreateTime(LocalDateTime.now());
        return chatMessageRepository.save(message);
    }

    @Override
    public List<ChatMessage> getConsultationMessages(Long consultationId) {
        return chatMessageRepository.findByConsultationIdOrderByCreateTimeAsc(consultationId);
    }
}
