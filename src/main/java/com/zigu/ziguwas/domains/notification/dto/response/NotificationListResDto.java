package com.zigu.ziguwas.domains.notification.dto.response;

import com.zigu.ziguwas.domains.notification.entity.Notification;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.time.LocalDateTime;

@Getter
@RequiredArgsConstructor
@Builder
@Schema(description = "알림 목록 응답 DTO")
public class NotificationListResDto {

    @Schema(description = "알림 ID", example = "12")
    private final String notificationId;

    @Schema(description = "알림 타입", example = "RENTAL_REQUEST")
    private final String type;

    @Schema(description = "알림 제목", example = "새로운 대여 요청")
    private final String title;

    @Schema(description = "알림 내용", example = "홍길동님이 카메라 대여를 요청했어요.")
    private final String content;

    @Schema(description = "알림 수신 시각", example = "2026-04-27T14:30:00")
    private final LocalDateTime receivedAt;

    @Schema(description = "읽음 여부", example = "false")
    private final Boolean isRead;

    @Schema(description = "읽음 처리 시각", example = "2026-04-27T15:00:00", nullable = true)
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
                .notificationId(notification.getId())
                .type(notification.getNotificationType().name())
                .title(notification.getNotiTitle())
                .content(notification.getNotiContent())
                .receivedAt(notification.getRecTime())
                .isRead(notification.getIsRead())
                .readAt(notification.getReadAt())
                .build();
    }
}


