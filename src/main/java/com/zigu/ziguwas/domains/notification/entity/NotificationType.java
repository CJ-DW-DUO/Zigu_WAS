package com.zigu.ziguwas.domains.notification.entity;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum NotificationType {
    // 채팅 관련
    CHAT("새로운 채팅 메시지가 도착했습니다."),

    // 대여 프로세스 (판매자 입장에서 받는 알림)
    RENTAL_REQUEST("물건 대여 요청이 들어왔습니다."),
    RETURN_REQUEST("물건 반납 신청이 들어왔습니다."),

    // 대여 프로세스 (구매자 입장에서 받는 알림)
    RENTAL_ACCEPT("대여 요청이 승인되었습니다."),
    RENTAL_REJECT("대여 요청이 거절되었습니다."),
    RETURN_COMPLETE("반납 처리가 완료되었습니다.");

    private final String description;

}
