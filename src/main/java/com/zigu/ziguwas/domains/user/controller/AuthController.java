package com.zigu.ziguwas.domains.user.controller;

import com.zigu.ziguwas.domains.university.repository.UniversityRepository;
import com.zigu.ziguwas.domains.user.dto.request.EmailReqDto;
import com.zigu.ziguwas.domains.user.dto.request.SignupReqDto;
import com.zigu.ziguwas.domains.user.entity.User;
import com.zigu.ziguwas.domains.user.repository.UserRepository;
import com.zigu.ziguwas.domains.user.service.AuthService;
import com.zigu.ziguwas.exception.CustomException;
import com.zigu.ziguwas.exception.ErrorCode;
import com.zigu.ziguwas.redis.RedisService;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/auth")
public class AuthController {

    private final AuthService authService;
    private final UserRepository userRepository;
    private final UniversityRepository universityRepository;
    private final RedisService redisService;


    // 이메일 중복, 이메일 학교 도메인 확인 및 인증 코드 발송
    @PostMapping("/email/send")
    public ResponseEntity<?> emailValidation(@Valid @RequestBody EmailReqDto dto) {

        authService.emailValidation(dto);

        return ResponseEntity.ok().build();
    }


    // 인증 코드 확인
    @PostMapping("/email/verify")
    public ResponseEntity<?> emailVerification(@Valid @RequestBody SignupReqDto signupReqDto) {

        // 1. 인증번호가 일치하는지 체크


        return ResponseEntity.ok().build();
    }


    /**
     * 회원가입 API 입니다.
     * @param dto 회원가입 정보가 담긴 데이터 전송 객체
     * @return 회원의 PK가 담긴 자원의 위치
     */
//    @PostMapping("/signup")
//    public ResponseEntity<?> signUp(@Valid @RequestBody SignupReqDto dto){
//        User user = authService.signUp(dto);
//        if(user == null)
//            throw new CustomException(ErrorCode.USER_CREATE_FAILED);
//        return ResponseEntity.created(URI.create("/api/v1/user/" + user.getId())).build();
//    }

    // 로그인


    // 회원탈퇴


    // 로그아웃
}
