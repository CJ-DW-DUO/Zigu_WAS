package com.zigu.ziguwas.domains.user;

import com.zigu.ziguwas.domains.university.repository.UniversityRepository;
import com.zigu.ziguwas.domains.user.dto.auth.request.EmailReqDto;
import com.zigu.ziguwas.domains.user.dto.auth.request.EmailVerifyReqDto;
import com.zigu.ziguwas.domains.user.repository.UserRepository;
import com.zigu.ziguwas.domains.user.service.AuthService;
import com.zigu.ziguwas.exception.CustomException;
import com.zigu.ziguwas.exception.ErrorCode;
import com.zigu.ziguwas.redis.RedisService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.mail.MailSendException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** 인증번호 재발송 대기·오답 횟수 제한 검증. Redis 는 만료 없는 메모리 맵으로 흉내 낸다(만료 자체는 Redis 몫). */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class VerifyCodeLimitTest {

    @Mock UserRepository userRepository;
    @Mock UniversityRepository universityRepository;
    @Mock JavaMailSender mailSender;
    @Mock RedisService redisService;
    @InjectMocks AuthService authService;

    Map<String, String> redis = new HashMap<>();

    @BeforeEach
    void setUp() {
        when(redisService.getData(anyString())).thenAnswer(i -> redis.get(i.<String>getArgument(0)));
        doAnswer(i -> redis.put(i.getArgument(0), i.getArgument(1)))
                .when(redisService).setDataExpire(anyString(), anyString(), anyLong());
        doAnswer(i -> redis.remove(i.<String>getArgument(0))).when(redisService).deleteData(anyString());
        when(universityRepository.existsByUnivEmail("snu.ac.kr")).thenReturn(true);
        when(userRepository.existsByEmail("a@snu.ac.kr")).thenReturn(false);
    }

    private EmailReqDto emailDto(String email) throws Exception {
        return new ObjectMapper().readValue("{\"email\":\"" + email + "\"}", EmailReqDto.class);
    }

    private EmailVerifyReqDto verifyDto(String email, String code) throws Exception {
        return new ObjectMapper().readValue("{\"email\":\"" + email + "\",\"code\":\"" + code + "\"}", EmailVerifyReqDto.class);
    }

    // 같은 이메일로 바로 다시 요청하면 429, 메일은 한 번만 나간다. 비밀번호 찾기도 같은 규칙(서로 별개로 센다).
    @Test
    void 재발송은_대기시간_안에는_거절() throws Exception {
        authService.emailCodeSend(emailDto("a@snu.ac.kr"));

        CustomException tooSoon = assertThrows(CustomException.class, () -> authService.emailCodeSend(emailDto("a@snu.ac.kr")));
        assertEquals(ErrorCode.VERIFY_CODE_RESEND_TOO_SOON, tooSoon.getErrorCode());
        verify(mailSender, times(1)).send(any(SimpleMailMessage.class));

        // 비밀번호 찾기 대기는 회원가입과 따로 센다
        when(userRepository.existsByEmail("a@snu.ac.kr")).thenReturn(true);
        assertDoesNotThrow(() -> authService.passwordResetEmailSend(emailDto("a@snu.ac.kr")));
        CustomException tooSoonAgain = assertThrows(CustomException.class, () -> authService.passwordResetEmailSend(emailDto("a@snu.ac.kr")));
        assertEquals(ErrorCode.VERIFY_CODE_RESEND_TOO_SOON, tooSoonAgain.getErrorCode());
    }

    // 메일 발송이 실패하면 대기를 풀어 바로 다시 요청할 수 있다.
    @Test
    void 메일발송_실패하면_대기를_푼다() throws Exception {
        doThrow(new MailSendException("fail")).doNothing().when(mailSender).send(any(SimpleMailMessage.class));

        assertThrows(MailSendException.class, () -> authService.emailCodeSend(emailDto("a@snu.ac.kr")));
        assertDoesNotThrow(() -> authService.emailCodeSend(emailDto("a@snu.ac.kr")));
    }

    // 다섯 번까지는 오답(400), 여섯 번째부터는 코드가 맞아도 429. 가입·비밀번호 찾기 모두.
    @Test
    void 오답이_다섯번을_넘으면_정답도_거절() throws Exception {
        redis.put("a@snu.ac.kr", "123456");

        for (int i = 0; i < 5; i++) {
            CustomException wrong = assertThrows(CustomException.class, () -> authService.emailVerification(verifyDto("a@snu.ac.kr", "000000")));
            assertEquals(ErrorCode.VERIFY_CODE_NOT_MATCHED, wrong.getErrorCode());
        }

        CustomException blocked = assertThrows(CustomException.class, () -> authService.emailVerification(verifyDto("a@snu.ac.kr", "123456")));
        assertEquals(ErrorCode.VERIFY_CODE_TOO_MANY_ATTEMPTS, blocked.getErrorCode());

        redis.put("PWRESET:a@snu.ac.kr", "654321");
        for (int i = 0; i < 5; i++) {
            assertThrows(CustomException.class, () -> authService.passwordResetEmailVerify(verifyDto("a@snu.ac.kr", "000000")));
        }
        CustomException pwBlocked = assertThrows(CustomException.class, () -> authService.passwordResetEmailVerify(verifyDto("a@snu.ac.kr", "654321")));
        assertEquals(ErrorCode.VERIFY_CODE_TOO_MANY_ATTEMPTS, pwBlocked.getErrorCode());
    }

    // 정답을 맞히면 오답 기록이 지워져 다시 처음부터 센다. 오답 횟수는 이메일마다 따로 센다.
    @Test
    void 정답이면_기록이_지워지고_이메일마다_따로_센다() throws Exception {
        redis.put("a@snu.ac.kr", "123456");
        for (int i = 0; i < 4; i++) {
            assertThrows(CustomException.class, () -> authService.emailVerification(verifyDto("a@snu.ac.kr", "000000")));
        }

        assertDoesNotThrow(() -> authService.emailVerification(verifyDto("a@snu.ac.kr", "123456")));
        assertEquals("DONE", redis.get("a@snu.ac.kr"));
        assertEquals(null, redis.get("VERIFYFAIL:a@snu.ac.kr"));

        // 다른 이메일은 영향 없음
        redis.put("b@snu.ac.kr", "111111");
        CustomException other = assertThrows(CustomException.class, () -> authService.emailVerification(verifyDto("b@snu.ac.kr", "000000")));
        assertEquals(ErrorCode.VERIFY_CODE_NOT_MATCHED, other.getErrorCode());
    }
}
