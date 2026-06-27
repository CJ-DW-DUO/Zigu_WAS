package com.zigu.ziguwas.domains.chat.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

//@Entity
@Document(collection = "chat_participants")
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Builder
public class ChatParticipant {

    @Id
    private String id;

    // RDB와 NoSQL PK 타입 차이로 인하여 String
    private String chatRoomId;

    // RDB와 NoSQL PK 타입 차이로 인하여 Long
    private Long userId;

    // 해당 사용자가 채팅방을 목록에 놓고 보고 있는지 체킹.
    // 이후 로직에 따라 달라질 수 있음
    // 1. 모든 사용자들이 해당 채팅방을 비참여 상태로 변경할 시 채팅방 삭제
    // 2. 한명이라도 채팅방 비참여 상태일 시 메시지를 더이상 보낼 수 없음. 다만 이전 기록은 볼 수 있음.
    private Boolean isParticipating;

    public void setParticipating(boolean participating) {
        this.isParticipating = participating;
    }

}
