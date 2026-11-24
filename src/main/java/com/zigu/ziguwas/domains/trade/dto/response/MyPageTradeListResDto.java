package com.zigu.ziguwas.domains.trade.dto.response;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.zigu.ziguwas.domains.item.entity.ItemImage;
import com.zigu.ziguwas.domains.trade.entity.Trade;
import com.zigu.ziguwas.domains.user.entity.User;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.time.LocalDate;

@Getter
@RequiredArgsConstructor
@Builder
@Schema(description = "마이페이지의 거래 관련 DTO (받은요청 빼고)")
public class MyPageTradeListResDto {

    @Schema(description = "거래 ID", example = "1")
    private final Long tradeId;

    @Schema(description = "아이템 ID", example = "2")
    private final Long itemId;

    @Schema(description = "게시글 유형 (SUPPLY: 빌려드려요, DEMAND: 빌려주세요). 삭제된 게시글이면 null", example = "SUPPLY")
    private final String postType;

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

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy.MM.dd", timezone = "Asia/Seoul")
    @Schema(description = "요청날짜", example = "2026.03.30")
    private final LocalDate tradeReqDate;

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy.MM.dd", timezone = "Asia/Seoul")
    @Schema(description = "대여 시작일", example = "2026.04.01")
    private final LocalDate tradeStdate;

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy.MM.dd", timezone = "Asia/Seoul")
    @Schema(description = "대여 종료일", example = "2026.04.30")
    private final LocalDate tradeEndate;

    @Schema(description = "거래 상대방 닉네임 (조회자가 임차인이면 임대인, 임대인이면 임차인)", example = "빌려주는사람")
    private final String counterpartNickname;

    @Schema(description = "거래 상대방 userId", example = "7")
    private final Long counterpartId;

    @Schema(description = "상대방과의 채팅방 ID. 아직 채팅방이 없거나 내가 나간 방이면 null", example = "665f1c2e9a3b4d0012ab34cd")
    private final String chatRoomId;

    public static MyPageTradeListResDto fromEntity(Trade trade, Long userId, String chatRoomId) {
        boolean isRentee = trade.getRentee().getId().equals(userId);
        User counterpart = isRentee ? trade.getRenter() : trade.getRentee();
        boolean itemDeleted = trade.getItem() == null;
        return MyPageTradeListResDto.builder()
                .tradeId(trade.getId())
                .itemId(itemDeleted ? null : trade.getItem().getId())
                .postType(itemDeleted ? null : trade.getItem().getPostType().name())
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
                .counterpartNickname(counterpart.getNickname())
                .counterpartId(counterpart.getId())
                .chatRoomId(chatRoomId)
                .build();
    }

}
