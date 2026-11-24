package com.zigu.ziguwas.domains.chat.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(
        name = "chat_participant",
        indexes = {
                // 채팅방 목록 조회 : 사용자가 나가지 않은(left_at 이 null 인) 참여 정보만 골라내기 위한 인덱스
                @Index(name = "idx_chat_participant_user_left", columnList = "user_id, left_at")
        },
        uniqueConstraints = {
                // 참여자 검증 : 특정 채팅방에 특정 사용자가 있는지 단건 조회하기 위한 인덱스
                // 한 사람이 같은 채팅방에 두 번 등록되는 것을 막기 위해 unique 로 설정
                @UniqueConstraint(name = "uk_chat_participant_room_user", columnNames = {"chat_room_id", "user_id"})
        }
)
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Builder
public class ChatParticipant {

    // ponytail: UUIDv4 문자열 PK, 쓰기량이 커져 인덱스 단편화가 보이면 UUIDv7 또는 IDENTITY 로
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    // 값 참조(FK 없음) — ponytail: 무결성 사고가 나면 FK(@ManyToOne) 로
    @Column(nullable = false)
    private String chatRoomId;

    @Column(nullable = false)
    private Long userId;

    // 해당 사용자가 채팅방을 나간(숨긴) 시각. null이면 현재 참여중인 상태이다.
    // 나가더라도 채팅방 자체는 남으며, 상대방이 새 메시지를 보내면 null로 되돌아가 목록에 다시 나타난다.
    private LocalDateTime leftAt;

    // 이 시각 이후에 생성된 메시지만 해당 사용자에게 보여준다. null이면 전체 대화 내역을 볼 수 있다.
    // leftAt과 분리해 둔 이유는, 나갔다가 다시 활성화되었을 때(leftAt이 null로 돌아갔을 때)
    // 나가기 이전의 대화 내역이 함께 되살아나는 것을 막기 위함이다.
    private LocalDateTime visibleFrom;

    /**
     * 채팅방을 나갑니다.
     *
     * 나간 시각을 조회 하한선(visibleFrom)으로 함께 기록하여,
     * 이후 채팅방이 다시 활성화되더라도 나가기 이전의 대화는 보이지 않도록 한다.
     *
     * @param leftAt 나간 시각
     */
    public void leave(LocalDateTime leftAt) {
        this.leftAt = leftAt;
        this.visibleFrom = leftAt;
    }

    /**
     * 나갔던 채팅방을 다시 활성화합니다.
     *
     * visibleFrom은 그대로 유지하므로 나가기 이전의 대화 내역은 계속 숨겨진다.
     */
    public void rejoin() {
        this.leftAt = null;
    }

    /**
     * 해당 사용자가 채팅방을 나간 상태인지 확인합니다.
     *
     * @return T/F
     */
    public boolean hasLeft() {
        return this.leftAt != null;
    }

}
