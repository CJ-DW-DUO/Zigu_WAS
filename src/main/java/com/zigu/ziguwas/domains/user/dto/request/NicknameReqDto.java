package com.zigu.ziguwas.domains.user.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class NicknameReqDto {

    @Schema(description = "닉네임", example = "지구새내기")
    @NotBlank(message = "닉네임은 필수 입력입니다.")
    private String nickname;
}
