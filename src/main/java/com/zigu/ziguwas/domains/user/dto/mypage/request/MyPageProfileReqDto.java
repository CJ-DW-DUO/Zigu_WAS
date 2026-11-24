package com.zigu.ziguwas.domains.user.dto.mypage.request;

import com.zigu.ziguwas.domains.user.entity.Gender;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@AllArgsConstructor
@NoArgsConstructor
@Getter
@Builder
@Schema(description = "프로필 수정 DTO (null인 필드는 변경하지 않음)")
public class MyPageProfileReqDto {

    @Schema(description = "이름(실명) 수정", example = "홍길동")
    private String nickname;

    @Schema(description = "학번 (학교가 부여한 학생번호 전체, 숫자 7~10자리). 현재 저장된 학번과 같으면 형식을 검사하지 않고, 바뀌는 경우에만 검사합니다.", example = "20261234")
    private String studentNumber;

    @Schema(description = "성별 (MALE, FEMALE)", example = "MALE")
    private Gender gender;

    @Schema(description = "프로필에 성별 표시 여부", example = "false")
    private Boolean genderVisible;

}
