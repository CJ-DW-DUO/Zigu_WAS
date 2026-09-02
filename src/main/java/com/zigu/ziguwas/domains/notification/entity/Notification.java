package com.zigu.ziguwas.domains.notification.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.CompoundIndexes;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;

@Document(collection = "notifications")
/**
 * name : 인덱스 이름 (Mongo에서 확인할 때 보이는 이름, 식별하기 쉽게만 지으면 됨)
 * def : 어떤 필드를 인덱스로 묶을지 정의, 1은 오름차순, -1은 내림차순
 */
@CompoundIndexes({
        // 알림목록 조회 : userId와 recTime을 묶어서 인덱스를 생성, recTime은 내림차순으로 정렬
        // userId가 123인 알림을 recTime 최신순(내림차순)으로 가져오기
        @CompoundIndex(name = "idx_userId_recTime", def = "{'userId': 1, 'recTime': -1}"),
        // 미읽음 카운트 : userId와 isRead를 묶어서 인덱스를 생성, isRead는 오름차순으로 정렬
        // userId가 123인 읽지않은(isRead가 false)인 알림이 몇개인지 카운트하기 위해 isRead를 오름차순으로 정렬(이때 오름차순 정렬은 의미없음)
        @CompoundIndex(name = "idx_userId_isRead",  def = "{'userId': 1, 'isRead': 1}")
})
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Getter
public class Notification {

    @Id
    private String id;

    private Long userId;

    private NotificationType notificationType;

    private String notiTitle;

    private String notiContent;

    // ISO-8601 형식, 90일, 오래된 알림은 제거하기 위한 TTL 인덱스 설정
    @Indexed(expireAfter = "P90D")
    private LocalDateTime recTime;

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
