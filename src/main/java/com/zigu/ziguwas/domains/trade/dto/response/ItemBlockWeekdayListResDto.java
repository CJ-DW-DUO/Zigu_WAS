package com.zigu.ziguwas.domains.trade.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;

import java.time.DayOfWeek;
import java.util.List;

@Getter
@Builder
@Schema(description = "등록자가 지정한 매주 반복 차단 요일 목록 응답 DTO")
public class ItemBlockWeekdayListResDto {

    @Schema(description = "매주 반복 차단되는 요일 목록")
    private final List<DayOfWeek> blockedWeekdays;
}
