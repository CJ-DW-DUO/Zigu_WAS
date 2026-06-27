package com.zigu.ziguwas.domains.chat.repository;

import com.zigu.ziguwas.domains.chat.entity.ChatMessage;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.Optional;

public interface ChatMessageRepository extends MongoRepository<ChatMessage, String> {

    /**
     * 가장 최근에 보낸 메시지 조회
     *
     * @param chatRoomId 채팅방ID
     * @return 가장 최근의 메시지 엔티티
     */
    Optional<ChatMessage> findFirstByChatRoomIdOrderByTimestampDesc(String chatRoomId);


    /**
     * 특정 채팅방의 메시지를 페이징하여 조회
     *
     * @param chatRoomId 채팅방ID
     * @param pageable 페이지 번호와 페이지 사이즈
     * @return 페이지 사이즈에 맞는 채팅 메시지들
     */
    Slice<ChatMessage> findByChatRoomId(String chatRoomId, Pageable pageable);
}
