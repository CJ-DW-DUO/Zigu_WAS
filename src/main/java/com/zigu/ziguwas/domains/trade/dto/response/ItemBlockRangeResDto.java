package com.zigu.ziguwas.domains.trade.dto.response;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDate;
import java.util.List;

@Getter
@Builder
@Schema(description = "아이템 대여 불가 기간 목록 응답 DTO")
public class ItemBlockRangeResDto {

    @Schema(description = "대여 불가 기간 목록")
    private final List<BlockRangeItem> blockRange;

    @Getter
    @Builder
    public static class BlockRangeItem {

        @Schema(description = "차단 시작일", example = "2026-08-31")
        @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd")
        private final LocalDate startDate;

        @Schema(description = "차단 종료일", example = "2026-09-01")
        @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd")
        private final LocalDate endDate;

        @Schema(description = "차단 출처 (RESERVATION: 승인/진행중 거래, OWNER: 등록자 직접 차단)", example = "RESERVATION")
        private final BlockSource source;
    }
}
