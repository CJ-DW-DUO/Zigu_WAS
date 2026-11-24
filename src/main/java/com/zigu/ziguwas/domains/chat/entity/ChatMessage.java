package com.zigu.ziguwas.domains.chat.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import lombok.Builder;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
// 채팅방 상세조회 : 특정 채팅방의 메시지를 최신순으로 페이징하고, 조회 하한선(visibleFrom) 이후만 걸러내기 위한 인덱스
@Table(name = "chat_message", indexes = @Index(name = "idx_chat_message_room_ts", columnList = "chat_room_id, timestamp"))
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Builder
public class ChatMessage {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    // 값 참조(FK 없음) — ponytail: 무결성 사고가 나면 FK(@ManyToOne) 로
    @Column(nullable = false)
    private String chatRoomId;

    @Column(nullable = false)
    private Long senderId;

    // 길이 제한이 없던 Mongo 와 같게 text (기본 varchar(255) 면 255자 넘는 메시지가 저장 실패)
    @Column(columnDefinition = "text")
    private String message;

    @Column(nullable = false)
    private LocalDateTime timestamp;

    // 클라이언트가 보낸 문자열을 그대로 저장하므로 text
    @Column(columnDefinition = "text")
    private String imageUrl;

}
