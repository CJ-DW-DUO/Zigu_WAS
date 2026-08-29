package com.zigu.ziguwas.domains.user.dto.auth.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class TokenReissueResDto {

    @Schema(example = "618hdjfnvs3jr1f....")
    private String accessToken;
}
