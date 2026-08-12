package com.zigu.ziguwas.domains.chat.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class ChatSendInfoDto {

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
