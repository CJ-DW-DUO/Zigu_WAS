package com.zigu.ziguwas.domains.chat.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;

@Getter
public class ChatMessageReqDto {
    @Schema(description = "채팅메시지", example = "지구야 안녕")
    private String message;

    @Schema(description = "채팅 이미지 URL", example = "https://example.com/image.jpg")
    private String imageUrl;

    public boolean hasImage() {
        return imageUrl != null && !imageUrl.isEmpty();
    }

    public boolean validateMessage() {
        return (message != null && !message.isEmpty()) || hasImage();
    }
}
