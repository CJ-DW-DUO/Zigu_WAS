package com.zigu.ziguwas.domains.user.service;

import com.zigu.ziguwas.domains.notification.repository.PushTokenRepository;
import com.zigu.ziguwas.domains.trade.entity.Trade;
import com.zigu.ziguwas.domains.trade.entity.TradeStatus;
import com.zigu.ziguwas.domains.trade.repository.TradeRepository;
import com.zigu.ziguwas.domains.university.entity.University;
import com.zigu.ziguwas.domains.university.repository.UniversityRepository;
import com.zigu.ziguwas.domains.user.dto.auth.request.EmailReqDto;
import com.zigu.ziguwas.domains.user.dto.auth.request.EmailVerifyReqDto;
import com.zigu.ziguwas.domains.user.dto.auth.request.LoginReqDto;
import com.zigu.ziguwas.domains.user.dto.auth.request.PassWordUpdateReqDto;
import com.zigu.ziguwas.domains.user.dto.auth.request.PasswordResetReqDto;
import com.zigu.ziguwas.domains.user.dto.auth.request.SignupReqDto;
import com.zigu.ziguwas.domains.user.dto.auth.response.LoginResDto;
import com.zigu.ziguwas.domains.user.dto.auth.response.OngoingTradeResDto;
import com.zigu.ziguwas.domains.user.dto.auth.response.TokenReissueResDto;
import com.zigu.ziguwas.domains.user.dto.auth.response.WithdrawalCheckResDto;
import com.zigu.ziguwas.domains.user.entity.NotificationSetting;
import com.zigu.ziguwas.domains.user.entity.User;
import com.zigu.ziguwas.domains.user.entity.VerificationStatus;
import com.zigu.ziguwas.domains.user.repository.UserRepository;
import com.zigu.ziguwas.exception.CustomException;
import com.zigu.ziguwas.exception.ErrorCode;
import com.zigu.ziguwas.redis.RedisService;
import com.zigu.ziguwas.security.JwtUtil;
import com.zigu.ziguwas.security.TokenService;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AuthService {

    // 회원가입용 이메일 인증 Redis key(이메일 그대로)와 겹치지 않도록 분리한 prefix
    private static final String PW_RESET_PREFIX = "PWRESET:";

    // 같은 이메일로 인증번호를 다시 요청할 수 있을 때까지의 대기 표시 / 오답 시도 횟수 기록 Redis key prefix
    // (인증코드 key 앞에 붙인다: 회원가입은 이메일 그대로, 비밀번호 재설정은 PWRESET:이메일)
    private static final String SEND_COOLDOWN_PREFIX = "VERIFYCOOL:";
    private static final String VERIFY_FAIL_PREFIX = "VERIFYFAIL:";

    // 인증번호 재발송 대기 시간(ms)
    private static final long SEND_COOLDOWN_MS = 60 * 1000L;

    // 인증번호 오답 허용 횟수와 그 횟수를 세는 시간(ms): 이메일별로 1분에 5번까지 틀릴 수 있다.
    private static final int MAX_VERIFY_FAILURES = 5;
    private static final long VERIFY_FAIL_WINDOW_MS = 60 * 1000L;

    // 같은 이메일의 인증번호 발송·확인을 한 번에 하나씩만 처리하기 위한 잠금 개수.
    // 이메일마다 잠금을 만들면 아무 이메일이나 보내서 메모리를 늘릴 수 있어, 고정된 개수의 잠금을 이메일 해시로 나눠 쓴다.
    // ponytail: JVM 안에서만 막는 잠금이라 서버 1대 전제다. 서버를 여러 대로 늘리면 횟수를 DB 에서 한 문장 upsert(…RETURNING)로 세는 방식으로 옮긴다.
    private static final int LOCK_STRIPES = 64;
    private final Object[] verifyLocks = createLocks();

    // 현재 서비스 약관 버전 (약관 개정 시 갱신)
    private static final String CURRENT_TERMS_VERSION = "v1.0";

    private final UserRepository userRepository;
    private final UniversityRepository universityRepository;
    private final JavaMailSender mailSender;
    private final RedisService redisService;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;
    private final TokenService tokenService;
    private final TradeRepository tradeRepository;
    private final PushTokenRepository pushTokenRepository;


    /**
     * 대학에 존재하는 도메인인지, 중복되지 않은 이메일인지 검증
     *
     * @param email 이메일
     */
    private void validateEmail(String email){

        // 1. 대학의 도메인인지 체크
        // @ 뒤에오는 이메일을 추출
        String domain = email.substring(email.indexOf("@") + 1);

        if(!universityRepository.existsByUnivEmail(domain)){
            throw new CustomException(ErrorCode.NOT_MATCHED_UNIV_EMAIL);
        }

        // 2. 중복이 없는지 체크
        if(userRepository.existsByEmail(email)){
            throw new CustomException(ErrorCode.EMAIL_CONFLICTED);
        }
    }


    /**
     * 닉네임 유효성 검증
     *
     * @param nickname 닉네임
     */
    private void validateNickname(String nickname){
        // 1. 중복되는지 검증
        if(userRepository.existsByNickname(nickname)){
            throw new CustomException(ErrorCode.NICKNAME_CONFLICT);
        }
    }

    /**
     * 이메일을 비교·저장에 쓰는 형태로 정리하는 서비스
     *
     * 앞뒤 공백을 지우고 소문자로 바꾼다. DB 는 대소문자를 구분하지 않고 찾으므로, 정리하지 않으면
     * 대소문자만 바꾼 주소마다 인증코드·재발송 대기·오답 횟수가 따로 생겨 횟수 제한을 피할 수 있다.
     *
     * @param email 요청으로 받은 이메일
     * @return 정리된 이메일
     */
    private String normalizeEmail(String email) {
        return email.trim().toLowerCase(Locale.ROOT);
    }

    private static Object[] createLocks() {
        Object[] locks = new Object[LOCK_STRIPES];
        for (int i = 0; i < LOCK_STRIPES; i++) {
            locks[i] = new Object();
        }
        return locks;
    }

    /**
     * 같은 이메일의 요청이 함께 쓰는 잠금을 반환
     *
     * @param codeKey 인증코드를 저장하는 Redis key
     */
    private Object lockFor(String codeKey) {
        return verifyLocks[Math.floorMod(codeKey.hashCode(), LOCK_STRIPES)];
    }

    /**
     * 같은 이메일로 인증번호를 너무 빨리 다시 요청했는지 검증하고, 아니면 재발송 대기를 건다
     *
     * 확인과 기록을 같은 잠금 안에서 하므로, 같은 순간에 여러 번 요청해도 한 번만 통과한다.
     *
     * @param codeKey 인증코드를 저장하는 Redis key (회원가입은 이메일, 비밀번호 재설정은 PWRESET:이메일)
     */
    private void reserveSendCooldown(String codeKey) {
        synchronized (lockFor(codeKey)) {
            if (redisService.getData(SEND_COOLDOWN_PREFIX + codeKey) != null) {
                throw new CustomException(ErrorCode.VERIFY_CODE_RESEND_TOO_SOON);
            }

            redisService.setDataExpire(SEND_COOLDOWN_PREFIX + codeKey, "1", SEND_COOLDOWN_MS);
        }
    }

    /**
     * 인증번호 메일을 보낸다
     *
     * 메일 발송이 실패하면 사용자가 바로 다시 요청할 수 있도록 재발송 대기를 푼다.
     *
     * @param codeKey 인증코드를 저장하는 Redis key
     * @param message 보낼 메일
     */
    private void sendCodeMail(String codeKey, SimpleMailMessage message) {
        try {
            mailSender.send(message);
        } catch (RuntimeException e) {
            redisService.deleteData(SEND_COOLDOWN_PREFIX + codeKey);
            throw e;
        }
    }

    /**
     * 인증번호를 이미 너무 많이 틀렸는지 검증
     *
     * @param codeKey 인증코드를 저장하는 Redis key
     */
    private void validateVerifyAttempts(String codeKey) {
        String record = redisService.getData(VERIFY_FAIL_PREFIX + codeKey);

        if (record == null) {
            return;
        }

        // 기록은 "틀린 횟수:처음 틀린 시각(ms)" 형태이고, 처음 틀린 때부터 1분 뒤에 Redis 에서 사라진다.
        if (Integer.parseInt(record.split(":")[0]) >= MAX_VERIFY_FAILURES) {
            throw new CustomException(ErrorCode.VERIFY_CODE_TOO_MANY_ATTEMPTS);
        }
    }

    /**
     * 인증번호를 틀린 횟수를 한 번 더 기록
     *
     * 새로 세기 시작한 시각을 함께 저장해 두고 남은 시간만큼만 보관하므로,
     * 틀릴 때마다 1분이 새로 시작되지 않고 처음 틀린 때부터 1분 동안 센다.
     *
     * @param codeKey 인증코드를 저장하는 Redis key
     */
    private void recordVerifyFailure(String codeKey) {
        String failKey = VERIFY_FAIL_PREFIX + codeKey;
        String record = redisService.getData(failKey);
        long now = System.currentTimeMillis();

        int count = 1;
        long startedAt = now;

        if (record != null) {
            String[] parts = record.split(":");
            count = Integer.parseInt(parts[0]) + 1;
            startedAt = Long.parseLong(parts[1]);
        }

        long remaining = VERIFY_FAIL_WINDOW_MS - (now - startedAt);

        // 기록이 만료되기 직전에 읽혀 남은 시간이 없으면 지금부터 새로 센다.
        if (remaining <= 0) {
            count = 1;
            startedAt = now;
            remaining = VERIFY_FAIL_WINDOW_MS;
        }

        redisService.setDataExpire(failKey, count + ":" + startedAt, remaining);
    }

    /**
     * 이메일 인증코드 발송
     *
     * Redis를 이용한 랜덤 인증 코드 생성 후 발송
     *
     * @param dto 이메일
     */
    @Transactional
    public void emailCodeSend(EmailReqDto dto) {
        String email = normalizeEmail(dto.getEmail());
        // 1. 이메일 검증
        validateEmail(email);

        // 같은 이메일로 너무 빨리 다시 요청하면 거절 (통과하면 재발송 대기도 함께 건다)
        reserveSendCooldown(email);

        // 2. 인증코드 생성 및 레디스 저장

        // 6자리 랜덤 인증 코드 생성
        String verificationCode = String.valueOf((int)(Math.random() * 899999) + 100000);

        // Redis에 저장 / 이메일, 인증코드, 5분
        redisService.setDataExpire(email, verificationCode, 300 * 1000);

        // 3. 이메일 발송
        SimpleMailMessage message = new SimpleMailMessage();
        message.setTo(email);
        message.setSubject("[Zigu] 회원가입 인증 번호 안내");
        message.setText("인증 번호는 [" + verificationCode + "] 입니다. 5분 내에 입력해주세요.");
        sendCodeMail(email, message);
    }


    /**
     * 이메일 코드 확인
     *
     * @param dto 이메일, 인증코드
     * @return 인증코드 일치 여부
     */
    @Transactional
    public void emailVerification(EmailVerifyReqDto dto) {
        String email = normalizeEmail(dto.getEmail());

        // 같은 이메일의 확인(횟수 확인 → 코드 비교 → 오답 기록)은 한 번에 하나씩만 처리해, 동시에 보내도 오답 횟수 제한이 넘어가지 않게 한다.
        synchronized (lockFor(email)) {
            // 오답을 너무 많이 낸 이메일은 코드가 맞더라도 잠시 거절
            validateVerifyAttempts(email);

            // 1. Redis에 저장된 인증코드 불러오기
            String savedCode = redisService.getData(email);
            if(savedCode == null){
                // 코드 자체가 없다면 서비스 단 예외처리로 반환
                throw new CustomException(ErrorCode.VERIFY_CODE_NOT_FOUND);
            }

            // 2. 인증코드 매치 확인
            if(savedCode.equals(dto.getCode())){
                // 인증 코드는 삭제
                redisService.deleteData(email);
                redisService.deleteData(VERIFY_FAIL_PREFIX + email);
                // 인증된 메일 상태는 10분간 유지하도록
                redisService.setDataExpire(email, "DONE", 600 * 1000);
            } else {
                // 인증 정보가 다를경우 틀린 횟수를 기록하고 예외
                recordVerifyFailure(email);
                throw new CustomException(ErrorCode.VERIFY_CODE_NOT_MATCHED);
            }
        }
    }

    /**
     * 비밀번호 재설정을 위한 이메일 인증코드 발송
     *
     * 회원가입과 반대로, 이미 가입되어 있는 이메일인지 확인 후 코드를 발송합니다.
     *
     * @param dto 이메일
     */
    @Transactional
    public void passwordResetEmailSend(EmailReqDto dto) {
        String email = normalizeEmail(dto.getEmail());
        // 1. 가입된 유저인지 확인
        if(!userRepository.existsByEmail(email)){
            throw new CustomException(ErrorCode.USER_NOT_FOUND);
        }

        // 같은 이메일로 너무 빨리 다시 요청하면 거절 (통과하면 재발송 대기도 함께 건다)
        reserveSendCooldown(PW_RESET_PREFIX + email);

        // 2. 인증코드 생성 및 레디스 저장 (회원가입용 key와 구분되는 prefix 사용)
        String verificationCode = String.valueOf((int)(Math.random() * 899999) + 100000);
        redisService.setDataExpire(PW_RESET_PREFIX + email, verificationCode, 300 * 1000);

        // 3. 이메일 발송
        SimpleMailMessage message = new SimpleMailMessage();
        message.setTo(email);
        message.setSubject("[Zigu] 비밀번호 재설정 인증 번호 안내");
        message.setText("인증 번호는 [" + verificationCode + "] 입니다. 5분 내에 입력해주세요.");
        sendCodeMail(PW_RESET_PREFIX + email, message);
    }

    /**
     * 비밀번호 재설정을 위한 이메일 인증코드 확인
     *
     * @param dto 이메일, 인증코드
     */
    @Transactional
    public void passwordResetEmailVerify(EmailVerifyReqDto dto) {
        String key = PW_RESET_PREFIX + normalizeEmail(dto.getEmail());

        // 같은 이메일의 확인(횟수 확인 → 코드 비교 → 오답 기록)은 한 번에 하나씩만 처리해, 동시에 보내도 오답 횟수 제한이 넘어가지 않게 한다.
        synchronized (lockFor(key)) {
            // 오답을 너무 많이 낸 이메일은 코드가 맞더라도 잠시 거절
            validateVerifyAttempts(key);

            // 1. Redis에 저장된 인증코드 불러오기
            String savedCode = redisService.getData(key);
            if(savedCode == null){
                throw new CustomException(ErrorCode.VERIFY_CODE_NOT_FOUND);
            }

            // 2. 인증코드 매치 확인
            if(savedCode.equals(dto.getCode())){
                // 인증 코드는 삭제하고, 인증된 상태를 10분간 유지
                redisService.deleteData(key);
                redisService.deleteData(VERIFY_FAIL_PREFIX + key);
                redisService.setDataExpire(key, "DONE", 600 * 1000);
            } else {
                recordVerifyFailure(key);
                throw new CustomException(ErrorCode.VERIFY_CODE_NOT_MATCHED);
            }
        }
    }

    /**
     * 인증 완료된 이메일의 비밀번호를 새 비밀번호로 즉시 변경
     *
     * 회원가입 때와 동일하게, 인증 완료 상태(Redis의 "DONE")를 확인한 뒤 바로 반영합니다.
     *
     * @param dto 이메일, 새 비밀번호, 확인용 비밀번호
     */
    @Transactional
    public void resetPassword(PasswordResetReqDto dto) {
        String email = normalizeEmail(dto.getEmail());
        String key = PW_RESET_PREFIX + email;

        // 1. 해당 이메일이 인증 완료 상태인지 확인
        if(!"DONE".equals(redisService.getData(key))){
            throw new CustomException(ErrorCode.EMAIL_NOT_VERIFIED);
        }

        // 2. 새 비밀번호와 확인용 비밀번호 일치 확인
        if(!dto.getNewPassword().equals(dto.getConfirmPassword())){
            throw new CustomException(ErrorCode.PASSWORD_MISMATCH);
        }

        // 3. 유저 조회 후 비밀번호 반영
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));
        user.updatePassWord(passwordEncoder.encode(dto.getNewPassword()));

        // 4. 인증 상태 정리
        redisService.deleteData(key);
    }

    /**
     * 닉네임 검증 단독 서비스
     *
     * 닉네임의 중복 여부만 판단하는 API를 위한 서비스입니다.
     *
     * @param nickname 닉네임
     */
    @Transactional
    public void nicknameCheck(String nickname) {

        // 닉네임 검증
        validateNickname(nickname);
    }

    /**
     * 회원가입 서비스
     *
     * @param dto 회원가입 정보
     * @return 생성된 사용자 객체
     */
    @Transactional
    public User signUp(SignupReqDto dto) {
        String email = normalizeEmail(dto.getEmail());
        // 1. 이메일 검증
        validateEmail(email);

        // 2. 해당 이메일이 현재 인증 상태인지
        if(redisService.getData(email) == null || !redisService.getData(email).equals("DONE")){
            throw new CustomException(ErrorCode.EMAIL_NOT_VERIFIED);
        }
        redisService.deleteData(email);

        // 3. 대학 찾기

        // 이메일에서 해당 대학 이메일 부분 추출
        String univDomain = email.substring(email.indexOf("@") + 1);

        // 대학 검색
        University univ = universityRepository.findByUnivEmail(univDomain).orElseThrow(
                () -> new CustomException(ErrorCode.UNIVERSITY_NOT_FOUND)
        );

        // 4. 회원가입
        return userRepository.save(User.builder()
                .nickname(dto.getNickname().trim())
                .studentNumber(dto.getStudentNumber())
                .gender(dto.getGender())
                .genderVisible(dto.isGenderVisible())
                .email(email)
                .password(passwordEncoder.encode(dto.getPassword()))
                .univ(univ)
                .profilePhotoUrl(dto.getProfileImageUrl())
                .termsAgreed(dto.isTermsAgreed())
                .termsVersion(CURRENT_TERMS_VERSION)
                .veriStatus(VerificationStatus.CERTIFIED)
                .notificationSetting(NotificationSetting.builder().build())
                .build());
    }

    /**
     * 로그인 시도 서비스
     *
     * @param dto 로그인 정보
     * @param res 응답 헤더
     */
    @Transactional
    public LoginResDto tryLogin(
            LoginReqDto dto,
            HttpServletResponse res
    ) {

        // 1. 해당 유저부터 존재하는지
        User user = userRepository.findByEmail(dto.getEmail()).orElseThrow(
                () -> new CustomException(ErrorCode.USER_NOT_FOUND)
        );

        // 2. 비밀번호 확인
        if(!passwordEncoder.matches(dto.getPassword(), user.getPassword())){
            throw new CustomException(ErrorCode.INCORRECT_PASSWORD);
        }

        // 기존 비밀번호를 기억해 로그인에 성공했다면, 이전에 요청했던 비밀번호 재설정 인증 상태(코드 발송/DONE)는 철회
        // (재설정을 요청했지만 실제로는 비밀번호를 기억해서 그냥 로그인한 케이스 방어)
        redisService.deleteData(PW_RESET_PREFIX + normalizeEmail(dto.getEmail()));

        // 3. JWT 토큰 발급
        String accessToken = createAccessToken(dto.getEmail());
        String refreshToken = createRefreshToken(dto.getEmail());

        // redis에 refresh token 저장
        tokenService.saveRefreshToken(user.getId(), refreshToken);

        // 4. 리프레쉬 토큰 헤더에 붙이는 작업
        ResponseCookie refreshCookie = ResponseCookie.from("refreshToken", refreshToken)
                .httpOnly(true)
                .path("/")
                .maxAge(7*24*60*60) // 일주일
                .build();
        res.addHeader(HttpHeaders.SET_COOKIE, refreshCookie.toString());

        // 5. 액세스 토큰 헤더에 붙이는 작업
        ResponseCookie accessCookie = ResponseCookie.from("accessToken", accessToken)
                .httpOnly(true)
                .path("/")
                .maxAge(12*60*60) // 12시간
                .build();
        res.addHeader(HttpHeaders.SET_COOKIE, accessCookie.toString());

        // 6. DTO 구성 반환
        return LoginResDto.builder()
                .userId(user.getId())
                .email(user.getEmail())
                .nickname(user.getNickname())
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .university(user.getUniv().getUnivName())
                .build();
    }

    /**
     * JWT AccessToken 발급
     *
     * @param email 이메일
     * @return AccessToken
     */
    public String createAccessToken(String email) {
        return jwtUtil.createAccessToken(email);
    }

    /**
     * JWT RefreshToken 발급
     *
     * @param email 이메일
     * @return RefreshToken
     */
    public String createRefreshToken(String email) {
        return jwtUtil.createRefreshToken(email);
    }

    /**
     * 리프레시 토큰으로 액세스 토큰을 재발급합니다.
     * 쿠키의 리프레시 토큰이 (1) 서명/만료 검증을 통과하고 (2) type이 refresh이며
     * (3) Redis에 저장된 값과 일치할 때만 재발급을 허용합니다.
     * 재발급 시 리프레시 토큰도 함께 회전(rotate)시켜 Redis 값을 갱신합니다.
     *
     * @param request 리프레시 토큰 쿠키를 읽기 위한 요청 객체
     * @param res 새 토큰을 쿠키로 내려주기 위한 응답 객체
     * @return 재발급된 AccessToken
     */
    @Transactional
    public TokenReissueResDto reissueToken(HttpServletRequest request, HttpServletResponse res) {
        // 1. 쿠키에서 리프레시 토큰 추출
        String refreshToken = jwtUtil.extractRefreshTokenFromCookie(request);
        if (refreshToken == null) {
            throw new CustomException(ErrorCode.TOKEN_NOT_FOUND);
        }

        // 2. 서명/만료 검증 및 토큰 타입 확인
        String email;
        try {
            if (!jwtUtil.isRefreshToken(refreshToken)) {
                throw new CustomException(ErrorCode.UNAUTHENTICATED_ACCESS);
            }
            email = jwtUtil.getEmailFromToken(refreshToken);
        } catch (JwtException | IllegalArgumentException e) {
            throw new CustomException(ErrorCode.JWT_TOKEN_PARSING_ERROR);
        }

        // 3. 유저 조회 (탈퇴 등으로 이미 없어졌을 수 있음)
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));

        // 4. Redis에 저장된 리프레시 토큰과 일치하는지 확인 (로그아웃/탈퇴로 무효화된 토큰 차단)
        String savedRefreshToken = tokenService.getRefreshToken(user.getId());
        if (savedRefreshToken == null || !savedRefreshToken.equals(refreshToken)) {
            throw new CustomException(ErrorCode.UNAUTHENTICATED_ACCESS);
        }

        // 5. 새 토큰 발급 및 리프레시 토큰 회전
        String newAccessToken = createAccessToken(email);
        String newRefreshToken = createRefreshToken(email);
        tokenService.saveRefreshToken(user.getId(), newRefreshToken);

        // 6. 쿠키 갱신

        // 리프레쉬 토큰 또한 계송 갱신시키는 회전형 토큰 전략 적용
        ResponseCookie refreshCookie = ResponseCookie.from("refreshToken", newRefreshToken)
                .httpOnly(true)
                .path("/")
                .maxAge(7 * 24 * 60 * 60)
                .build();
        res.addHeader(HttpHeaders.SET_COOKIE, refreshCookie.toString());

        ResponseCookie accessCookie = ResponseCookie.from("accessToken", newAccessToken)
                .httpOnly(true)
                .path("/")
                .maxAge(12 * 60 * 60)
                .build();
        res.addHeader(HttpHeaders.SET_COOKIE, accessCookie.toString());

        return TokenReissueResDto.builder()
                .accessToken(newAccessToken)
                .build();
    }



    /**
     * 사용자의 비밀번호를 확인하고 새 비밀번호로 업데이트합니다.
     *
     * @param userId 비밀번호를 변경할 사용자의 식별자
     * @param reqDto 기존 비밀번호, 새 비밀번호, 확인용 비밀번호를 담은 데이터 객체
     * @throws CustomException 비밀번호가 틀리거나 정책에 어긋날 경우 발생
     */
    @Transactional
    public void updatePassword(Long userId, PassWordUpdateReqDto reqDto) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));

        // 1. 기존 비밀번호 일치 확인 (matches 메서드 활용)
        if (!passwordEncoder.matches(reqDto.getOldPassword(), user.getPassword())) {
            throw new CustomException(ErrorCode.INVALID_OLD_PASSWORD);
        }

        // 2. 새 비밀번호와 확인용 비밀번호 일치 확인
        if (!reqDto.getNewPassword().equals(reqDto.getConfirmPassword())) {
            throw new CustomException(ErrorCode.PASSWORD_MISMATCH);
        }

        // 3. 기존 비밀번호와 동일한지 체크 (보안 강화)
        if (passwordEncoder.matches(reqDto.getNewPassword(), user.getPassword())) {
            throw new CustomException(ErrorCode.SAME_AS_OLD_PASSWORD);
        }

        // 4. 새 비밀번호 암호화 및 반영
        String encodedPassword = passwordEncoder.encode(reqDto.getNewPassword());
        user.updatePassWord(encodedPassword);
    }


    /**
     * 로그아웃 처리합니다.
     *
     * JWT는 서버가 상태를 들고 있지 않고 서명만으로 검증하는 방식이라, 새 토큰을 발급해도
     * 기존에 발급된 액세스 토큰은 만료 시각 전까지 그 자체로 계속 유효합니다 (재발급으로 "덮어쓰기"가 안 됨).
     * 그래서 탈퇴 처리와 동일한 방식으로, 남은 유효시간만큼 Redis 블랙리스트에 등록해
     * JwtAuthenticationFilter가 매 요청마다 걸러내도록 합니다.
     *
     * @param userId 로그아웃할 사용자 ID
     * @param accessToken 현재 액세스 토큰
     */
    public void logout(Long userId, String accessToken) {
        tokenService.deleteRefreshToken(userId);

        long remainingTime = jwtUtil.getExpiration(accessToken);
        redisService.setDataExpire("BLACKLIST:" + accessToken, "logout", remainingTime);
    }

    /**
     * 탈퇴 가능 상태를 조회하여 진행 중인 거래 목록을 반환합니다.
     * @param userId 유저 식별자
     * @return 탈퇴 가능 여부 및 상세 거래 정보
     */
    @Transactional(readOnly = true)
    public WithdrawalCheckResDto getWithdrawalStatus(Long userId) {
        List<Trade> trades = tradeRepository.findAllByUserIdInvolvedAndStatus(userId, TradeStatus.IN_PROGRESS);

        List<OngoingTradeResDto> ongoingTradeDtos = trades.stream()
                .map(OngoingTradeResDto::from)
                .collect(Collectors.toList());

        return WithdrawalCheckResDto.toEntity(ongoingTradeDtos);
    }

    /**
     * 회원을 소프트 탈퇴 처리하고 토큰을 무효화합니다.
     * @param userId 탈퇴할 유저 식별자
     * @param accessToken 현재 액세스 토큰
     * @param reason 탈퇴 사유
     */
    @Transactional
    public void withdraw(Long userId, String accessToken, String reason) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));

        // 진행 중 거래 존재 시 예외 발생
        List<Trade> activeTrades = tradeRepository.findAllByUserIdInvolvedAndStatus(userId, TradeStatus.IN_PROGRESS);
        if (!activeTrades.isEmpty()) {
            throw new CustomException(ErrorCode.WITHDRAW_ACTIVE_TRADE_EXISTS);
        }

        // 토큰 처리
        tokenService.deleteRefreshToken(userId);
        long remainingTime = jwtUtil.getExpiration(accessToken);
        redisService.setDataExpire("BLACKLIST:" + accessToken, "withdrawn", remainingTime);

        // 푸시 토큰 삭제 (탈퇴한 계정으로 알림이 계속 가지 않도록)
        pushTokenRepository.deleteByUserId(userId);

        // 개인정보 마스킹 및 사유 저장 (User 엔티티 내부 메서드 활용)
        user.applyWithdrawal(reason);
        userRepository.saveAndFlush(user);

        userRepository.delete(user);
    }




}
