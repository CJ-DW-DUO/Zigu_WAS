package com.zigu.ziguwas.domains.notification.event;

import com.zigu.ziguwas.domains.notification.entity.NotificationType;

/**
 * 알림 생성 요청을 전달하는 도메인 이벤트입니다.
 *
 * 서비스 계층은 이 이벤트만 발행하고,
 * 실제 알림 저장은 리스너/알림 서비스가 담당합니다.
 */
public record NotificationCreatedEvent(
        Long receiverUserId,
        NotificationType type,
        String title,
        String content
) {
}


