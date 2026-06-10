package com.telemed.model.repository;

import com.telemed.model.entity.ChatMessage;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ChatMessageRepository extends JpaRepository<ChatMessage, Long> {

    List<ChatMessage> findByConsultationIdOrderByCreateTimeAsc(Long consultationId);
}
