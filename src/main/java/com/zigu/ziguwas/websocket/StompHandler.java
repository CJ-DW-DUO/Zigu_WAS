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
import java.util.regex.Pattern;

/**
 * 클라이언트 -> 서버 방향(inbound) STOMP 프레임을 검사하는 인터셉터
 *
 * CONNECT 시 JWT를 검증해 세션에 사용자 정보를 심고, SUBSCRIBE 시 해당 채팅방의
 * 참여자인지 확인한다.
 */
@Component
@RequiredArgsConstructor
public class StompHandler implements ChannelInterceptor {

    private final JwtUtil jwtUtil;
    private final UserRepository userRepository;
    private final ChatParticipantRepository chatParticipantRepository;

    /**
     * 허용되는 유일한 구독 주소 형식.
     *
     * Spring의 SimpleBroker(DefaultSubscriptionRegistry)는 구독 주소에 AntPathMatcher
     * 패턴을 그대로 허용한다. 따라서 "/sub/chat/room/로 시작하면 검사한다"는 식의
     * 접두어 검사는 "/sub/**", "/sub/chat/**", "/sub/chat/roo?/**" 같은 주소로 손쉽게
     * 우회되고, 우회에 성공하면 서버의 모든 채팅방 메시지를 그대로 수신하게 된다.
     *
     * 그래서 접두어가 아니라 "정확히 이 형식만 허용"하는 화이트리스트로 막는다.
     * 방 ID 자리에는 와일드카드 문자(* ? /)가 들어갈 수 없으므로 패턴 구독 자체가 불가능하다.
     * (/sub 아래에 새로운 구독 주소를 추가한다면 이 목록에도 함께 추가해야 한다)
     *
     * 보안상 추가한 기능
     */
    private static final Pattern CHAT_ROOM_DESTINATION = Pattern.compile("^/sub/chat/room/[A-Za-z0-9_-]{1,64}$");

    private static final String CHAT_ROOM_DESTINATION_PREFIX = "/sub/chat/room/";

    // 세션에 심어두는 사용자 식별 정보 키
    public static final String SESSION_USER_EMAIL = "userEmail";
    public static final String SESSION_USER_ID = "userId";

    @Override
    public Message<?> preSend(Message<?> message, MessageChannel channel) {

        // 접근자 객체
        StompHeaderAccessor accessor = StompHeaderAccessor.wrap(message);

        // 연결 요청 시 JWT 토큰 검증
        if (StompCommand.CONNECT == accessor.getCommand()) {
            handleConnect(accessor);
        }

        // 해당 채팅방에 대해 구독 권한이 있는지 확인
        if (StompCommand.SUBSCRIBE == accessor.getCommand()) {
            handleSubscribe(accessor);
        }

        return message;
    }

    /**
     * CONNECT 프레임의 JWT를 검증하고 사용자 정보를 세션에 저장합니다.
     *
     * 여기서 사용자 ID까지 한 번만 조회해 세션에 심어두면, 이후 SUBSCRIBE와
     * 메시지 전달 검사에서 매번 사용자 테이블을 다시 조회하지 않아도 된다.
     *
     * @param accessor STOMP 헤더 접근자
     */
    private void handleConnect(StompHeaderAccessor accessor) {

        String jwtToken = accessor.getFirstNativeHeader("Authorization");

        if (jwtToken == null || !jwtToken.startsWith("Bearer ")) {
            // 토큰이 존재하지 않음
            throw new CustomException(ErrorCode.TOKEN_NOT_FOUND);
        }

        String token = jwtToken.substring(7);

        // 재발급 전용인 refresh 토큰(유효기간 7일)으로는 연결할 수 없다.
        // 검증만 하는 getEmailFromToken은 토큰 타입을 구분하지 않으므로 여기서 따로 막는다.
        if (jwtUtil.isRefreshToken(token)) {
            throw new CustomException(ErrorCode.UNAUTHORIZED_ACCESS);
        }

        // JwtUtil의 이메일 추출 로직을 활용해 검증 (서명/만료가 어긋나면 예외)
        String email = jwtUtil.getEmailFromToken(token);

        if (email == null) {
            // 해당 이메일 정보가 없으면 유저가 없음
            throw new CustomException(ErrorCode.USER_NOT_FOUND);
        }

        User user = userRepository.findByEmail(email).orElseThrow(
                () -> new CustomException(ErrorCode.USER_NOT_FOUND)
        );

        Map<String, Object> sessionAttributes = accessor.getSessionAttributes();

        if (sessionAttributes == null) {
            throw new CustomException(ErrorCode.UNAUTHORIZED_ACCESS);
        }

        sessionAttributes.put(SESSION_USER_EMAIL, email);
        sessionAttributes.put(SESSION_USER_ID, user.getId());
    }

    /**
     * SUBSCRIBE 프레임의 구독 주소와 참여자 권한을 검증합니다.
     *
     * 허용 목록에 없는 주소는 전부 거절한다(기본 거절). 알 수 없는 주소를 통과시키면
     * 브로커의 패턴 매칭 때문에 남의 채팅방 메시지까지 흘러갈 수 있기 때문이다.
     *
     * @param accessor STOMP 헤더 접근자
     */
    private void handleSubscribe(StompHeaderAccessor accessor) {

        String destination = accessor.getDestination();

        // 1. 허용된 형식의 주소인지 확인 (와일드카드 구독 차단)
        if (destination == null || !CHAT_ROOM_DESTINATION.matcher(destination).matches()) {
            throw new CustomException(ErrorCode.UNAUTHORIZED_ACCESS);
        }

        // 2. 앞의 부분만큼 제거하고 실제 채팅방 ID를 가져옴
        String chatRoomId = destination.substring(CHAT_ROOM_DESTINATION_PREFIX.length());

        // 3. 세션 정보에서 사용자 ID 꺼내기 (CONNECT 시점에 저장해 둔 값)
        Map<String, Object> sessionAttributes = accessor.getSessionAttributes();

        Long userId = sessionAttributes != null
                ? (Long) sessionAttributes.get(SESSION_USER_ID) : null;

        if (userId == null) {
            throw new CustomException(ErrorCode.USER_NOT_FOUND);
        }

        // 4. 이 채팅방의 참여자가 맞는지 검사 (나간 사용자는 구독할 수 없음)
        boolean isParticipant = chatParticipantRepository.existsByChatRoomIdAndUserIdAndLeftAtIsNull(
                chatRoomId, userId
        );

        if (!isParticipant) {
            throw new CustomException(ErrorCode.UNAUTHORIZED_ACCESS);
        }
    }
}
