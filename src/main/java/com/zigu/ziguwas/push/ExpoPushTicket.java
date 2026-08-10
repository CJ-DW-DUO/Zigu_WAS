package com.zigu.ziguwas.push;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * Expo Push API가 메시지 1건마다 즉시 돌려주는 접수 결과(티켓)입니다.
 * 실제 기기에 도착했는지까지는 보장하지 않고, "Expo가 접수했는지"만 알려줍니다.
 *
 * status가 "error"이고 details.error가 "DeviceNotRegistered"인 경우,
 * 해당 토큰은 더 이상 유효하지 않으므로 저장소에서 삭제 대상입니다.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record ExpoPushTicket(
        String status,
        String id,
        String message,
        ExpoPushTicketDetails details
) {
    public boolean isDeviceNotRegistered() {
        return details != null && "DeviceNotRegistered".equals(details.error());
    }
}
