package com.zigu.ziguwas.domains.user.controller;

import com.zigu.ziguwas.domains.user.api.AuthApi;
import com.zigu.ziguwas.domains.user.dto.auth.request.EmailReqDto;
import com.zigu.ziguwas.domains.user.dto.auth.request.EmailVerifyReqDto;
import com.zigu.ziguwas.domains.user.dto.auth.request.LoginReqDto;
import com.zigu.ziguwas.domains.user.dto.auth.request.NicknameReqDto;
import com.zigu.ziguwas.domains.user.dto.auth.request.PassWordUpdateReqDto;
import com.zigu.ziguwas.domains.user.dto.auth.request.SignupReqDto;
import com.zigu.ziguwas.domains.user.entity.User;
import com.zigu.ziguwas.domains.user.service.AuthService;
import com.zigu.ziguwas.exception.CustomException;
import com.zigu.ziguwas.exception.ErrorCode;
import com.zigu.ziguwas.security.CustomUserDetails;
import com.zigu.ziguwas.security.JwtUtil;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/auth")
public class AuthController implements AuthApi {

    private final AuthService authService;
    private final JwtUtil jwtUtil;


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
     * 닉네임 검증 단독 API
     *
     * @param dto 닉네임
     * @return 검증 성공 || 실패
     */
    @PostMapping("/nickname-check")
    public ResponseEntity<?> nicknameCheck(@Valid @RequestBody NicknameReqDto dto) {
        authService.nicknameCheck(dto.getNickname());
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


    /**
     * 사용자의 비밀번호를 변경합니다.
     *
     * @param customUserDetails 현재 로그인한 사용자의 인증된 객체
     * @param reqDto 비밀번호 변경 요청 정보
     * @return 성공 여부를 담은 응답
     */
    @PatchMapping("/password")
    public ResponseEntity<Void> updatePassword(
            @AuthenticationPrincipal CustomUserDetails customUserDetails,
            @Valid @RequestBody PassWordUpdateReqDto reqDto
    ) {
        authService.updatePassword(customUserDetails.getUserId(), reqDto);
        return ResponseEntity.ok().build();
    }

    /**
     * 로그인된 유저의 토큰 정보를 기반으로 회원 탈퇴를 진행한다.
     * @param customUserDetails AuthenticationPrincipal 어노테이션을 통해 주입받은 유저 정보
     * @return 성공 시 상태 코드 반환
     */
    @DeleteMapping
    public ResponseEntity<Void> withdraw(
            @AuthenticationPrincipal CustomUserDetails customUserDetails ,
            HttpServletRequest request
    ) {

        Long userId = customUserDetails.getUserId();
        String accessToken = jwtUtil.resolveToken(request);
        authService.withdraw(userId, accessToken);

        return ResponseEntity.noContent().build();
    }
}
