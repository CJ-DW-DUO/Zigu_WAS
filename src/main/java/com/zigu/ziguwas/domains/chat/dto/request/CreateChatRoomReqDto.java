package com.zigu.ziguwas.domains.chat.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class CreateChatRoomReqDto {

    @Schema(description = "채팅 대상자 ID", example = "2")
    @NotBlank(message = "채팅 대상자 ID는 입력해야 합니다.")
    @Positive(message = "채팅 대상자 ID는 양수여야 합니다.")
    private Long receiverId;

    @Schema(description = "물품 ID", example = "1")
    @NotBlank(message = "물품 ID는 입력해야 합니다.")
    @Positive(message = "물품 ID는 양수여야 합니다.")
    private Long itemId;
}
