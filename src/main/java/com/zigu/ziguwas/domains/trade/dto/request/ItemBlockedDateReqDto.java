package com.zigu.ziguwas.domains.trade.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotEmpty;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.List;

@Getter
@NoArgsConstructor
@Schema(description = "등록자가 차단/해제할 특정 날짜 목록 요청 DTO")
public class ItemBlockedDateReqDto {

    @NotEmpty(message = "차단할 날짜를 하나 이상 선택해 주세요.")
    @Schema(description = "차단(해제)할 날짜 목록", example = "[\"2026-09-24\", \"2026-09-28\"]")
    private List<LocalDate> dates;
}
