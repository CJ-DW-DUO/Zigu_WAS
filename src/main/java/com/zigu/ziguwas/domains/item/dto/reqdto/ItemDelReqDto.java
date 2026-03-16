package com.zigu.ziguwas.domains.item.dto.reqdto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;


import java.util.List;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ItemDelReqDto {

    @Schema(description = "삭제할 이미지 ID 리스트", example = "[11, 12, 13]")
    private List<Long> imageIds;
}
