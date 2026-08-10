package com.zigu.ziguwas.websocket;

import com.zigu.ziguwas.domains.chat.repository.ChatParticipantRepository;
import com.zigu.ziguwas.domains.user.entity.User;
import com.zigu.ziguwas.domains.user.repository.UserRepository;
import com.zigu.ziguwas.exception.CustomException;
import com.zigu.ziguwas.exception.ErrorCode;
import com.zigu.ziguwas.security.JwtUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
@RequiredArgsConstructor
public class StompHandler implements ChannelInterceptor {

    private final JwtUtil jwtUtil;
    private final UserRepository userRepository;
    private final ChatParticipantRepository chatParticipantRepository;

    @Override
    public Message<?> preSend(Message<?> message, MessageChannel channel) {

        // 접근자 객체
        StompHeaderAccessor accessor = StompHeaderAccessor.wrap(message);

        // 연결 요청 시 JWT 토큰 검증
        if (StompCommand.CONNECT == accessor.getCommand()) {
            String jwtToken = accessor.getFirstNativeHeader("Authorization");

            if (jwtToken != null && jwtToken.startsWith("Bearer ")) {
                String token = jwtToken.substring(7);
                // JwtUtil의 이메일 추출 로직을 활용해 검증
                String email = jwtUtil.getEmailFromToken(token);

                if (email == null) {
                    // 해당 이메일 정보가 없으면 유저가 없음
                    throw new CustomException(ErrorCode.USER_NOT_FOUND);
                } else {
                    Map<String, Object> sessionAttributes = accessor.getSessionAttributes();
                    if (sessionAttributes != null) {
                        sessionAttributes.put("userEmail", email);
                    }
                }
                // 필요 시 accessor에 사용자 정보를 저장하여 이후 로직에서 활용 가능
            } else {
                // 이메일이 있으나 토큰이 존재하지 않음
                throw new CustomException(ErrorCode.TOKEN_NOT_FOUND);
            }
        }

        // 해당 채팅방에 대해 구독상태인지 확인
        if (StompCommand.SUBSCRIBE == accessor.getCommand()) {
            String destination = accessor.getDestination();

            if (destination != null && destination.startsWith("/sub/chat/room/")) {
                // 앞의 부분만큼 제거하고 실제 채팅방 ID를 가져옴
                String chatRoomId = destination.substring("/sub/chat/room/".length());

                // 세션 정보들 가져오기
                Map<String, Object> sessionAttributes = accessor.getSessionAttributes();

                // 세션 정보에서 이메일 정보 꺼내기
                String email = sessionAttributes != null
                        ? (String) sessionAttributes.get("userEmail") : null;

                if (email == null) {
                    throw new CustomException(ErrorCode.USER_NOT_FOUND);
                }

                User user = userRepository.findByEmail(email).orElseThrow(
                        () -> new CustomException(ErrorCode.USER_NOT_FOUND)
                );

                // 이 채팅방의 참여자가 맞는지 검사
                boolean isParticipant = chatParticipantRepository.existsByChatRoomIdAndUserId(
                        chatRoomId, user.getId()
                );

                if (!isParticipant) {
                    throw new CustomException(ErrorCode.UNAUTHORIZED_ACCESS);
                }
            }
        }

        return message;
    }
}
