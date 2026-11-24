package com.zigu.ziguwas.security;

import com.zigu.ziguwas.domains.chat.repository.ChatParticipantRepository;
import com.zigu.ziguwas.domains.user.entity.User;
import com.zigu.ziguwas.domains.user.repository.UserRepository;
import com.zigu.ziguwas.exception.CustomAuthenticationEntryPoint;
import com.zigu.ziguwas.exception.CustomException;
import com.zigu.ziguwas.exception.ErrorCode;
import com.zigu.ziguwas.redis.RedisService;
import com.zigu.ziguwas.websocket.ChatDeliveryInterceptor;
import com.zigu.ziguwas.websocket.StompHandler;
import jakarta.servlet.FilterChain;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.messaging.Message;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.HashMap;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/** S0 W-01(필터가 refresh 토큰 거부) · W-02(STOMP CONNECT 블랙리스트 거부) 회귀 검증. */
class TokenGuardTest {

    private final JwtUtil jwtUtil = new JwtUtil("test-secret-key-test-secret-key-test-secret-key-1234");
    private final RedisService redisService = mock(RedisService.class);
    private final CustomUserDetailService userDetailService = mock(CustomUserDetailService.class);
    private final UserRepository userRepository = mock(UserRepository.class);
    private final FilterChain chain = mock(FilterChain.class);

    private JwtAuthenticationFilter filter;
    private StompHandler stompHandler;

    @BeforeEach
    void setUp() {
        SecurityContextHolder.clearContext();
        filter = new JwtAuthenticationFilter(jwtUtil, userDetailService, new CustomAuthenticationEntryPoint(), redisService);
        stompHandler = new StompHandler(jwtUtil, userRepository, mock(ChatParticipantRepository.class), redisService,
                new ChatDeliveryInterceptor(mock(ChatParticipantRepository.class)));
    }

    private MockHttpServletRequest bearer(String token) {
        MockHttpServletRequest req = new MockHttpServletRequest();
        req.addHeader("Authorization", "Bearer " + token);
        return req;
    }

    @Test
    void filter_refreshToken은_인증하지_않는다() throws Exception {
        MockHttpServletRequest req = bearer(jwtUtil.createRefreshToken("a@x.com"));
        filter.doFilter(req, new MockHttpServletResponse(), chain);

        assertNull(SecurityContextHolder.getContext().getAuthentication());
        assertEquals(ErrorCode.UNAUTHORIZED_ACCESS, req.getAttribute("exception"));
        verify(userDetailService, never()).loadUserByUsername(any());
    }

    @Test
    void filter_accessToken은_기존대로_인증한다() throws Exception {
        var details = mock(org.springframework.security.core.userdetails.UserDetails.class);
        when(userDetailService.loadUserByUsername("a@x.com")).thenReturn(details);

        filter.doFilter(bearer(jwtUtil.createAccessToken("a@x.com")), new MockHttpServletResponse(), chain);

        assertNotNull(SecurityContextHolder.getContext().getAuthentication());
    }

    @Test
    void stomp_블랙리스트_access토큰은_연결_거부() {
        String access = jwtUtil.createAccessToken("a@x.com");
        when(redisService.getData("BLACKLIST:" + access)).thenReturn("logout");

        CustomException e = assertThrows(CustomException.class, () -> stompHandler.preSend(connect(access), null));
        assertEquals(ErrorCode.BLACKLIST_TOKEN, e.getErrorCode());
        verify(userRepository, never()).findByEmail(any());
    }

    @Test
    void stomp_정상_access토큰은_연결_허용() {
        String access = jwtUtil.createAccessToken("a@x.com");
        User user = mock(User.class);
        when(user.getId()).thenReturn(7L);
        when(userRepository.findByEmail("a@x.com")).thenReturn(Optional.of(user));

        Message<?> msg = connect(access);
        assertDoesNotThrow(() -> stompHandler.preSend(msg, null));
        assertEquals(7L, StompHeaderAccessor.wrap(msg).getSessionAttributes().get(StompHandler.SESSION_USER_ID));
    }

    private Message<?> connect(String token) {
        StompHeaderAccessor acc = StompHeaderAccessor.create(StompCommand.CONNECT);
        acc.setNativeHeader("Authorization", "Bearer " + token);
        acc.setSessionAttributes(new HashMap<>());
        // 실제 웹소켓 CONNECT 에는 항상 세션ID가 있다. (StompHandler 가 세션ID로 전달 대상을 등록한다)
        acc.setSessionId("test-session");
        return MessageBuilder.createMessage(new byte[0], acc.getMessageHeaders());
    }
}
