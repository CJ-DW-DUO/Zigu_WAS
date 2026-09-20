package com.zigu.ziguwas.domains.item.entity;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum PostType {

    SUPPLY("빌려드려요"),
    DEMAND("빌려주세요");

    private final String description;
}
