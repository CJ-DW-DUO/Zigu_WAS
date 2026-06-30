package com.zigu.ziguwas.domains.notification.entity;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum NotificationCategory {
    CHAT("채팅"),
    TRADE("거래"),
    MARKETING("마케팅 수신 동의");

    private final String label;
}
