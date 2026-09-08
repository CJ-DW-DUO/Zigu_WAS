package com.zigu.ziguwas.domains.trade.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotEmpty;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.DayOfWeek;
import java.util.List;

@Getter
@NoArgsConstructor
@Schema(description = "등록자가 매주 반복 차단할 요일 목록 요청 DTO")
public class ItemBlockWeekdayReqDto {

    @NotEmpty(message = "차단할 요일을 하나 이상 선택해 주세요.")
    @Schema(description = "매주 반복 차단할 요일 목록", example = "[\"TUESDAY\", \"THURSDAY\"]")
    private List<DayOfWeek> daysOfWeek;
}
