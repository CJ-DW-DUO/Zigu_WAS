package com.zigu.ziguwas.domains.notification.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class PushTokenDeleteReqDto {

    @Schema(description = "삭제할 Expo 푸시 토큰", example = "ExponentPushToken[xxxxxxxxxxxxxxxxxxxxxx]")
    @NotBlank(message = "토큰은 필수 입력입니다.")
    private String token;
}
