package com.zigu.ziguwas.domains.user.dto.auth.response;

import com.zigu.ziguwas.domains.item.entity.ItemImage;
import com.zigu.ziguwas.domains.trade.entity.Trade;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@Builder
@RequiredArgsConstructor
public class OngoingTradeResDto {

    @Schema(description = "거래 고유 식별자", example = "1")
    private final Long tradeId;

    @Schema(description = "아이템 고유 식별자", example = "105")
    private final Long itemId;

    @Schema(description = "임대인(물건 주인) 식별자", example = "12")
    private final Long renterId;

    @Schema(description = "임차인(빌리는 사람) 식별자", example = "45")
    private final Long lesseeId;

    @Schema(description = "아이템 제목", example = "맥북 에어 M2 빌려가세요")
    private final String title;

    @Schema(description = "거래 진행 상태", example = "IN_PROGRESS")
    private final String status;

    @Schema(description = "일일 대여 가격", example = "15000")
    private final Long price;

    @Schema(description = "메인이미지 url", example = "이미지url~~~~~~")
    private final String mainImageUrl;

    public static OngoingTradeResDto from(Trade trade) {
        return OngoingTradeResDto.builder()
                .tradeId(trade.getId())
                .itemId(trade.getItem().getId())
                .renterId(trade.getRenter().getId())
                .lesseeId(trade.getRentee().getId())
                .title(trade.getItem().getTitle())
                .status(trade.getTradeStatus().getDescription())
                .price(trade.getItem().getDayPerPrice())
                .mainImageUrl(trade.getItem().getImageUrl().stream()
                        .filter(ItemImage::isMainImageUrl)
                        .map(ItemImage::getImageUrl)
                        .findFirst()
                        .orElse(null))
                .build();
    }
}
