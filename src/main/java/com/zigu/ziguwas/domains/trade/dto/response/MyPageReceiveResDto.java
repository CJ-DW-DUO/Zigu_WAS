package com.zigu.ziguwas.domains.trade.dto.response;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.zigu.ziguwas.domains.item.entity.ItemImage;
import com.zigu.ziguwas.domains.trade.entity.Trade;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.time.LocalDate;

@Getter
@RequiredArgsConstructor
@Builder
@Schema(description = "마이페이지의 받은요청 전용 DTO")
public class MyPageReceiveResDto {

    @Schema(description = "거래 ID", example = "1")
    private final Long tradeId;

    @Schema(description = "아이템 ID", example = "2")
    private final Long itemId;

    @Schema(description = "신청자 닉네임", example = "빌리미123")
    private final String renteeName;

    @Schema(description = "제목", example = "제목입니다")
    private final String title;

    @Schema(description = "빌린기간", example = "30")
    private final Long period;

    @Schema(description = "총 가격", example = "50000")
    private final Long totalPrice;

    @Schema(description = "메인이미지 url", example = "이미지url~~~~~~")
    private final String mainImageUrl;

    @Schema(description = "거래 상태", example = "수락")
    private final String tradeStatus;

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy.MM.dd", timezone = "Asia/Seoul")
    @Schema(description = "요청날짜", example = "2026.03.30")
    private final LocalDate tradeReqDate;

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy.MM.dd", timezone = "Asia/Seoul")
    @Schema(description = "대여 시작일", example = "2026.04.01")
    private final LocalDate tradeStdate;

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy.MM.dd", timezone = "Asia/Seoul")
    @Schema(description = "대여 종료일", example = "2026.04.30")
    private final LocalDate tradeEndate;

    public static MyPageReceiveResDto fromEntity(Trade trade) {
        boolean itemDeleted = trade.getItem() == null;
        return MyPageReceiveResDto.builder()
                .tradeId(trade.getId())
                .itemId(itemDeleted ? null : trade.getItem().getId())
                .renteeName(trade.getRentee().getNickname())
                .title(itemDeleted ? "삭제된 게시글입니다." : trade.getItem().getTitle())
                .period(trade.getPeriod())
                .totalPrice(itemDeleted ? 0L : trade.calculateTotalPrice())
                .mainImageUrl(itemDeleted ? null : trade.getItem().getImageUrl().stream()
                        .filter(ItemImage::isMainImageUrl)
                        .map(ItemImage::getImageUrl)
                        .findFirst()
                        .orElse(null))
                .tradeStatus(trade.getTradeStatus().getDescription())
                .tradeReqDate(trade.getTradeReqdate())
                .tradeStdate(trade.getTradeStdate())
                .tradeEndate(trade.getTradeEndate())
                .build();
    }

}
