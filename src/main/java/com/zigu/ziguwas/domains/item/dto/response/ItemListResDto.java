package com.zigu.ziguwas.domains.item.dto.response;

import com.zigu.ziguwas.domains.item.entity.Item;
import com.zigu.ziguwas.domains.item.entity.ItemImage;
import io.swagger.v3.oas.annotations.media.Schema;

import lombok.Builder;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
@Builder
@Schema(description = "아이템 목록 조회 응답 DTO")
public class ItemListResDto {

    @Schema(description = "아이템 식별자", example = "1")
    private final Long itemId;

    @Schema(description = "아이템 제목", example = "제목입니다 빌려가세요")
    private final String title;

    @Schema(description = "1일 대여 가격", example = "3000")
    private final Long dayPerPrice;

    @Schema(description = "대표 이미지 URL", example = "https://zigu-bucket.s3.amazonaws.com/image.jpg")
    private final String mainImageUrl;

    // 아이템 세트
    @Schema(description = "아이템 상태", example = "REGISTERED", allowableValues = {"IN_PROGRESS","RETURNED","REGISTERED"})
    private final String itemStatus;
    @Schema(description = "아이템 상태 한글 명칭", example = "등록됨", allowableValues = {"IN_PROGRESS","RETURNED","REGISTERED"})
    private final String itemStatusKor;

    // 카테고리 세트
    @Schema(description = "카테고리", example = "ELECTRONICS")
    private final String itemCategory;
    @Schema(description = "카테고리 한글 명칭", example = "전자기기")
    private final String itemCategoryKor;

    public static ItemListResDto fromEntity(Item item) {
        return ItemListResDto.builder()
                .itemId(item.getId())
                .title(item.getTitle())
                .dayPerPrice(item.getDayPerPrice())
                .itemStatus(item.getItemStatus().name())
                .itemStatusKor(item.getItemStatus().getDescription())
                .itemCategory(item.getCategory().name())
                .itemCategoryKor(item.getCategory().getDescription())
                .mainImageUrl(item.getImageUrl().stream()
                        .filter(ItemImage::isMainImageUrl)
                        .map(ItemImage::getImageUrl)
                        .findFirst()
                        .orElse(null))
                .build();
    }
}
