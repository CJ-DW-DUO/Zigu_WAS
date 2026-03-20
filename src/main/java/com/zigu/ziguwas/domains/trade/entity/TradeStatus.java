package com.zigu.ziguwas.domains.trade.entity;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum TradeStatus {

    // 대여 상태
    IN_PROGRESS("대여중"),
    RETURNED("반납완료"),

    // 대여 요청 상태
    REQUESTED("요청됨(대기중)"),
    REJECTED("거절됨");

    private final String description;
}
