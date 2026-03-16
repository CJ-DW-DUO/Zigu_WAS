package com.zigu.ziguwas.domains.user.service;

import com.zigu.ziguwas.domains.university.repository.UniversityRepository;
import com.zigu.ziguwas.domains.user.dto.request.EmailReqDto;
import com.zigu.ziguwas.domains.user.dto.request.EmailVerifyReqDto;
import com.zigu.ziguwas.domains.user.dto.request.SignupReqDto;
import com.zigu.ziguwas.domains.user.entity.User;
import com.zigu.ziguwas.domains.user.repository.UserRepository;
import com.zigu.ziguwas.exception.CustomException;
import com.zigu.ziguwas.exception.ErrorCode;
import com.zigu.ziguwas.redis.RedisService;
import lombok.RequiredArgsConstructor;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final UniversityRepository universityRepository;
    private final JavaMailSender mailSender;
    private final RedisService redisService;


    /**
     * 대학에 존재하는 도메인인지, 중복되지 않은 이메일인지 검증
     *
     * @param email 이메일
     */
    private void emailValidate(String email){

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
     * 이메일 인증코드 발송
     *
     * Redis를 이용한 랜덤 인증 코드 생성 후 발송
     *
     * @param dto 이메일
     */
    @Transactional
    public void emailCodeSend(EmailReqDto dto) {
        // 1. 이메일 검증
        emailValidate(dto.getEmail());

        // 2. 인증코드 생성 및 레디스 저장

        // 6자리 랜덤 인증 코드 생성
        String verificationCode = String.valueOf((int)(Math.random() * 899999) + 100000);

        // Redis에 저장 / 이메일, 인증코드, 300초
        redisService.setDataExpire(dto.getEmail(), verificationCode, 300);

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
    public boolean emailVerification(EmailVerifyReqDto dto) {

        // 1. Redis에 저장된 인증코드 불러오기
        String savedCode = redisService.getData(dto.getEmail());
        if(savedCode == null){
            // 코드 자체가 없다면 서비스 단 예외처리로 반환
            throw new CustomException(ErrorCode.VERIFY_CODE_NOT_FOUND);
        }

        // 2. 인증코드 매치 확인
        boolean result = savedCode.equals(dto.getCode());
        if(result){
            // 인증 코드는 삭제
            redisService.deleteData(dto.getEmail());
            // 인증된 메일 상태는 10분간 유지하도록
            redisService.setDataExpire(dto.getEmail(), "DONE", 600);
        }

        // 3. 매칭 결과 반환
        return result;
    }

    @Transactional
    public User signUp(@Valid SignupReqDto dto) {
        return null;
    }
}
