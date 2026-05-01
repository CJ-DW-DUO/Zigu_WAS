package com.zigu.ziguwas.domains.user.dto.auth.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

@AllArgsConstructor
@Getter
@Builder
@Schema(description = "비밀번호 변경요청 DTO")
public class PassWordUpdateReqDto {

    @Schema(description = "기존 비밀번호", example = "adbae12")
    @NotBlank(message = "기존 비밀번호 입력은 필수입니다.")
    private String oldPassword;

    @Schema(description = "새 비밀번호", example = "newpass123!")
    @NotBlank(message = "새 비밀번호 입력은 필수입니다.")
    private String newPassword;

    @Schema(description = "새 비밀번호 확인", example = "newpass123!")
    @NotBlank(message = "비밀번호 확인 입력은 필수입니다.")
    private String confirmPassword;

}
