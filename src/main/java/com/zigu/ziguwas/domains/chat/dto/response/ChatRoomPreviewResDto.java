package com.zigu.ziguwas.domains.chat.dto.response;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class ChatRoomPreviewResDto {

    // 방 ID
    private Long roomId;

    // 상대 사용자 이름(방이름)
    private String roomName;

    // 마지막 메시지
    private String lastMessage;

    // 마지막 메시지 수신 시간
    private LocalDateTime lastMessageTimestamp;
}
