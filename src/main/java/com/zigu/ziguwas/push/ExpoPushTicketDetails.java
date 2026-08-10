package com.zigu.ziguwas.push;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * 티켓 상태가 "error"일 때 Expo가 함께 내려주는 상세 에러 코드입니다.
 * error 값이 "DeviceNotRegistered"이면 앱이 삭제되었거나 토큰이 더 이상 유효하지 않다는 뜻입니다.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record ExpoPushTicketDetails(String error) {
}
