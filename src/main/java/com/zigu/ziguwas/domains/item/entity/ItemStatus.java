package com.zigu.ziguwas.domains.item.entity;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum ItemStatus {

    // 등록 상태
    REGISTERED("등록됨"),
    RENTING("대여중");

    private final String description;

    /**
     * 게시글 유형에 맞는 상태 문구를 반환합니다.
     * 요청글(DEMAND)은 등록됨/대여중 대신 구하는 중/매칭완료로 표시한다.
     */
    public String descriptionFor(PostType postType) {
        if (postType == PostType.DEMAND) {
            return this == REGISTERED ? "구하는 중" : "매칭완료";
        }
        return description;
    }

}
