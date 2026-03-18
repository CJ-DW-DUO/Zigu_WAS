package com.zigu.ziguwas.domains.user.dto.mypage.response;

import com.zigu.ziguwas.domains.item.entity.Item;
import com.zigu.ziguwas.domains.item.entity.ItemImage;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
@Builder
@Schema(description = "MyPage 등록한 물건 대표이미지 DTO")
public class MyItemSummaryResDto {

    @Schema(description = "아이템 ID", example = "1")
    private final Long itemId;

    @Schema(description = "해당 아이템 메인 이미지 URL", example = "메인이미지 URL~~")
    private final String mainImageUrl;

    public static MyItemSummaryResDto fromEntity(Item item) {
        return MyItemSummaryResDto.builder()
                .itemId(item.getId())
                .mainImageUrl(item.getImageUrl().stream()
                        .filter(ItemImage::isMainImageUrl)
                        .map(ItemImage::getImageUrl)
                        .findFirst().orElse(null))
                .build();
    }
}

