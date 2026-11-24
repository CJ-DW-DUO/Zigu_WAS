package com.zigu.ziguwas.domains.user;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.zigu.ziguwas.domains.university.entity.University;
import com.zigu.ziguwas.domains.user.dto.auth.request.LoginReqDto;
import com.zigu.ziguwas.domains.user.entity.User;
import com.zigu.ziguwas.domains.user.repository.UserRepository;
import com.zigu.ziguwas.domains.user.service.AuthService;
import com.zigu.ziguwas.exception.CustomException;
import com.zigu.ziguwas.exception.ErrorCode;
import com.zigu.ziguwas.redis.RedisService;
import com.zigu.ziguwas.security.JwtUtil;
import com.zigu.ziguwas.security.TokenService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** 로그인도 이메일을 소문자로 정리해 조회·토큰·비밀번호 재설정 상태 정리에 쓴다. (대소문자를 구분하는 DB 로 옮겨도 대문자 로그인이 막히지 않게) */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class LoginEmailNormalizeTest {

    @Mock UserRepository userRepository;
    @Mock PasswordEncoder passwordEncoder;
    @Mock RedisService redisService;
    @Mock JwtUtil jwtUtil;
    @Mock TokenService tokenService;
    @InjectMocks AuthService authService;

    private LoginReqDto login(String email) throws Exception {
        return new ObjectMapper().readValue("{\"email\":\"" + email + "\",\"password\":\"Zigu123!@#\"}", LoginReqDto.class);
    }

    @Test
    void 대문자_이메일로_로그인해도_소문자로_조회하고_토큰도_소문자() throws Exception {
        User user = User.builder().id(1L).email("a@snu.ac.kr").nickname("a").password("enc")
                .univ(new University(1L, "서울대학교", "snu.ac.kr")).build();
        when(userRepository.findByEmail("a@snu.ac.kr")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches(anyString(), anyString())).thenReturn(true);
        when(jwtUtil.createAccessToken("a@snu.ac.kr")).thenReturn("access");
        when(jwtUtil.createRefreshToken("a@snu.ac.kr")).thenReturn("refresh");

        var res = authService.tryLogin(login("A@SNU.AC.KR"), new MockHttpServletResponse());

        verify(userRepository).findByEmail("a@snu.ac.kr");
        verify(redisService).deleteData("PWRESET:a@snu.ac.kr");
        verify(jwtUtil).createAccessToken("a@snu.ac.kr");
        assertEquals("access", res.getAccessToken());
    }

    @Test
    void 없는_계정이면_기존처럼_USER_NOT_FOUND() throws Exception {
        when(userRepository.findByEmail("none@snu.ac.kr")).thenReturn(Optional.empty());

        CustomException e = assertThrows(CustomException.class,
                () -> authService.tryLogin(login("NONE@snu.ac.kr"), new MockHttpServletResponse()));

        assertEquals(ErrorCode.USER_NOT_FOUND, e.getErrorCode());
    }
}
