package com.zigu.ziguwas.domains.item.dto.response;

import com.zigu.ziguwas.domains.item.entity.Item;
import com.zigu.ziguwas.domains.item.entity.ItemCategory;
import com.zigu.ziguwas.domains.item.entity.ItemImage;
import com.zigu.ziguwas.domains.item.entity.ItemStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.List;

@Getter
@RequiredArgsConstructor
@Builder
public class ItemResDto {

    @Schema(description = "아이템 고유 ID (PK)", example = "1")
    private final Long itemId;

    @Schema(description = "작성자 고유 ID", example = "10")
    private final Long writerId;

    @Schema(description = "본인 게시글 여부", example = "true")
    private final boolean isMine;

    @Schema(description = "아이템 카테고리 (Enum)", example = "ELECTRONICS")
    private final ItemCategory itemCategory;

    @Schema(description = "카테고리 한글 표시명", example = "전자기기")
    private final String categoryName;

    @Schema(description = "아이템 상태", example = "REGISTERED", allowableValues = {"REGISTERED", "RENTING"})
    private final ItemStatus itemStatus;

    @Schema(description = "아이템 상태 한글 표시명", example = "등록됨")
    private final String itemStatusKor;

    @Schema(description = "1일 대여 가격 (원)", example = "5000")
    private final Long dayPerPrice;

    @Schema(description = "아이템을 등록한 사용자의 닉네임", example = "코딩하는지구인")
    private final String nickname;

    @Schema(description = "업로드된 아이템 이미지 고유 ID 목록", example = "[1, 2]")
    private final List<Long> imageIds;

    @Schema(description = "업로드된 아이템 이미지 URL 목록", example = "[\"https://s3.ap-northeast-2.amazonaws.com/bucket/image1.jpg\", \"https://s3.ap-northeast-2.amazonaws.com/bucket/image2.jpg\"]")
    private final List<String> imageUrl;

    @Schema(description = "아이템 제목", example = "로지텍 무소음 무선 키보드")
    private final String title;

    @Schema(description = "아이템 상세 설명", example = "거의 새 제품입니다. 키스킨 포함해서 대여해 드려요.")
    private final String description;

    public static ItemResDto fromEntity(Item item,Long currentUserId) {
        boolean isMine = currentUserId != null && item.getUser().getId().equals(currentUserId);
        List<ItemImage> itemImages = item.getImageUrl().stream().toList();

        return ItemResDto.builder()
                .itemId(item.getId())
                .writerId(item.getUser().getId())
                .isMine(isMine)
                .itemCategory(item.getCategory())
                .categoryName(item.getCategory().getDescription())
                .itemStatus(item.getItemStatus())
                .itemStatusKor(item.getItemStatus().getDescription())
                .dayPerPrice(item.getDayPerPrice())
                .nickname(item.getUser().getNickname())
                .imageIds(itemImages.stream().map(ItemImage::getImageId).toList())
                .imageUrl(itemImages.stream().map(ItemImage::getImageUrl).toList())
                .title(item.getTitle())
                .description(item.getDescription())
                .build();

    }

}
