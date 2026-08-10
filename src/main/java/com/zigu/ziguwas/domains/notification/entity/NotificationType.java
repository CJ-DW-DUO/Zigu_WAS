package com.zigu.ziguwas.domains.notification.entity;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum NotificationType {
    // 채팅 관련
    CHAT(NotificationCategory.CHAT,"새로운 채팅 메시지가 도착했습니다."),

    // 대여 프로세스 (판매자 입장에서 받는 알림)
    RENTAL_REQUEST(NotificationCategory.TRADE,"물건 대여 요청이 들어왔습니다."),
    RETURN_REQUEST(NotificationCategory.TRADE,"물건 반납 신청이 들어왔습니다."),

    // 대여 프로세스 (구매자 입장에서 받는 알림)
    RENTAL_ACCEPT(NotificationCategory.TRADE,"대여 요청이 승인되었습니다."),
    RENTAL_REJECT(NotificationCategory.TRADE,"대여 요청이 거절되었습니다."),
    RETURN_COMPLETE(NotificationCategory.TRADE,"반납 처리가 완료되었습니다."),

    // 반납기한 초과 (매일 스케줄러가 발행, 임대인/임차인 각각 다른 문구)
    RETURN_OVERDUE_RENTER(NotificationCategory.TRADE,"빌려준 물건의 반납 기한이 지났습니다."),
    RETURN_OVERDUE_RENTEE(NotificationCategory.TRADE,"빌린 물건의 반납 기한이 지났습니다.");

    private final NotificationCategory category;
    private final String description;

}
