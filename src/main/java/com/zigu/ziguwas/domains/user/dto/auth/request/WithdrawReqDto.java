package com.zigu.ziguwas.domains.user.dto.auth.request;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class WithdrawReqDto {

    @Schema(description = "탈퇴사유", example = "탈퇴하고 싶어서입니다.")
    private String reason; // 탈퇴 사유
}