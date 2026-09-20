package com.zigu.ziguwas.domains.trade.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@Schema(description = "요청글에 대한 대여 제안 요청 정보")
public class TradeProposeReqDto {

    @Schema(description = "제안할 요청글(DEMAND)의 아이템ID", example = "1")
    @NotNull(message = "아이템ID는 비울 수 없습니다.")
    private Long itemId;
}
