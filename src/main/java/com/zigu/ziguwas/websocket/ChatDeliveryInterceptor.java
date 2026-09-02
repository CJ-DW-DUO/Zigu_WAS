package com.zigu.ziguwas.websocket;

import com.zigu.ziguwas.domains.chat.repository.ChatParticipantRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.SimpMessageType;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * 서버 -> 클라이언트 방향(outbound)으로 나가는 채팅 메시지를 마지막으로 검사하는 인터셉터
 *
 * 구독 권한은 SUBSCRIBE 시점에 한 번만 검사되므로, 구독을 유지한 채 채팅방을 나간
 * 사용자에게는 그 뒤에 오는 메시지가 계속 전달된다. 이렇게 전달된 메시지는 조회
 * 하한선(visibleFrom) 때문에 새로고침하면 사라지므로, 사용자 입장에서는 "보낸 적도
 * 받은 적도 없는 유령 메시지"로 보인다.
 *
 * 그래서 실제로 프레임을 내보내기 직전에 현재도 참여중인지 한 번 더 확인하고,
 * 아니면 그 수신자에게만 전달을 취소한다(null 반환).
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ChatDeliveryInterceptor implements ChannelInterceptor {

    private final ChatParticipantRepository chatParticipantRepository;

    private static final String CHAT_ROOM_DESTINATION_PREFIX = "/sub/chat/room/";

    @Override
    public Message<?> preSend(Message<?> message, MessageChannel channel) {

        StompHeaderAccessor accessor = StompHeaderAccessor.wrap(message);

        // 1. 실제 메시지 전달 프레임이 아니면 그대로 통과 (CONNECTED, RECEIPT, ERROR 등)
        if (SimpMessageType.MESSAGE != accessor.getMessageType()) {
            return message;
        }

        // 2. 채팅방 메시지가 아니면 그대로 통과
        String destination = accessor.getDestination();

        if (destination == null || !destination.startsWith(CHAT_ROOM_DESTINATION_PREFIX)) {
            return message;
        }

        // 채팅방 ID 추출하기위한 프리픽스 제거
        String chatRoomId = destination.substring(CHAT_ROOM_DESTINATION_PREFIX.length());

        // 3. 수신자 식별 (CONNECT 시점에 세션에 심어둔 값), 내부에 userId와 userEmail이 들어있음.
        Map<String, Object> sessionAttributes = accessor.getSessionAttributes();

        Long userId = sessionAttributes != null
                ? (Long) sessionAttributes.get(StompHandler.SESSION_USER_ID) : null;

        if (userId == null) {
            // 누구에게 가는 메시지인지 알 수 없으면 보내지 않는다.
            log.warn("수신자를 식별할 수 없어 채팅 메시지 전달을 취소했습니다 - destination={}", destination);
            return null;
        }

        // 4. 지금도 이 채팅방의 참여자인지 확인 (나간 뒤 남아있는 구독으로의 전달 차단)
        if (!chatParticipantRepository.existsByChatRoomIdAndUserIdAndLeftAtIsNull(chatRoomId, userId)) {
            return null;
        }

        return message;
    }
}
