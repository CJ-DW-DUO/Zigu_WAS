package com.zigu.ziguwas.domains.search.dto.response;

import com.zigu.ziguwas.domains.search.entity.SearchHistory;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@Builder
@RequiredArgsConstructor
@Schema(description = "검색 히스토리 응답 DTO")
public class SearchHistoryResDto {
    @Schema(description = "히스토리 식별자", example = "1")
    private final Long searchId;

    @Schema(description = "검색어", example = "맥북")
    private final String searchName;

    public static SearchHistoryResDto fromEntity(SearchHistory history) {
        return SearchHistoryResDto.builder()
                .searchId(history.getSearchId())
                .searchName(history.getSearchName())
                .build();
    }
}