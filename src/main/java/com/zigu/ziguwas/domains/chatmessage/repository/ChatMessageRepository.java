package com.zigu.ziguwas.domains.chatmessage.repository;

import com.zigu.ziguwas.domains.chatmessage.entity.ChatMessage;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ChatMessageRepository extends JpaRepository<ChatMessage, String> {
}
