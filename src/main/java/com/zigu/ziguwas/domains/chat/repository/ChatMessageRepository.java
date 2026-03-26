package com.zigu.ziguwas.domains.chat.repository;

import com.zigu.ziguwas.domains.chat.entity.ChatMessage;
import com.zigu.ziguwas.domains.chat.entity.ChatRoom;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ChatMessageRepository extends JpaRepository<ChatMessage, Long> {

    /**
     * 가장 최근에 보낸 메시지 조회
     *
     * @param chatRoom 채팅방
     * @return 가장 최근의 메시지 엔티티
     */
    Optional<ChatMessage> findFirstByChatRoomOrderByTimestampDesc(ChatRoom chatRoom);
}
