package com.zigu.ziguwas.domains.trade.dto.response;

import com.zigu.ziguwas.domains.item.entity.ItemImage;
import com.zigu.ziguwas.domains.trade.entity.Trade;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
@Builder
@Schema(description = "마이페이지의 거래 관련 DTO (받은요청 빼고)")
public class MyPageTradeListResDto {

    @Schema(description = "거래 ID", example = "1")
    private final Long tradeId;

    @Schema(description = "아이템 ID", example = "2")
    private final Long itemId;

    @Schema(description = "제목", example = "제목입니다")
    private final String title;

    @Schema(description = "빌린기간", example = "30")
    private final Long period;

    @Schema(description = "총 가격", example = "50000")
    private final Long totalPrice;

    @Schema(description = "메인이미지 url", example = "이미지url~~~~~~")
    private final String mainImageUrl;

    @Schema(description = "거래 상태", example = "반납 완료")
    private final String tradeStatus;

    public static MyPageTradeListResDto fromEntity(Trade trade) {
        return MyPageTradeListResDto.builder()
                .tradeId(trade.getId())
                .itemId(trade.getItem().getId())
                .title(trade.getItem().getTitle())
                .period(trade.getPeriod())
                .totalPrice(trade.calculateTotalPrice())
                .mainImageUrl(trade.getItem().getImageUrl().stream()
                        .filter(ItemImage::isMainImageUrl)
                        .map(ItemImage::getImageUrl)
                        .findFirst()
                        .orElse(null))
                .tradeStatus(trade.getTradeStatus().getDescription())
                .build();
    }

}
