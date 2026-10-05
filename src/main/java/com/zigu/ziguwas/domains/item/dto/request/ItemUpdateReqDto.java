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
import java.util.List;

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

    @Schema(description = "기타 메모 (요청글 전용, 공급글은 무시). 거래 시간/장소는 timeFlexible, preferredHours, tradeLocation 사용", example = "충전기도 같이 빌려주시면 좋아요")
    @Size(max = 500, message = "메모는 최대 500자까지 입력 가능합니다.")
    private final String memo;

    @Schema(description = "거래 희망 시간 무관 여부 (미입력 시 true). true면 preferredHours는 무시", example = "false")
    private final Boolean timeFlexible;

    @Schema(description = "거래 희망 시간 목록 (0~23시). timeFlexible=false일 때 1개 이상 필수, 중복 제거 후 오름차순 저장", example = "[8, 9, 15]")
    @Size(max = 24, message = "거래 희망 시간은 최대 24개까지 선택 가능합니다.")
    private final List<Integer> preferredHours;

    @Schema(description = "거래 희망 장소 (공급글/요청글 공통)", example = "정문 앞 편의점")
    @Size(max = 100, message = "거래 희망 장소는 최대 100자까지 입력 가능합니다.")
    private final String tradeLocation;
}
