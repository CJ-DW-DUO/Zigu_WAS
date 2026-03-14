package com.zigu.ziguwas.domains.item.dto.resdto;

import com.zigu.ziguwas.domains.item.entity.Item;
import com.zigu.ziguwas.domains.item.entity.ItemCategory;
import com.zigu.ziguwas.domains.item.entity.ItemImage;
import lombok.Builder;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.List;

@Getter
@RequiredArgsConstructor
@Builder
public class ItemResDto {

    private final ItemCategory itemCategory;
    private final String categoryName;
    private final Long dayPerPrice;
    private final String nickname;
    private final List<String> imageUrl;
    private final String title;
    private final String description;

    public static ItemResDto fromEntity(Item item) {
        return ItemResDto.builder()
                .itemCategory(item.getCategory())
                .categoryName(item.getCategory().getDescription())
                .dayPerPrice(item.getDayPerPrice())
                .nickname(item.getUser().getNickname())
                .imageUrl(item.getImageUrl().stream().map(ItemImage::getImageUrl).toList())
                .title(item.getTitle())
                .description(item.getDescription())
                .build();

    }

}
