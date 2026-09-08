package com.zigu.ziguwas.domains.trade.dto.response;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDate;
import java.util.List;

@Getter
@Builder
@Schema(description = "등록자가 지정한 차단 날짜 목록 응답 DTO")
public class ItemBlockedDateListResDto {

    @Schema(description = "차단된 날짜 목록")
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd")
    private final List<LocalDate> blockedDates;
}
