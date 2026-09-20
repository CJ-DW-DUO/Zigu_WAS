package com.zigu.ziguwas.domains.item.dto.request;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.zigu.ziguwas.domains.item.entity.ItemCategory;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Builder;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.time.LocalDate;

@Schema(description = "아이템 등록 수정 정보")
@Getter
@RequiredArgsConstructor
@Builder
public class ItemUpdateReqDto {

    @NotBlank(message = "제목을 입력해 주세요.")
    @Schema(description = "아이템 제목", example = "무소음 키보드")
    @Size(max = 40, message = "제목은 최대 40자까지 입력 가능합니다.")
    private final String title;

    @NotNull(message = "카테고리를 선택해 주세요.")
    @Schema(description = "아이템 카테고리 (대문자 Enum 값)", example = "ELECTRONICS")
    private final ItemCategory category;

    @NotNull(message = "대여 가격을 입력해 주세요.")
    @Schema(description = "1일 대여 가격 (원)", example = "5000")
    private final Long dayPerPrice;

    @NotBlank(message = "물건에 대한 설명을 적어주세요.")
    @Schema(description = "아이템 상세 설명", example = "거의 새 제품입니다. 키스킨 포함해서 대여해 드려요.")
    private final String description;

    @Schema(description = "희망 대여 시작일 (요청글 DEMAND 필수, 공급글은 무시)", example = "2026-10-01")
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd")
    private final LocalDate desiredStartDate;

    @Schema(description = "희망 대여 종료일 (요청글 DEMAND 필수, 공급글은 무시)", example = "2026-10-05")
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd")
    private final LocalDate desiredEndDate;

    @Schema(description = "거래 희망 장소/시간 등 메모 (요청글 전용, 공급글은 무시)", example = "평일 오후 정문 앞에서 거래 원해요")
    @Size(max = 500, message = "메모는 최대 500자까지 입력 가능합니다.")
    private final String memo;
}
