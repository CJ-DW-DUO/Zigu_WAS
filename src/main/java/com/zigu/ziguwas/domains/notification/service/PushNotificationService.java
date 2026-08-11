package com.zigu.ziguwas.domains.notification.service;

import com.zigu.ziguwas.domains.notification.entity.Notification;
import com.zigu.ziguwas.domains.notification.entity.NotificationCategory;
import com.zigu.ziguwas.domains.notification.entity.PushToken;
import com.zigu.ziguwas.domains.notification.repository.PushTokenRepository;
import com.zigu.ziguwas.push.ExpoPushClient;
import com.zigu.ziguwas.push.ExpoPushMessage;
import com.zigu.ziguwas.push.ExpoPushTicket;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 저장된 알림(Notification) 1건을 바탕으로, 해당 사용자가 등록한 모든 기기에 Expo 푸시를 발송합니다.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PushNotificationService {

    private final PushTokenRepository pushTokenRepository;
    private final PushTokenService pushTokenService;
    private final ExpoPushClient expoPushClient;

    public void send(Notification notification) {
        List<PushToken> tokens = pushTokenRepository.findAllByUserId(notification.getUserId());
        if (tokens.isEmpty()) {
            return;
        }

        Map<String, Object> data = buildPayload(notification);

        List<ExpoPushMessage> messages = tokens.stream()
                .map(t -> new ExpoPushMessage(t.getToken(), notification.getNotiTitle(), notification.getNotiContent(), data))
                .toList();

        List<ExpoPushTicket> tickets = expoPushClient.send(messages);

        removeInvalidTokens(tokens, tickets);
    }

    /**
     * 프론트에서 요청한 push payload 형식(notificationId/type/chatRoomId/tradeId/itemId)을 구성합니다.
     * CHAT 카테고리는 채팅방ID, 그 외(거래 관련)는 거래ID로 referenceId의 의미가 갈립니다.
     */
    private Map<String, Object> buildPayload(Notification notification) {
        Map<String, Object> data = new HashMap<>();
        data.put("notificationId", notification.getId());
        data.put("type", notification.getNotificationType().name());
        data.put("itemId", notification.getItemId());

        if (notification.getNotificationType().getCategory() == NotificationCategory.CHAT) {
            data.put("chatRoomId", notification.getReferenceId());
        } else {
            data.put("tradeId", notification.getReferenceId());
        }

        return data;
    }

    /**
     * Expo 티켓 응답은 요청 순서를 그대로 보장하므로 tokens와 tickets를 같은 인덱스로 매칭합니다.
     * 한 사용자가 가진 기기 수는 소수(보통 1~2대)라 100개 단위 배치 분할이 사실상 발생하지 않으므로
     * 인덱스 매칭이 어긋날 걱정은 실질적으로 없습니다.
     */
    private void removeInvalidTokens(List<PushToken> tokens, List<ExpoPushTicket> tickets) {
        for (int i = 0; i < tickets.size() && i < tokens.size(); i++) {
            if (tickets.get(i).isDeviceNotRegistered()) {
                // PushTokenService(별도 빈)를 거쳐야 새 트랜잭션이 열려 삭제가 실제로 반영됨
                pushTokenService.deleteInvalidToken(tokens.get(i).getToken());
                log.info("유효하지 않은 푸시 토큰 삭제. token={}", tokens.get(i).getToken());
            }
        }
    }
}
