package com.zigu.ziguwas.domains.chat.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class ChatRecvIdReqDto {

    @Schema(description = "채팅 대상자 ID", example = "2")
    @NotBlank(message = "채팅 대상자 ID는 입력")
    private Long recieverId;
}
