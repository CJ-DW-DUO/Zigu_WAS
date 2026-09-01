package com.zigu.ziguwas.domains.chat.entity;

import lombok.Builder;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Document("chat_messages")
/**
 * name : 인덱스 이름 (Mongo에서 확인할 때 보이는 이름, 식별하기 쉽게만 지으면 됨)
 * def : 어떤 필드를 인덱스로 묶을지 정의, 1은 오름차순, -1은 내림차순
 */
// 채팅방 상세조회 : 특정 채팅방의 메시지를 최신순으로 페이징하고, 조회 하한선(visibleFrom) 이후만 걸러내기 위한 인덱스
@CompoundIndex(name = "idx_chatRoomId_timestamp", def = "{'chatRoomId': 1, 'timestamp': -1}")
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Builder
public class ChatMessage {

    @Id
    private String id;

    // RDB와 NoSQL PK 타입 차이로 인하여 String
    private String chatRoomId;

    // RDB와 NoSQL PK 타입 차이로 인하여 Long
    private Long senderId;

    private String message;

    // 보관기간이 지난 메시지를 찾는 배치(ChatMessageRetentionScheduler)가 채팅방과 무관하게
    // timestamp만으로 조회하므로, 복합 인덱스(chatRoomId + timestamp)와 별개로 단일 인덱스가 필요하다.
    @Indexed
    private LocalDateTime timestamp;

    private String imageUrl;

}
