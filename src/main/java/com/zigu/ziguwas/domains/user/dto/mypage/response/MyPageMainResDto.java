package com.zigu.ziguwas.domains.user.dto.mypage.response;

import com.zigu.ziguwas.domains.item.entity.Item;
import com.zigu.ziguwas.domains.user.entity.User;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.List;

@Getter
@RequiredArgsConstructor
@Builder
@Schema(description = "MyPage 메인 DTO")
public class MyPageMainResDto {

    @Schema(description = "닉네임", example = "지구헌내기")
    private final String nickname;

    @Schema(description = "학교명", example = "영남대학교")
    private final String univName;

    @Schema(description = "등록된 물건 메인이미지 요약 url", example = "물건 이미지url~~~~")
    private final List<MyItemSummaryResDto> registeredItemsMainImage;

    @Schema(description = "프로필 사진 url", example = "프로필사진url~~~")
    private final String profilePhotoUrl;

    public static MyPageMainResDto fromEntity(User user, List<Item> items) {
        return MyPageMainResDto.builder()
                .nickname(user.getNickname())
                .univName(user.getUniv().getUnivName())
                .registeredItemsMainImage(items.stream()
                    .map(MyItemSummaryResDto::fromEntity)
                    .toList())
                .profilePhotoUrl(user.getProfilePhotoUrl())
                .build();
    }

}
