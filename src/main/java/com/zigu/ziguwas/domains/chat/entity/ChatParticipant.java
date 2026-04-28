package com.zigu.ziguwas.domains.chat.entity;

import com.zigu.ziguwas.domains.user.entity.User;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Builder
public class ChatParticipant {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "part_id")
    private Long partId;

    @ManyToOne
    @JoinColumn(name = "chat_id", nullable = false)
    private ChatRoom chatRoom;

    @ManyToOne
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    // 해당 사용자가 채팅방을 목록에 놓고 보고 있는지 체킹.
    // 이후 로직에 따라 달라질 수 있음
    // 1. 모든 사용자들이 해당 채팅방을 비참여 상태로 변경할 시 채팅방 삭제
    // 2. 한명이라도 채팅방 비참여 상태일 시 메시지를 더이상 보낼 수 없음. 다만 이전 기록은 볼 수 있음.
    @Column(name = "is_participating", nullable = false)
    private Boolean isParticipating;

    public void setParticipating(boolean participating) {
        this.isParticipating = participating;
    }

}
