package com.zigu.ziguwas.domains.chat.repository;

import com.zigu.ziguwas.domains.chat.entity.ChatMessage;
import com.zigu.ziguwas.domains.chat.entity.ChatRoom;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
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


    /**
     * 특정 채팅방의 메시지를 페이징하여 조회
     *
     * @param chatRoom 채팅방
     * @param pageable 페이지 번호와 페이지 사이즈
     * @return 페이지 사이즈에 맞는 채팅 메시지들
     */
    Slice<ChatMessage> findByChatRoom(ChatRoom chatRoom, Pageable pageable);
}
