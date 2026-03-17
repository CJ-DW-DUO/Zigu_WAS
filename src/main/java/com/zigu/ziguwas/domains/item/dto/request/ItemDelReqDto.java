package com.zigu.ziguwas.domains.item.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.RequiredArgsConstructor;


import java.util.List;

@Getter
@RequiredArgsConstructor
public class ItemDelReqDto {

    @Schema(description = "삭제할 이미지 ID 리스트", example = "[11, 12, 13]")
    private final List<Long> imageIds;
}
