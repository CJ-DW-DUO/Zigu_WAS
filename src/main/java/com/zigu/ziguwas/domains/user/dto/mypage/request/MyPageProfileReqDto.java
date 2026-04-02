package com.zigu.ziguwas.domains.user.dto.mypage.request;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@AllArgsConstructor
@NoArgsConstructor
@Getter
@Builder
@Schema(description = "프로필 수정 DTO")
public class MyPageProfileReqDto {

    @Schema(description = "profile nickname 수정", example = "닉넴123")
    private String nickname;


}
