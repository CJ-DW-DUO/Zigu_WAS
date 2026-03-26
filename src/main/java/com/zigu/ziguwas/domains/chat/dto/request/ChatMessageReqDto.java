package com.zigu.ziguwas.domains.chat.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class ChatMessageReqDto {

    @Schema(description = "채팅메시지", example = "지구야 안녕")
    @NotBlank(message = "메시지는 비울 수 없습니다.")
    private String message;
}
