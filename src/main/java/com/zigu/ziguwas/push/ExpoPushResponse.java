package com.zigu.ziguwas.push;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.List;

/**
 * Expo Push API 응답 전체 래퍼입니다. ({"data": [...]})
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record ExpoPushResponse(List<ExpoPushTicket> data) {
}
