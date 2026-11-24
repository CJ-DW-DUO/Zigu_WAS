package com.zigu.ziguwas.domains.notification.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(
        name = "notification",
        indexes = {
                // 알림목록 조회 : user_id 로 찾고 rec_time 최신순으로 가져오기
                @Index(name = "idx_notification_user_rec", columnList = "user_id, rec_time"),
                // 미읽음 카운트 : user_id 가 123 이고 읽지 않은(is_read 가 false) 알림이 몇 개인지 세기
                @Index(name = "idx_notification_user_read", columnList = "user_id, is_read"),
                // 90일 보관 배치가 rec_time 만으로 지우므로 단독 인덱스가 있어야 매일 전체 스캔을 하지 않는다
                @Index(name = "idx_notification_rec_time", columnList = "rec_time")
        }
)
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Getter
public class Notification {

    // ponytail: UUIDv4 문자열 PK, 쓰기량이 커져 인덱스 단편화가 보이면 UUIDv7 또는 IDENTITY 로
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    @Column(nullable = false)
    private Long userId;

    // 숫자(ORDINAL)로 저장되면 enum 순서가 바뀔 때 의미가 뒤바뀐다
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private NotificationType notificationType;

    private String notiTitle;

    // 채팅 알림은 "닉네임님: 메시지 전문" 이라 255자를 넘을 수 있다
    @Column(columnDefinition = "text")
    private String notiContent;

    // 90일이 지난 알림은 NotificationRetentionScheduler 가 매일 지운다 (Mongo TTL 인덱스 대체)
    @Column(nullable = false)
    private LocalDateTime recTime;

    @Column(nullable = false)
    private Boolean isRead;

    private LocalDateTime readAt;

    // 알림을 눌렀을 때 이동할 대상의 PK (채팅방ID 또는 거래ID 등, notificationType에 따라 참조 대상이 다름)
    private String referenceId;

    // 알림과 연관된 매물 PK (푸시 알림 payload 구성 시 사용)
    private Long itemId;

    // 알림과 연관된 매물명 (알림 생성 시점의 스냅샷, 목록/푸시 표시용)
    private String itemTitle;

    /**
     * 알림 생성 시 사용하는 정적 팩토리 메서드입니다.
     *
     * 생성 시점에 수신 시간(recTime)을 현재 시간으로 기록하고,
     * 읽음 상태는 기본값(false)으로 초기화합니다.
     */
    public static Notification create(
            Long userId,
            NotificationType notificationType,
            String notiTitle,
            String notiContent,
            String referenceId,
            Long itemId,
            String itemTitle
    ) {
        return Notification.builder()
                .userId(userId)
                .notificationType(notificationType)
                .notiTitle(notiTitle)
                .notiContent(notiContent)
                .recTime(LocalDateTime.now())
                .isRead(false)
                .referenceId(referenceId)
                .itemId(itemId)
                .itemTitle(itemTitle)
                .build();
    }

    /**
     * referenceId가 채팅방ID를 의미하는 타입(CHAT)인 경우에만 채팅방ID를 반환합니다.
     */
    public String getChatRoomId() {
        return notificationType.getCategory() == NotificationCategory.CHAT ? referenceId : null;
    }

    /**
     * referenceId가 거래ID를 의미하는 타입(CHAT 외)인 경우에만 거래ID를 반환합니다.
     */
    public String getTradeId() {
        return notificationType.getCategory() == NotificationCategory.CHAT ? null : referenceId;
    }

    /**
     * 알림을 읽음 상태로 변경합니다.
     *
     * 이미 읽은 알림은 중복 갱신하지 않습니다.
     */
    public void markAsRead() {
        if (Boolean.TRUE.equals(this.isRead)) {
            return;
        }
        this.isRead = true;
        this.readAt = LocalDateTime.now();
    }
}
