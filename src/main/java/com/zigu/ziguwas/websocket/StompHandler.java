package com.zigu.ziguwas.websocket;

import com.zigu.ziguwas.security.JwtUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class StompHandler implements ChannelInterceptor {

    private final JwtUtil jwtUtil;

    @Override
    public Message<?> preSend(Message<?> message, MessageChannel channel) {
        StompHeaderAccessor accessor = StompHeaderAccessor.wrap(message);

        // 연결 요청 시 JWT 토큰 검증
        if (StompCommand.CONNECT == accessor.getCommand()) {
            String jwtToken = accessor.getFirstNativeHeader("Authorization");

            if (jwtToken != null && jwtToken.startsWith("Bearer ")) {
                String token = jwtToken.substring(7);
                // JwtUtil의 이메일 추출 로직을 활용해 검증
                String email = jwtUtil.getEmailFromToken(token);

                if (email == null) {
                    throw new RuntimeException("인증 정보가 올바르지 않습니다.");
                }
                // 필요 시 accessor에 사용자 정보를 저장하여 이후 로직에서 활용 가능
            } else {
                throw new RuntimeException("토큰이 존재하지 않습니다.");
            }
        }
        return message;
    }
}
