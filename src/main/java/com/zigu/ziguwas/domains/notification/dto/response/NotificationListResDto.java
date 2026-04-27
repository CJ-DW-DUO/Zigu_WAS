package com.zigu.ziguwas.domains.notification.dto.response;

import com.zigu.ziguwas.domains.notification.entity.Notification;
import lombok.Builder;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.time.LocalDateTime;

@Getter
@RequiredArgsConstructor
@Builder
public class NotificationListResDto {

    private final Long notificationId;
    private final String type;
    private final String title;
    private final String content;
    private final LocalDateTime receivedAt;
    private final Boolean isRead;
    private final LocalDateTime readAt;

    /**
     * 알림 엔티티를 목록 응답 DTO로 변환합니다.
     *
     * @param notification 알림 엔티티
     * @return 목록 응답 DTO
     */
    public static NotificationListResDto fromEntity(Notification notification) {
        // 1. 엔티티 필드를 응답 스키마에 맞게 매핑
        return NotificationListResDto.builder()
                .notificationId(notification.getNotiId())
                .type(notification.getNotificationType().name())
                .title(notification.getNotiTitle())
                .content(notification.getNotiContent())
                .receivedAt(notification.getRecTime())
                .isRead(notification.getIsRead())
                .readAt(notification.getReadAt())
                .build();
    }
}


