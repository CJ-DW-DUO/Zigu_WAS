package com.zigu.ziguwas.websocket;

import com.zigu.ziguwas.domains.chat.repository.ChatParticipantRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.SimpMessageType;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.messaging.SessionDisconnectEvent;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

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
 *
 * 수신자는 세션ID로 식별한다. SimpleBroker 가 구독자마다 새로 만들어 보내는 프레임에는
 * 세션 속성(sessionAttributes)과 사용자(simpUser)가 실리지 않고 세션ID만 실리기 때문이다.
 * 그래서 CONNECT 때 StompHandler 가 세션ID와 사용자ID를 여기에 등록해 두고, 연결이 끊기면 지운다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ChatDeliveryInterceptor implements ChannelInterceptor {

    private final ChatParticipantRepository chatParticipantRepository;

    private static final String CHAT_ROOM_DESTINATION_PREFIX = "/sub/chat/room/";

    // 연결 중인 세션ID -> 사용자ID
    // ponytail: 서버 1대 + 인메모리 SimpleBroker 구조라 이 맵도 서버 메모리에만 둔다. 서버를 여러 대로 늘려 외부 브로커로 바꿀 때는 Redis 등 공유 저장소로 함께 옮긴다.
    private final Map<String, Long> userIdBySessionId = new ConcurrentHashMap<>();

    /**
     * CONNECT 에 성공한 세션과 사용자를 등록합니다.
     *
     * @param sessionId 웹소켓 세션ID
     * @param userId 사용자ID
     */
    public void register(String sessionId, Long userId) {
        userIdBySessionId.put(sessionId, userId);
    }

    /**
     * 연결이 끊긴 세션을 지웁니다. 정상 DISCONNECT, 네트워크 단절, 하트비트 만료 모두에서 발행됩니다.
     *
     * @param event 세션 종료 이벤트
     */
    @EventListener
    public void onDisconnect(SessionDisconnectEvent event) {
        userIdBySessionId.remove(event.getSessionId());
    }

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

        // 3. 수신자 식별 (CONNECT 시점에 StompHandler 가 등록해 둔 세션ID -> 사용자ID)
        // 브로커가 구독자마다 만든 프레임에는 세션ID만 실려 있어, 세션 속성이 아니라 이 값으로 찾는다.
        String sessionId = accessor.getSessionId();

        Long userId = sessionId != null ? userIdBySessionId.get(sessionId) : null;

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
