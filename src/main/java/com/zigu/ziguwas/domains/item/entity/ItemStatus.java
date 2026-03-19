package com.zigu.ziguwas.domains.item.entity;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum ItemStatus {

    // 등록 상태
    REGISTERED("등록됨");

    private final String description;


}
