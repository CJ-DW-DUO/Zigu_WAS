package com.zigu.ziguwas.domains.trade.dto.response;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.zigu.ziguwas.domains.trade.entity.Trade;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

@Getter
@RequiredArgsConstructor
@Builder
@Schema(description = "마이페이지 빌린물건 상세조회 DTO")
public class MyPageTradeDetailResDto {

    @Schema(description = "제목", example = "제목입니다!!")
    private final String title;

    @Schema(description = "닉네임", example = "물건주인 닉네임")
    private final String nickName;

    @Schema(description = "대여상태", example = "대여중")
    private final String tradeStatus;

    @Schema(description = "대여일정" , example = "2026.3.3 ~ 2026.4.2")
    private final String totalPeriod;

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy.M.d", timezone = "Asia/Seoul")
    @Schema(description = "요청날짜", example = "2026.3.2")
    private final LocalDate tradeReqDate;

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy.M.d", timezone = "Asia/Seoul")
    @Schema(description = "수락날짜", example = "2026.3.3")
    private final LocalDate tradeResDate;

    @Schema(description = "대여가격", example = "300")
    private final Long dayPerPrice;

    @Schema(description = "대여기간", example = "30")
    private final Long period;

    @Schema(description = "총 금액", example = "9000")
    private final Long totalPrice;

    @Schema(description = "현재 조회자의 역할", example = "RENTEE")
    private final String myRole;

    public static MyPageTradeDetailResDto fromEntity(Trade trade, Long userId) {

        String formattedPeriod = String.format("%s ~ %s",
                trade.getTradeStdate().format(DateTimeFormatter.ofPattern("yyyy.M.d")),
                trade.getEndDate().format(DateTimeFormatter.ofPattern("yyyy.M.d"))
        );
        String role = trade.getRentee().getId().equals(userId) ? "RENTEE" : "RENTER";
        boolean itemDeleted = trade.getItem() == null;

        return MyPageTradeDetailResDto.builder()
                .title(itemDeleted ? "삭제된 게시글입니다." : trade.getItem().getTitle())
                .nickName(trade.getRenter().getNickname())
                .tradeStatus(trade.getTradeStatus().getDescription())
                .totalPeriod(formattedPeriod)
                .tradeReqDate(trade.getTradeReqdate())
                .tradeResDate(trade.getTradeResdate())
                .dayPerPrice(itemDeleted ? null : trade.getItem().getDayPerPrice())
                .period(trade.getPeriod())
                .totalPrice(itemDeleted ? 0L : trade.calculateTotalPrice())
                .myRole(role)
                .build();
    }

}
