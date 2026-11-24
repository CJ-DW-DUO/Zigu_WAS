package com.zigu.ziguwas.domains.trade.dto.response;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.zigu.ziguwas.domains.trade.entity.Trade;
import com.zigu.ziguwas.domains.user.entity.User;
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

    @Schema(description = "거래 ID", example = "1")
    private final Long tradeId;

    @Schema(description = "아이템 ID. 삭제된 게시글이면 null", example = "2")
    private final Long itemId;

    @Schema(description = "게시글 유형 (SUPPLY: 빌려드려요, DEMAND: 빌려주세요). 삭제된 게시글이면 null", example = "SUPPLY")
    private final String postType;

    @Schema(description = "제목", example = "제목입니다!!")
    private final String title;

    @Schema(description = "상대방 닉네임 (조회자가 임차인이면 임대인 닉네임, 임대인이면 임차인 닉네임)", example = "상대방 닉네임")
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

    @Schema(description = "거래 상태 코드 (REQUESTED, IN_PROGRESS, RETURNED, REJECTED). tradeStatus 는 한글 표기", example = "IN_PROGRESS")
    private final String tradeStatusCode;

    @Schema(description = "거래 상대방 userId", example = "7")
    private final Long counterpartId;

    @Schema(description = "상대방과의 채팅방 ID. 아직 채팅방이 없거나 내가 나간 방이면 null", example = "665f1c2e9a3b4d0012ab34cd")
    private final String chatRoomId;

    public static MyPageTradeDetailResDto fromEntity(Trade trade, Long userId, String chatRoomId) {

        String formattedPeriod = String.format("%s ~ %s",
                trade.getTradeStdate().format(DateTimeFormatter.ofPattern("yyyy.M.d")),
                trade.getEndDate().format(DateTimeFormatter.ofPattern("yyyy.M.d"))
        );
        boolean isRentee = trade.getRentee().getId().equals(userId);
        String role = isRentee ? "RENTEE" : "RENTER";
        boolean itemDeleted = trade.getItem() == null;

        // 조회자 본인이 아닌 상대방의 닉네임을 보여줘야 하므로, 역할에 따라 반대편 닉네임을 선택
        User counterpart = isRentee ? trade.getRenter() : trade.getRentee();
        String counterpartNickname = counterpart.getNickname();

        return MyPageTradeDetailResDto.builder()
                .tradeId(trade.getId())
                .itemId(itemDeleted ? null : trade.getItem().getId())
                .postType(itemDeleted ? null : trade.getItem().getPostType().name())
                .title(itemDeleted ? "삭제된 게시글입니다." : trade.getItem().getTitle())
                .nickName(counterpartNickname)
                .tradeStatus(trade.getTradeStatus().getDescription())
                .totalPeriod(formattedPeriod)
                .tradeReqDate(trade.getTradeReqdate())
                .tradeResDate(trade.getTradeResdate())
                .dayPerPrice(itemDeleted ? null : trade.getItem().getDayPerPrice())
                .period(trade.getPeriod())
                .totalPrice(itemDeleted ? 0L : trade.calculateTotalPrice())
                .myRole(role)
                .tradeStatusCode(trade.getTradeStatus().name())
                .counterpartId(counterpart.getId())
                .chatRoomId(chatRoomId)
                .build();
    }

}
