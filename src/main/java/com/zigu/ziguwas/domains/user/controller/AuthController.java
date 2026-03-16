package com.zigu.ziguwas.domains.user.controller;

import com.zigu.ziguwas.domains.user.dto.request.EmailReqDto;
import com.zigu.ziguwas.domains.user.dto.request.EmailVerifyReqDto;
import com.zigu.ziguwas.domains.user.dto.request.LoginReqDto;
import com.zigu.ziguwas.domains.user.dto.request.SignupReqDto;
import com.zigu.ziguwas.domains.user.entity.User;
import com.zigu.ziguwas.domains.user.service.AuthService;
import com.zigu.ziguwas.exception.CustomException;
import com.zigu.ziguwas.exception.ErrorCode;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
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


    /**
     * 이메일 검증과 인증코드 발송 API
     *
     * @param dto 이메일
     * @return 이메일 전송 성공여부
     */
    @PostMapping("/email/send")
    public ResponseEntity<?> emailCodeSend(@Valid @RequestBody EmailReqDto dto) {
        authService.emailCodeSend(dto);
        return ResponseEntity.ok().build();
    }


    /**
     * 이메일로 전송된 인증번호 확인 API
     *
     * @param dto 이메일, 인증코드
     * @return 인증코드 일치 성공 || 인증코드 불일치 || 인증코드 미존재 결과
     */
    @PostMapping("/email/verify")
    public ResponseEntity<?> emailVerification(@Valid @RequestBody EmailVerifyReqDto dto) {
        authService.emailVerification(dto);
        return ResponseEntity.ok().build();
    }


    /**
     * 회원가입 API
     *
     * @param dto 회원가입 정보가 담긴 데이터 전송 객체
     * @return 회원의 PK가 담긴 자원의 위치
     */
    @PostMapping("/signup")
    public ResponseEntity<?> signUp(@Valid @RequestBody SignupReqDto dto){
        User user = authService.signUp(dto);
        if(user == null)
            throw new CustomException(ErrorCode.USER_CREATE_FAILED);
        return ResponseEntity.created(URI.create("/api/v1/user/" + user.getId())).build();
    }


    /**
     * 로그인 API
     *
     * @param dto 로그인 정보
     * @param res http에 담을 응답
     * @return 로그인 정보, 유저 ID, AccessToken, RefreshToken
     */
    @PostMapping("/login")
    public ResponseEntity<?> login(
            @Valid @RequestBody LoginReqDto dto,
            HttpServletResponse res
    ){
        return ResponseEntity.ok().body(authService.tryLogin(dto, res));
    }


    // 회원탈퇴


    // 로그아웃
}
