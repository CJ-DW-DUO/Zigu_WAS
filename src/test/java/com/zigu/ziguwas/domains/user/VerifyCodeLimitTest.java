package com.zigu.ziguwas.domains.user;

import com.zigu.ziguwas.domains.university.repository.UniversityRepository;
import com.zigu.ziguwas.domains.user.dto.auth.request.EmailReqDto;
import com.zigu.ziguwas.domains.user.dto.auth.request.EmailVerifyReqDto;
import com.zigu.ziguwas.domains.user.repository.UserRepository;
import com.zigu.ziguwas.domains.user.service.AuthService;
import com.zigu.ziguwas.exception.CustomException;
import com.zigu.ziguwas.exception.ErrorCode;
import com.zigu.ziguwas.kv.KeyValueService;
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

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

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
    @Mock KeyValueService keyValueService;
    @InjectMocks AuthService authService;

    Map<String, String> redis = new ConcurrentHashMap<>();
    // 동시성 테스트에서 "확인 → 기록" 사이를 일부러 벌려 잠금이 없으면 오답 횟수가 넘어가도록 만드는 지연
    volatile boolean slowFailRead = false;

    @BeforeEach
    void setUp() {
        when(keyValueService.getData(anyString())).thenAnswer(i -> {
            String key = i.getArgument(0);
            if (slowFailRead && key.startsWith("VERIFYFAIL:")) {
                Thread.sleep(2);
            }
            return redis.get(key);
        });
        doAnswer(i -> redis.put(i.getArgument(0), i.getArgument(1)))
                .when(keyValueService).setDataExpire(anyString(), anyString(), anyLong());
        doAnswer(i -> redis.remove(i.<String>getArgument(0))).when(keyValueService).deleteData(anyString());
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

    // 대소문자·앞뒤 공백만 다른 주소도 같은 이메일로 본다: 같은 인증코드·재발송 대기·오답 기록을 쓴다. (변형 주소로 횟수 제한을 피할 수 없다)
    @Test
    void 대소문자와_공백만_다른_주소는_같은_이메일로_센다() throws Exception {
        authService.emailCodeSend(emailDto("a@snu.ac.kr"));

        CustomException tooSoon = assertThrows(CustomException.class, () -> authService.emailCodeSend(emailDto(" A@SNU.AC.KR ")));
        assertEquals(ErrorCode.VERIFY_CODE_RESEND_TOO_SOON, tooSoon.getErrorCode());
        assertEquals(null, redis.get("A@SNU.AC.KR"));

        redis.put("a@snu.ac.kr", "123456");
        String[] variants = {"a@snu.ac.kr", "A@snu.ac.kr", "a@SNU.ac.kr", " a@snu.ac.kr", "A@SNU.AC.KR"};
        for (String v : variants) {
            CustomException wrong = assertThrows(CustomException.class, () -> authService.emailVerification(verifyDto(v, "000000")));
            assertEquals(ErrorCode.VERIFY_CODE_NOT_MATCHED, wrong.getErrorCode());
        }
        CustomException blocked = assertThrows(CustomException.class, () -> authService.emailVerification(verifyDto("a@snu.ac.kr", "123456")));
        assertEquals(ErrorCode.VERIFY_CODE_TOO_MANY_ATTEMPTS, blocked.getErrorCode());

        // 비밀번호 찾기도 같다: 변형 주소의 오답이 같은 기록에 쌓인다
        redis.put("PWRESET:b@snu.ac.kr", "654321");
        String[] pwVariants = {"B@SNU.AC.KR", "b@snu.ac.kr", "B@snu.ac.kr", " b@snu.ac.kr", "b@SNU.AC.KR"};
        for (String v : pwVariants) {
            assertThrows(CustomException.class, () -> authService.passwordResetEmailVerify(verifyDto(v, "000000")));
        }
        CustomException pwBlocked = assertThrows(CustomException.class, () -> authService.passwordResetEmailVerify(verifyDto("B@snu.ac.kr", "654321")));
        assertEquals(ErrorCode.VERIFY_CODE_TOO_MANY_ATTEMPTS, pwBlocked.getErrorCode());
    }

    // 같은 이메일로 동시에 오답을 몰아 보내도 400 은 정확히 5번만 나가고 나머지는 429 다.
    @Test
    void 동시에_몰아쳐도_오답은_다섯번까지만_통과() throws Exception {
        redis.put("a@snu.ac.kr", "123456");
        slowFailRead = true;

        ExecutorService pool = Executors.newFixedThreadPool(40);
        AtomicInteger notMatched = new AtomicInteger();
        AtomicInteger tooMany = new AtomicInteger();
        List<Future<?>> futures = new ArrayList<>();
        for (int i = 0; i < 40; i++) {
            futures.add(pool.submit(() -> {
                try {
                    authService.emailVerification(verifyDto("a@snu.ac.kr", "000000"));
                } catch (CustomException e) {
                    (e.getErrorCode() == ErrorCode.VERIFY_CODE_NOT_MATCHED ? notMatched : tooMany).incrementAndGet();
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            }));
        }
        for (Future<?> f : futures) {
            f.get(10, TimeUnit.SECONDS);
        }
        pool.shutdown();

        assertEquals(5, notMatched.get());
        assertEquals(35, tooMany.get());
    }

    // 같은 이메일로 인증번호를 동시에 요청해도 메일은 한 번만 나간다.
    @Test
    void 동시에_재발송해도_메일은_한번만() throws Exception {
        ExecutorService pool = Executors.newFixedThreadPool(20);
        AtomicInteger accepted = new AtomicInteger();
        List<Future<?>> futures = new ArrayList<>();
        for (int i = 0; i < 20; i++) {
            futures.add(pool.submit(() -> {
                try {
                    authService.emailCodeSend(emailDto("a@snu.ac.kr"));
                    accepted.incrementAndGet();
                } catch (CustomException ignored) {
                    // 대기 중이라 거절됨
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            }));
        }
        for (Future<?> f : futures) {
            f.get(10, TimeUnit.SECONDS);
        }
        pool.shutdown();

        assertEquals(1, accepted.get());
        verify(mailSender, times(1)).send(any(SimpleMailMessage.class));
    }

    // Redis 만 쓰는 확인 메서드는 트랜잭션(DB 연결)을 걸지 않는다: 잠금에서 기다리는 동안 DB 연결 풀을 붙잡지 않도록.
    @Test
    void 인증_확인_메서드는_트랜잭션을_걸지_않는다() throws Exception {
        assertEquals(null, AuthService.class.getMethod("emailVerification", EmailVerifyReqDto.class)
                .getAnnotation(org.springframework.transaction.annotation.Transactional.class));
        assertEquals(null, AuthService.class.getMethod("passwordResetEmailVerify", EmailVerifyReqDto.class)
                .getAnnotation(org.springframework.transaction.annotation.Transactional.class));
    }
}
