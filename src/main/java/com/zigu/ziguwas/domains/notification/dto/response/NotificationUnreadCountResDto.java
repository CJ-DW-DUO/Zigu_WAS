package com.zigu.ziguwas.domains.notification.dto.response;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class NotificationUnreadCountResDto {

    // 현재 로그인 사용자의 미읽음 알림 개수
    private Long unreadCount;
}


