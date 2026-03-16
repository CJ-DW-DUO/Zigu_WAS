package com.zigu.ziguwas.domains.user.service;

import com.zigu.ziguwas.domains.university.repository.UniversityRepository;
import com.zigu.ziguwas.domains.user.dto.request.EmailReqDto;
import com.zigu.ziguwas.domains.user.repository.UserRepository;
import com.zigu.ziguwas.exception.CustomException;
import com.zigu.ziguwas.exception.ErrorCode;
import com.zigu.ziguwas.redis.RedisService;
import com.zigu.ziguwas.security.JwtUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final UniversityRepository universityRepository;
    private final JavaMailSender mailSender;
    private final RedisService redisService;


    public void emailValidation(EmailReqDto dto) {
        // 1. 대학의 도메인인지 체크
        // @ 뒤에오는 이메일을 추출
        String domain = dto.getEmail().substring(dto.getEmail().indexOf("@") + 1);

        if(!universityRepository.existsByUnivEmail(domain)){
            throw new CustomException(ErrorCode.NOT_MATCHED_UNIV_EMAIL);
        }

        // 2. 중복이 없는지 체크
        if(!userRepository.existsByEmail(dto.getEmail())){
            throw new CustomException(ErrorCode.EMAIL_CONFLICTED);
        }

        // 3. 인증코드 생성 및 레디스 저장

        // 6자리 랜덤 인증 코드 생성
        String verificationCode = String.valueOf((int)(Math.random() * 899999) + 100000);

        // Redis에 저장 / 이메일, 인증코드, 300초
        redisService.setDataExpire(dto.getEmail(), verificationCode, 300);

        // 4. 이메일 발송
        SimpleMailMessage message = new SimpleMailMessage();
        message.setTo(dto.getEmail());
        message.setSubject("[Zigu] 회원가입 인증 번호 안내");
        message.setText("인증 번호는 [" + verificationCode + "] 입니다. 5분 내에 입력해주세요.");
        mailSender.send(message);
    }






}
