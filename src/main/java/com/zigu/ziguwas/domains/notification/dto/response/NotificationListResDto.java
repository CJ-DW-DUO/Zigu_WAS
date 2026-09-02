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

    @Schema(description = "알림 클릭 시 이동할 대상의 PK. type이 CHAT이면 채팅방ID, 그 외(거래 관련)면 거래ID입니다.", example = "42", nullable = true)
    private final String referenceId;

    @Schema(description = "채팅방 ID. type이 CHAT인 경우에만 값이 있습니다.", example = "42", nullable = true)
    private final String chatRoomId;

    @Schema(description = "거래 ID. type이 거래 관련(CHAT 외)인 경우에만 값이 있습니다.", example = "42", nullable = true)
    private final String tradeId;

    @Schema(description = "알림과 연관된 매물 ID", example = "11", nullable = true)
    private final Long itemId;

    @Schema(description = "알림과 연관된 매물명", example = "캐논 카메라 렌즈", nullable = true)
    private final String itemTitle;

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
                .referenceId(notification.getReferenceId())
                .chatRoomId(notification.getChatRoomId())
                .tradeId(notification.getTradeId())
                .itemId(notification.getItemId())
                .itemTitle(notification.getItemTitle())
                .build();
    }
}


