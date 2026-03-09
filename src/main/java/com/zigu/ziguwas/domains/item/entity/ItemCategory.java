package com.zigu.ziguwas.domains.item.entity;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum ItemCategory {
    HOUSEHOLD_GOODS("생활용품"),
    ELECTRONICS("전자기기"),
    BOOKS_AND_ACADEMICS("도서/학업"),
    HOBBY_AND_LEISURE("취미/여가"),
    BEAUTY("뷰티");

    private final String description;
}
