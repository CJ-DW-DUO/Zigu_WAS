package com.zigu.ziguwas.domains.user.service;

import com.zigu.ziguwas.domains.trade.entity.Trade;
import com.zigu.ziguwas.domains.trade.entity.TradeStatus;
import com.zigu.ziguwas.domains.trade.repository.TradeRepository;
import com.zigu.ziguwas.domains.university.entity.University;
import com.zigu.ziguwas.domains.university.repository.UniversityRepository;
import com.zigu.ziguwas.domains.user.dto.auth.request.EmailReqDto;
import com.zigu.ziguwas.domains.user.dto.auth.request.EmailVerifyReqDto;
import com.zigu.ziguwas.domains.user.dto.auth.request.LoginReqDto;
import com.zigu.ziguwas.domains.user.dto.auth.request.PassWordUpdateReqDto;
import com.zigu.ziguwas.domains.user.dto.auth.request.SignupReqDto;
import com.zigu.ziguwas.domains.user.dto.auth.response.LoginResDto;
import com.zigu.ziguwas.domains.user.dto.auth.response.OngoingTradeResDto;
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
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final UniversityRepository universityRepository;
    private final JavaMailSender mailSender;
    private final RedisService redisService;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;
    private final TokenService tokenService;
    private final TradeRepository tradeRepository;


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
     * 이메일 인증코드 발송
     *
     * Redis를 이용한 랜덤 인증 코드 생성 후 발송
     *
     * @param dto 이메일
     */
    @Transactional
    public void emailCodeSend(EmailReqDto dto) {
        // 1. 이메일 검증
        validateEmail(dto.getEmail());

        // 2. 인증코드 생성 및 레디스 저장

        // 6자리 랜덤 인증 코드 생성
        String verificationCode = String.valueOf((int)(Math.random() * 899999) + 100000);

        // Redis에 저장 / 이메일, 인증코드, 5분
        redisService.setDataExpire(dto.getEmail(), verificationCode, 300 * 1000);

        // 3. 이메일 발송
        SimpleMailMessage message = new SimpleMailMessage();
        message.setTo(dto.getEmail());
        message.setSubject("[Zigu] 회원가입 인증 번호 안내");
        message.setText("인증 번호는 [" + verificationCode + "] 입니다. 5분 내에 입력해주세요.");
        mailSender.send(message);
    }


    /**
     * 이메일 코드 확인
     *
     * @param dto 이메일, 인증코드
     * @return 인증코드 일치 여부
     */
    @Transactional
    public void emailVerification(EmailVerifyReqDto dto) {

        // 1. Redis에 저장된 인증코드 불러오기
        String savedCode = redisService.getData(dto.getEmail());
        if(savedCode == null){
            // 코드 자체가 없다면 서비스 단 예외처리로 반환
            throw new CustomException(ErrorCode.VERIFY_CODE_NOT_FOUND);
        }

        // 2. 인증코드 매치 확인
        if(savedCode.equals(dto.getCode())){
            // 인증 코드는 삭제
            redisService.deleteData(dto.getEmail());
            // 인증된 메일 상태는 10분간 유지하도록
            redisService.setDataExpire(dto.getEmail(), "DONE", 600 * 1000);
        } else {
            // 인증 정보가 다를경우 예외
            throw new CustomException(ErrorCode.VERIFY_CODE_NOT_MATCHED);
        }

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
        // 1. 이메일 검증
        validateEmail(dto.getEmail());

        // 2. 해당 이메일이 현재 인증 상태인지
        if(redisService.getData(dto.getEmail()) == null || !redisService.getData(dto.getEmail()).equals("DONE")){
            throw new CustomException(ErrorCode.EMAIL_NOT_VERIFIED);
        }
        redisService.deleteData(dto.getEmail());

        // 3. 닉네임 검증
        validateNickname(dto.getNickname());

        // 4. 대학 찾기

        // 이메일에서 해당 대학 이메일 부분 추출
        String univDomain = dto.getEmail().substring(dto.getEmail().indexOf("@") + 1);

        // 대학 검색
        University univ = universityRepository.findByUnivEmail(univDomain).orElseThrow(
                () -> new CustomException(ErrorCode.UNIVERSITY_NOT_FOUND)
        );

        // 5. 회원가입
        // 아직 프로필사진 업로드 기능이 구현되지 않았습니다.
        return userRepository.save(User.builder()
                .nickname(dto.getNickname())
                .email(dto.getEmail())
                .password(passwordEncoder.encode(dto.getPassword()))
                .univ(univ)
                .profilePhotoUrl(null)
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

        // 3. JWT 토큰 발급
        String accessToken = createAccessToken(dto.getEmail());
        String refreshToken = createRefreshToken(dto.getEmail());

        // redis에 refresh token 저장
        tokenService.saveRefreshToken(user.getId(), refreshToken);

        // 4. 리프레쉬 토큰 헤더에 붙이는 작업
        ResponseCookie responseCookie = ResponseCookie.from("refreshToken", refreshToken)
                .httpOnly(true)
                .path("/")
                .maxAge(7*24*60*60) // 일주일
                .build();
        res.addHeader(HttpHeaders.SET_COOKIE, responseCookie.toString());

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
            throw new CustomException(ErrorCode.ACTIVE_TRADE_EXISTS);
        }

        // 토큰 처리
        tokenService.deleteRefreshToken(userId);
        long remainingTime = jwtUtil.getExpiration(accessToken);
        redisService.setDataExpire("BLACKLIST:" + accessToken, "withdrawn", remainingTime);

        // 개인정보 마스킹 및 사유 저장 (User 엔티티 내부 메서드 활용)
        user.applyWithdrawal(reason);
        userRepository.saveAndFlush(user);

        userRepository.delete(user);
    }




}
