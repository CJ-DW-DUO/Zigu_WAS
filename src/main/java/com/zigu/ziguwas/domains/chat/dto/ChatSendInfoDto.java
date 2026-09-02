package com.zigu.ziguwas.domains.chat.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class ChatSendInfoDto {

    // 저장된 메시지의 PK. 클라이언트가 실시간 수신분과 목록 조회분을 같은 키로 비교해
    // 중복 표시를 걸러낼 수 있도록 함께 내려준다. (ChatMessageDetailResDto.messageId와 동일한 값)
    @Schema(description = "메시지 ID", example = "68b0f1a2c3d4e5f600000001")
    private String messageId;

    // 서버가 실제로 저장한 시각. 수신 순서가 아니라 이 값으로 정렬해야
    // 목록 조회 결과와 순서가 어긋나지 않는다.
    @Schema(description = "메시지 저장 시각", example = "2026-09-02T10:15:30")
    private String timestamp;

    @Schema(description = "보내는 사람의 ID", example = "1")
    @NotBlank
    private Long senderId;

    @Schema(description = "채팅방 ID", example = "1")
    @NotBlank
    private String chatRoomId;

    @Schema(description = "보내는 사람의 닉네임", example = "지구")
    @NotBlank
    private String senderNickname;

    @Schema(description = "채팅메시지", example = "지구야 안녕")
    private String message;

    @Schema(description = "이미지 URL", example = "https://example.com/image.jpg")
    private String imageUrl;

    public boolean hasImage() {
        return imageUrl != null && !imageUrl.isEmpty();
    }
}
