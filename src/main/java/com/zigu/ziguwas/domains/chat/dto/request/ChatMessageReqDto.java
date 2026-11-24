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

    @Schema(description = "클라이언트가 만든 메시지 식별자(선택). 같은 ID로 다시 보내면 중복 저장하지 않고 처음 저장된 메시지를 그대로 다시 내려줍니다. 100자를 넘으면 무시합니다.", example = "pending:1732413000000-ab12")
    private String clientMessageId;

    // 길이가 비정상이거나 비어 있으면 식별자를 쓰지 않는 것으로 본다. (중복 제거 키와 echo 로 그대로 쓰이므로 입력 길이를 제한)
    public String resolveClientMessageId() {
        return (clientMessageId != null && !clientMessageId.isBlank() && clientMessageId.length() <= 100)
                ? clientMessageId : null;
    }

    public boolean hasImage() {
        return imageUrl != null && !imageUrl.isEmpty();
    }

    public boolean validateMessage() {
        return (message != null && !message.isEmpty()) || hasImage();
    }
}
