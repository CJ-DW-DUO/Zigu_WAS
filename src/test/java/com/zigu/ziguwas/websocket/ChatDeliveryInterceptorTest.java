package com.zigu.ziguwas.websocket;

import com.zigu.ziguwas.domains.chat.repository.ChatParticipantRepository;
import org.junit.jupiter.api.Test;
import org.springframework.messaging.Message;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.messaging.simp.SimpMessageType;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.messaging.SessionDisconnectEvent;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/** 아웃바운드 채팅 전달 검사: 브로커가 만드는 프레임처럼 세션ID만 실린 메시지로 확인한다. */
class ChatDeliveryInterceptorTest {

    ChatParticipantRepository participants = mock(ChatParticipantRepository.class);
    ChatDeliveryInterceptor interceptor = new ChatDeliveryInterceptor(participants);

    // SimpleBrokerMessageHandler 가 구독자에게 보내는 프레임과 같은 모양: 세션ID·구독ID·목적지만 있고 세션 속성·사용자는 없다.
    private Message<byte[]> brokerFrame(String sessionId) {
        SimpMessageHeaderAccessor accessor = SimpMessageHeaderAccessor.create(SimpMessageType.MESSAGE);
        accessor.setSessionId(sessionId);
        accessor.setSubscriptionId("s1");
        accessor.setDestination("/sub/chat/room/room1");
        accessor.setLeaveMutable(true);
        return MessageBuilder.createMessage(new byte[0], accessor.getMessageHeaders());
    }

    // 등록된 세션이고 지금도 참여자면 전달, 나간 참여자나 등록되지 않은 세션이면 취소.
    @Test
    void 참여자에게만_전달된다() {
        interceptor.register("sessionB", 2L);
        when(participants.existsByChatRoomIdAndUserIdAndLeftAtIsNull("room1", 2L)).thenReturn(true);
        assertNotNull(interceptor.preSend(brokerFrame("sessionB"), null));

        when(participants.existsByChatRoomIdAndUserIdAndLeftAtIsNull("room1", 2L)).thenReturn(false);
        assertNull(interceptor.preSend(brokerFrame("sessionB"), null));

        assertNull(interceptor.preSend(brokerFrame("unknown"), null));
    }

    // 연결이 끊기면 등록이 지워진다.
    @Test
    void 연결이_끊기면_등록이_지워진다() {
        interceptor.register("sessionB", 2L);
        when(participants.existsByChatRoomIdAndUserIdAndLeftAtIsNull("room1", 2L)).thenReturn(true);
        assertNotNull(interceptor.preSend(brokerFrame("sessionB"), null));

        interceptor.onDisconnect(new SessionDisconnectEvent(this,
                MessageBuilder.createMessage(new byte[0], SimpMessageHeaderAccessor.create(SimpMessageType.DISCONNECT).getMessageHeaders()),
                "sessionB", CloseStatus.NORMAL));

        assertNull(interceptor.preSend(brokerFrame("sessionB"), null));
    }
}
