package com.zigu.ziguwas.domains.user.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class EmailVerifyReqDto {

    @Schema(description = "사용자 이메일", example = "zigu@example.com")
    @Email(message = "이메일 형식을 맞춰주세요")
    @NotBlank(message = "이메일은 필수 입력입니다.")
    private String email;

    @Schema(description = "인증 코드", example = "735560")
    private String code;
}
