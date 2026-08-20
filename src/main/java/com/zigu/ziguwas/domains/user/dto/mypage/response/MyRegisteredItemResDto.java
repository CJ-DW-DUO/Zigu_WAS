package com.zigu.ziguwas.domains.user.dto.mypage.response;

import com.zigu.ziguwas.domains.item.entity.Item;
import com.zigu.ziguwas.domains.item.entity.ItemImage;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.time.LocalDateTime;

@Getter
@RequiredArgsConstructor
@Builder
@Schema(description = "나의 등록한 item DTO")
public class MyRegisteredItemResDto {

    @Schema(description = "아이템Id", example = "1")
    private final Long itemId;

    @Schema(description = "제목", example = "제목입니다 빌려가세요 상태도 ..")
    private final String title;

    @Schema(description = "메인이미지 url", example = "사진 url~~~")
    private final String mainImageUrl;

    @Schema(description = "가격", example = "3000")
    private final Long dayPerPrice;

    @Schema(description = "등록 날짜", example = "2026-08-20T12:34:56")
    private final LocalDateTime createdAt;

    public static MyRegisteredItemResDto fromEntity(Item item) {
        return MyRegisteredItemResDto.builder()
                .itemId(item.getId())
                .title(item.getTitle())
                .mainImageUrl(item.getImageUrl().stream()
                        .filter(ItemImage::isMainImageUrl)
                        .map(ItemImage::getImageUrl)
                        .findFirst()
                        .orElse(null))
                .dayPerPrice(item.getDayPerPrice())
                .createdAt(item.getCreatedAt())
                .build();
    }
}
