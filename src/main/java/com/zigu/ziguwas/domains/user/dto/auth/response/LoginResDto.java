package com.zigu.ziguwas.domains.user.dto.auth.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class LoginResDto {

    @Schema(example = "1")
    private Long userId;

    @Schema(example = "zigu@example.com")
    private String email;

    @Schema(example = "지구헌내기")
    private String nickname;

    @Schema(example = "618hdjfnvs3jr1f....")
    private String accessToken;

    @Schema(example = "1fmdivy283yfo1m....")
    private String refreshToken;

    @Schema(example = "한동대학교")
    private String university;
}
