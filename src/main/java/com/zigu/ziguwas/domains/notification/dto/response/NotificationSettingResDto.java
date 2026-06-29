package com.zigu.ziguwas.domains.notification.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

@Getter
@AllArgsConstructor
@Builder
public class NotificationSettingResDto {

    @Schema(description = "사용자 ID", example = "1")
    private Long userId;

    @Schema(description = "채팅 알림 수신 여부", example = "true")
    private boolean chatNotiEnabled;

    @Schema(description = "거래 알림 수신 여부", example = "true")
    private boolean tradeNotiEnabled;

    @Schema(description = "마케팅 알림 수신 여부", example = "false")
    private boolean marketingNotiEnabled;

}
