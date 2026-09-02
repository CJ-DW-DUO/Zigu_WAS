package com.zigu.ziguwas.websocket;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.ChannelRegistration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

@Configuration
@EnableWebSocketMessageBroker
@RequiredArgsConstructor
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    private final StompHandler stompHandler;
    private final ChatDeliveryInterceptor chatDeliveryInterceptor;

    @Override
    public void configureMessageBroker(MessageBrokerRegistry config) {
        // 메시지 받을 때: /sub로 시작하는 경로를 구독하는 클라이언트에게 메시지 전달
        config.enableSimpleBroker("/sub");
        // 메시지 보낼 때: /pub로 시작하는 경로의 메시지만 MessageMapping으로 라우팅
        config.setApplicationDestinationPrefixes("/pub");
    }

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        registry.addEndpoint("/ws-stomp") // 웹소켓 연결 엔드포인트
                .setAllowedOriginPatterns("*"); // 앱 통신을 위해 허용 범위 설정
    }

    @Override
    public void configureClientInboundChannel(ChannelRegistration registration) {
        // 클라이언트로부터 들어오는 메시지 인터셉터 등록 (JWT 인증 + 구독 권한 검사)
        registration.interceptors(stompHandler);
    }

    @Override
    public void configureClientOutboundChannel(ChannelRegistration registration) {
        // 클라이언트로 나가는 메시지 인터셉터 등록 (전달 직전 참여자 재확인)
        registration.interceptors(chatDeliveryInterceptor);
    }
}
