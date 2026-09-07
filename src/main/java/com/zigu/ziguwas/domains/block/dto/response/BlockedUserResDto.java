package com.zigu.ziguwas.domains.block.dto.response;

import com.zigu.ziguwas.domains.block.entity.Block;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
@Schema(description = "차단한 사용자 정보 응답 DTO")
public class BlockedUserResDto {

    @Schema(description = "차단당한 사용자 ID", example = "5")
    private final Long userId;

    @Schema(description = "차단당한 사용자 닉네임", example = "홍길동")
    private final String nickname;

    @Schema(description = "차단당한 사용자 프로필 이미지 URL", example = "https://zigu-bucket.s3.amazonaws.com/profile.jpg")
    private final String profilePhotoUrl;

    @Schema(description = "차단한 시각")
    private final LocalDateTime blockedAt;

    public static BlockedUserResDto fromEntity(Block block) {
        return BlockedUserResDto.builder()
                .userId(block.getBlocked().getId())
                .nickname(block.getBlocked().getNickname())
                .profilePhotoUrl(block.getBlocked().getProfilePhotoUrl())
                .blockedAt(block.getCreatedAt())
                .build();
    }
}
