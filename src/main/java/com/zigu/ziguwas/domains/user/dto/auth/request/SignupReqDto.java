package com.zigu.ziguwas.domains.user.dto.auth.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class SignupReqDto {

    @Schema(description = "사용자 이메일", example = "zigu@example.com")
    @Email(message = "이메일 형식을 맞춰주세요")
    @NotBlank(message = "이메일은 필수 입력입니다.")
    private String email;

    @Schema(description = "사용자 닉네임", example = "도서관새내기")
    @NotBlank(message = "닉네임은 필수 입력입니다.")
    private String nickname;

    @Schema(description = "비밀번호", example = "zigu123123")
    @NotBlank(message = "비밀번호는 필수 입력입니다.")
    private String password;

    @Schema(description = "프로필 이미지 URL", example = "https://s3.ap-northeast-2.amazonaws.com/bucket/profile.jpg")
    private String profileImageUrl;

    @Schema(description = "약관 동의 여부 (동의해야 가입 가능)", example = "true")
    @AssertTrue(message = "약관에 동의해야 합니다.")
    private boolean termsAgreed;

}
