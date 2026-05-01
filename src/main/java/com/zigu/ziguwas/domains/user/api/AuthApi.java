package com.zigu.ziguwas.domains.user.api;

import com.zigu.ziguwas.domains.user.dto.auth.request.EmailReqDto;
import com.zigu.ziguwas.domains.user.dto.auth.request.EmailVerifyReqDto;
import com.zigu.ziguwas.domains.user.dto.auth.request.LoginReqDto;
import com.zigu.ziguwas.domains.user.dto.auth.request.NicknameReqDto;
import com.zigu.ziguwas.domains.user.dto.auth.request.PassWordUpdateReqDto;
import com.zigu.ziguwas.domains.user.dto.auth.request.SignupReqDto;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestBody;

@Tag(name = "Auth API", description = "인증/인가 관련 API 입니다.")
public interface AuthApi {

    @Operation(summary = "이메일 인증코드 발송", description = "회원가입을 위한 이메일 중복 검증 및 인증코드를 발송합니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "이메일 전송 성공"),
            @ApiResponse(responseCode = "400", description = "잘못된 이메일 형식",
                    content = @Content(mediaType = "application/json", examples = {
                            @ExampleObject(value = "{\"status\": 400, \"message\": \"이메일 형식을 맞춰주세요\"}")
                    })),
            @ApiResponse(responseCode = "409", description = "이미 가입된 이메일",
                    content = @Content(mediaType = "application/json", examples = {
                            @ExampleObject(value = "{\"status\": 409, \"message\": \"이미 존재하는 이메일입니다.\"}")
                    }))
    })
    ResponseEntity<?> emailCodeSend(@Valid @RequestBody EmailReqDto dto);


    @Operation(summary = "이메일 인증번호 확인", description = "사용자가 입력한 인증번호가 유효한지 확인합니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "인증 성공"),
            @ApiResponse(responseCode = "400", description = "인증번호 불일치 혹은 만료",
                    content = @Content(mediaType = "application/json", examples = {
                            @ExampleObject(value = "{\"status\": 400, \"message\": \"인증 코드가 일치하지 않거나 만료되었습니다.\"}")
                    }))
    })
    ResponseEntity<?> emailVerification(@Valid @RequestBody EmailVerifyReqDto dto);


    @Operation(summary = "닉네임 중복 확인", description = "사용 가능한 닉네임인지 중복 여부를 확인합니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "사용 가능한 닉네임"),
            @ApiResponse(responseCode = "409", description = "이미 존재하는 닉네임",
                    content = @Content(mediaType = "application/json", examples = {
                            @ExampleObject(value = "{\"status\": 409, \"message\": \"이미 존재하는 닉네임입니다.\"}")
                    }))
    })
    ResponseEntity<?> nicknameCheck(@Valid @RequestBody NicknameReqDto dto);


    @Operation(summary = "회원가입", description = "필요한 정보를 입력받아 신규 회원으로 등록합니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "회원가입 성공"),
            @ApiResponse(responseCode = "400", description = "입력값 누락 혹은 형식 오류",
                    content = @Content(mediaType = "application/json", examples = {
                            @ExampleObject(value = "{\"status\": 400, \"message\": \"비밀번호는 필수 입력입니다.\"}")
                    })),
            @ApiResponse(responseCode = "500", description = "회원가입 처리 실패",
                    content = @Content(mediaType = "application/json", examples = {
                            @ExampleObject(value = "{\"status\": 500, \"message\": \"회원가입에 실패하였습니다.\"}")
                    }))
    })
    ResponseEntity<?> signUp(@Valid @RequestBody SignupReqDto dto);


    @Operation(summary = "로그인", description = "이메일과 비밀번호를 통해 로그인을 수행하고 토큰을 발급합니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "로그인 성공",
                    content = @Content(mediaType = "application/json", examples = {
                            @ExampleObject(value = """
                                    {
                                        "userId": 1,
                                        "email": "zigu@example.com",
                                        "nickname": "지구헌내기",
                                        "accessToken": "eyJhbGciOiJIUzI1NiJ9...",
                                        "refreshToken": "eyJhbGciOiJIUzI1NiJ9..."
                                    }
                                    """)
                    })),
            @ApiResponse(responseCode = "401", description = "로그인 실패 (비밀번호 불일치 등)",
                    content = @Content(mediaType = "application/json", examples = {
                            @ExampleObject(value = "{\"status\": 401, \"message\": \"비밀번호가 일치하지 않습니다.\"}")
                    })),
            @ApiResponse(responseCode = "404", description = "존재하지 않는 사용자",
                    content = @Content(mediaType = "application/json", examples = {
                            @ExampleObject(value = "{\"status\": 404, \"message\": \"해당 사용자를 찾을 수 없습니다.\"}")
                    }))
    })
    ResponseEntity<?> login(@Valid @RequestBody LoginReqDto dto, HttpServletResponse res);

    @Tag(name = "User Auth", description = "사용자 인증 및 계정 관리 API")
    public interface UserAuthApi {

        /**
         * 사용자의 비밀번호를 확인하고 새 비밀번호로 변경합니다.
         *
         * @param userId 인증된 사용자 식별자
         * @param reqDto 기존 비밀번호, 새 비밀번호 정보
         * @return 성공 시 200 OK
         */
        @Operation(
                summary = "비밀번호 변경",
                description = "현재 비밀번호와 대조하여 일치할 경우 새로운 비밀번호로 업데이트합니다."
        )
        @ApiResponses({
                @ApiResponse(
                        responseCode = "200",
                        description = "비밀번호 변경 성공",
                        content = @Content(schema = @Schema(implementation = Void.class))
                ),
                @ApiResponse(
                        responseCode = "401",
                        description = "인증 실패 및 비밀번호 검증 오류",
                        content = @Content(examples = {
                                @ExampleObject(name = "인증되지 않은 사용자", value = """
                                { "status": 401, "message": "인증되지 않은 사용자 입니다." }
                                """),
                                @ExampleObject(name = "기존 비밀번호 불일치", value = """
                                { "status": 401, "message": "기존 비밀번호와 입력하신 비밀번호는 일치하지 않습니다." }
                                """),
                                @ExampleObject(name = "새 비밀번호 확인 불일치", value = """
                                { "status": 401, "message": "새로운 비밀번호와 일치하지 않습니다." }
                                """),
                                @ExampleObject(name = "기존 비밀번호와 동일", value = """
                                { "status": 401, "message": "새로운 비밀번호와 기존비밀번호가 동일합니다." }
                                """)
                        })
                ),
                @ApiResponse(
                        responseCode = "403",
                        description = "접근 권한 없음",
                        content = @Content(examples = @ExampleObject(value = """
                        { "status": 403, "message": "허용되지 않은 접근입니다." }
                        """))
                ),
                @ApiResponse(
                        responseCode = "404",
                        description = "사용자 정보 없음",
                        content = @Content(examples = @ExampleObject(value = """
                        { "status": 404, "message": "사용자를 찾을 수 없습니다." }
                        """))
                ),
                @ApiResponse(
                        responseCode = "500",
                        description = "서버 오류",
                        content = @Content(examples = @ExampleObject(value = """
                        { "status": 500, "message": "내부 서버 오류입니다." }
                        """))
                )
        })
        ResponseEntity<Void> updatePassword(Long userId, PassWordUpdateReqDto reqDto);
    }
}
