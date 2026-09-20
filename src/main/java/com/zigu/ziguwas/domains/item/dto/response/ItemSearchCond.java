package com.zigu.ziguwas.domains.item.dto.response;

import com.zigu.ziguwas.domains.item.entity.ItemCategory;
import com.zigu.ziguwas.domains.item.entity.PostType;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class ItemSearchCond {
    private ItemCategory category; // 카테고리
    private PostType postType;     // 게시글 유형 (미지정 시 SUPPLY만 조회)
    private String sort;           // "인기순", "낮은가격순", "최신순"
}