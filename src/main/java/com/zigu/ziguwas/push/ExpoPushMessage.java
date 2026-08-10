package com.zigu.ziguwas.push;

import java.util.Map;

/**
 * Expo Push API에 보낼 메시지 1건입니다. (기기 토큰 1개 = 메시지 1개)
 *
 * @param to    Expo 푸시 토큰 (예: "ExponentPushToken[xxxx]")
 * @param title 알림 제목
 * @param body  알림 본문
 * @param data  알림을 탭했을 때 앱이 사용할 부가 데이터 (notificationId, type, chatRoomId, tradeId, itemId 등)
 */
public record ExpoPushMessage(
        String to,
        String title,
        String body,
        Map<String, Object> data
) {
}
