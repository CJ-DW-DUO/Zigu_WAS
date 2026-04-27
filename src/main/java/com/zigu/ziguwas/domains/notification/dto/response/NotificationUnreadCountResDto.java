package com.zigu.ziguwas.domains.notification.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
@Schema(description = "미읽음 알림 개수 응답 DTO")
public class NotificationUnreadCountResDto {

    // 현재 로그인 사용자의 미읽음 알림 개수
    @Schema(description = "미읽음 알림 개수", example = "3")
    private Long unreadCount;
}


