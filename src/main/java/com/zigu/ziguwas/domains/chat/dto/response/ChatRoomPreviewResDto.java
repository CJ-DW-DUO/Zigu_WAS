package com.zigu.ziguwas.domains.chat.dto.response;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class ChatRoomPreviewResDto {

    // 방 ID
    // 방 ID 는 문자열(UUID)
    private String roomId;

    // 상대 사용자 이름(방이름)
    private String roomName;

    // 마지막 메시지
    private String lastMessage;

    // 마지막 메시지 수신 시간
    private LocalDateTime lastMessageTimestamp;

    // 마지막 메시지 ID (메시지가 없는 방은 null)
    // 클라이언트가 방마다 상세조회를 다시 하지 않고도 새 메시지인지 구분할 수 있도록 함께 내려준다.
    private String lastMessageId;

    // 마지막 메시지를 보낸 사용자 ID (메시지가 없는 방은 null)
    private Long lastSenderId;

    // 채팅방에 연결된 물품 ID
    private Long itemId;

    // 채팅방 상대 사용자 ID (이미 만든 방인지 itemId 와 함께 찾는 용도, 탈퇴한 상대여도 ID는 내려간다)
    private Long receiverId;

    // 물품 제목 (소프트 딜리트된 물품이면 null)
    private String itemTitle;

    // 물품 대표 이미지 URL (이미지가 없거나 삭제된 물품이면 null)
    private String itemImageUrl;
}
