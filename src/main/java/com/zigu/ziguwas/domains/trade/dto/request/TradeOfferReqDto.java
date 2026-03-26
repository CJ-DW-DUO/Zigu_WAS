package com.zigu.ziguwas.domains.trade.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class TradeOfferReqDto {

    @Schema(description = "아이템ID", example = "1")
    @NotBlank(message = "아이템ID는 비울 수 없습니다.")
    Long itemId;

    @Schema(description = "대여기간(일)", example = "7")
    @NotBlank(message = "대여기간은 비울 수 없습니다.")
    Long period;

}
