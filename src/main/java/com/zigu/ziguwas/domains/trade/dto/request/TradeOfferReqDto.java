package com.zigu.ziguwas.domains.trade.dto.request;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.time.LocalDate;

@Data
public class TradeOfferReqDto {

    @Schema(description = "아이템ID", example = "1")
    @NotBlank(message = "아이템ID는 비울 수 없습니다.")
    Long itemId;

    @Schema(description = "대여시작 날짜", example = "2026-01-01")
    @NotBlank(message = "대여시작 날짜는 비울 수 없습니다.")
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd")
    private LocalDate startDate;

    @Schema(description = "대여종료 날짜", example = "2026-01-15")
    @NotBlank(message = "대여종료 날짜는 비울 수 없습니다.")
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd")
    private LocalDate endDate;

}
