package com.zigu.ziguwas.domains.item.dto.reqdto;

import com.zigu.ziguwas.domains.item.entity.Item;
import com.zigu.ziguwas.domains.item.entity.ItemCategory;
import com.zigu.ziguwas.domains.user.entity.User;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Builder;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.List;

@Getter
@RequiredArgsConstructor
@Builder
public class ItemRegisterReqDto {

    @NotEmpty(message = "사진을 한 장 이상 추가해 주세요.")
    @Size(max = 5, message = "사진은 최대 5장까지 등록 가능합니다.")
    private final List<String> imageUrl;

    @NotBlank(message = "제목을 입력해 주세요.")
    @Size(max = 40, message = "제목은 최대 40자까지 입력 가능합니다.")
    private final String title;

    @NotNull(message = "카테고리를 선택해 주세요.")
    private final ItemCategory category;

    @NotNull(message = "대여 가격을 입력해 주세요.")
    private final Long dayPerPrice;

    @NotBlank(message = "물건에 대한 설명을 적어주세요.")
    private final String description;

    public Item toEntity(User user){
        return Item.builder()
                .user(user)
                .title(this.title)
                .category(this.category)
                .dayPerPrice(this.dayPerPrice)
                .description(this.description)
                .mainImageUrl(this.imageUrl.get(0)) // 0번째 이미지를 대표이미지로 세팅
                .build();
    }
}
