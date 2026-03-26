package com.zigu.ziguwas.domains.chat.dto.response;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class ChatMessageDetailResDto {

    // 메시지 ID
    private Long messageId;

    // 채팅방 ID
    private Long chatRoomId;

    // 보낸이 ID
    private Long senderId;

    // 보낸이 이름
    private String senderName;

    // 메시지
    private String message;

    // 보낸 시간
    private String timestamp;

    // 이미지 URL
    private String imageUrl;

}
