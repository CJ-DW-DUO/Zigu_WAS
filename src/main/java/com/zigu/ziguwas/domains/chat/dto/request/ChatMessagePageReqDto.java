package com.zigu.ziguwas.domains.chat.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class ChatMessagePageReqDto {

    @Schema(description = "페이지 번호", example = "0")
    @NotBlank(message = "페이지 번호는 비울 수 없습니다.")
    private Integer page;

    @Schema(description = "불러오는 페이지 사이즈", example = "10")
    @NotBlank(message = "페이시 사이즈는 비울 수 없습니다.")
    private Integer size;
}
