package com.zigu.ziguwas.domains.user.dto.auth.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class LoginReqDto {

    @Schema(description = "사용자 이메일", example = "zigu@example.com")
    @Email(message = "이메일 형식을 맞춰주세요")
    @NotBlank(message = "이메일은 필수 입력입니다.")
    private String email;

    @Schema(description = "비밀번호", example = "zigu123123")
    @NotBlank(message = "비밀번호는 필수 입력입니다.")
    private String password;
}
