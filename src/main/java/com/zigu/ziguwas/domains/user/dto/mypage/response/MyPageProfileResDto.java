package com.zigu.ziguwas.domains.user.dto.mypage.response;

import com.zigu.ziguwas.domains.user.entity.Gender;
import com.zigu.ziguwas.domains.user.entity.User;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
@Getter
@Builder
@Schema(description = "프로필 조회/수정 res DTO")
public class MyPageProfileResDto {

    @Schema(description = "이름(실명)", example = "홍길동")
    private final String nickname;

    @Schema(description = "profile image", example = "http~~~~~")
    private final String profilePhotoUrl;

    @Schema(description = "학번", example = "20261234")
    private final String studentNumber;

    @Schema(description = "성별 (MALE, FEMALE)", example = "MALE")
    private final Gender gender;

    @Schema(description = "프로필에 성별 표시 여부", example = "false")
    private final boolean genderVisible;

    public static MyPageProfileResDto fromEntity(User user){
        return MyPageProfileResDto.builder()
                .nickname(user.getNickname())
                .profilePhotoUrl(user.getProfilePhotoUrl())
                .studentNumber(user.getStudentNumber())
                .gender(user.getGender())
                .genderVisible(user.isGenderVisible())
                .build();

    }
}
