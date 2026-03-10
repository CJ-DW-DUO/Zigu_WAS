package com.zigu.ziguwas.domains.report.entity;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum ReportCategory {
    IRRELEVANT_POST("거래와 무관한 게시물이에요"),
    FALSE_INFORMATION("허위 정보(가격, 내용)가 포함되어 있어요"),
    ADVERTISEMENT("광고, 홍보성 게시물이에요"),
    INAPPROPRIATE_CONTENT("부적절한 표현이 포함되어 있어요"),
    FRAUD("사기 또는 사기 의심이 돼요");

    private final String description;


}
