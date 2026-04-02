package com.zigu.ziguwas.domains.user.dto.mypage.response;

import com.zigu.ziguwas.domains.user.entity.User;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
@Getter
@Builder
@Schema(description = "프로필수정 res DTO")
public class MyPageProfileResDto {

    @Schema(description = "profile nickname", example = "닉넴123")
    private final String nickname;

    @Schema(description = "profile image", example = "http~~~~~")
    private final String profilePhotoUrl;

    public static MyPageProfileResDto fromEntity(User user){
        return MyPageProfileResDto.builder()
                .nickname(user.getNickname())
                .profilePhotoUrl(user.getProfilePhotoUrl())
                .build();

    }
}
