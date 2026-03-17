package com.zigu.ziguwas.domains.item.dto.request;

import com.zigu.ziguwas.domains.item.entity.Item;
import com.zigu.ziguwas.domains.item.entity.ItemCategory;
import com.zigu.ziguwas.domains.user.entity.User;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Builder;
import lombok.Getter;
import lombok.RequiredArgsConstructor;


@Schema(description = "아이템 등록 요청 정보")
@Getter
@RequiredArgsConstructor
@Builder
public class ItemRegisterReqDto {


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

    public Item toEntity(User user){
        return Item.builder()
                .user(user)
                .title(this.title)
                .category(this.category)
                .dayPerPrice(this.dayPerPrice)
                .description(this.description)
                .build();
    }
}
