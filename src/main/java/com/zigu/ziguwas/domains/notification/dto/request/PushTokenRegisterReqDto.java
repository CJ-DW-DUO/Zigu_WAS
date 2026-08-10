package com.zigu.ziguwas.domains.notification.dto.request;

import com.zigu.ziguwas.domains.notification.entity.PushPlatform;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class PushTokenRegisterReqDto {

    @Schema(description = "Expo 푸시 토큰", example = "ExponentPushToken[xxxxxxxxxxxxxxxxxxxxxx]")
    @NotBlank(message = "토큰은 필수 입력입니다.")
    private String token;

    @Schema(description = "기기 플랫폼", example = "ios")
    @NotNull(message = "플랫폼은 필수 입력입니다.")
    private PushPlatform platform;
}
