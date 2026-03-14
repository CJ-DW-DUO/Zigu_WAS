package com.zigu.ziguwas.domains.item.entity;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum ItemStatus {
    // 대여 상태
    IN_PROGRESS("대여중"),
    RETURNED("반납완료"),

    // 등록 상태
    RESISTED("등록됨");

    private final String description;


}
