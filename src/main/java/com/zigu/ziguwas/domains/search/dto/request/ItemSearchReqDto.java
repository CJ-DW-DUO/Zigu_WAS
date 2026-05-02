package com.zigu.ziguwas.domains.search.dto.request;

import com.zigu.ziguwas.domains.item.entity.ItemCategory;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@Schema(description = "아이템 검색 요청 DTO")
public class ItemSearchReqDto {

    @Schema(description = "검색 키워드 (2자 이상)", example = "맥북")
    @Size(min = 2, message = "검색어는 최소 2자 이상 입력해주세요.")
    private String keyword;

    @Schema(description = "카테고리 필터", example = "ELECTRONICS")
    private ItemCategory category;

    @Schema(description = "정렬 기준 (인기순, 낮은가격순, 최신순)", example = "최신순")
    private String sort;
}