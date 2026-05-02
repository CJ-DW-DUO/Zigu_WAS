package com.zigu.ziguwas.domains.chat.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ChatRoomItemAndTradeInfoResDto {

    // 채팅방 ID
    private Long chatroomId;

    // 물건 ID
    private Long itemId;

    // 물건 게시글 제목
    private String itemTitle;

    // 물건 일일 가격
    private Long itemPrice;

    // 물건 이미지 URL
    private String imageUrl;

    // 사용자 역할 (RENTER, RENTEE)
    private String userRole;

    // 거래 상태
    private String tradeStatus;

}
