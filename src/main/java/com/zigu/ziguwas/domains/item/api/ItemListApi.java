package com.zigu.ziguwas.domains.item.api;

import com.zigu.ziguwas.domains.item.dto.response.ItemListResDto;
import com.zigu.ziguwas.domains.item.dto.response.ItemSearchCond;
import com.zigu.ziguwas.security.CustomUserDetails;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;

@Tag(name = "Item List API", description = "아이템 목록 조회 관련 API")
@RequestMapping(value = "/api/v1/items", produces = MediaType.APPLICATION_JSON_VALUE)
public interface ItemListApi {

    @Operation(
            summary = "아이템 목록 페이징 조회",
            description = "카테고리 필터와 정렬(최신순, 인기순, 낮은가격순)을 적용하여 목록을 조회합니다."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "조회 성공",
                    content = @Content(schema = @Schema(implementation = ItemListResDto.class))
            ),
            @ApiResponse(responseCode = "400", description = "인증 실패 또는 권한 오류",
                    content = @Content(examples = {
                            @ExampleObject(name = "인증 실패", value = """
                                { "status": 400, "message": "인증되지 않은 사용자 입니다." }
                                """),
                            @ExampleObject(name = "허용되지 않은 접근", value = """
                                { "status": 400, "message": "허용되지 않은 접근입니다." }
                                """)
                    })
            ),
            @ApiResponse(responseCode = "404", description = "유저 찾을 수 없음",
                    content = @Content(examples = @ExampleObject(value = """
                        { "status": 404, "message": "유저를 찾을 수 없습니다." }
                        """))
            )
    })
    @GetMapping
    ResponseEntity<Page<ItemListResDto>> getItemList(
            @ParameterObject @ModelAttribute ItemSearchCond cond,

            @Parameter(description = "페이징 설정 (page, size)", example = "page=0&size=12")
            @ParameterObject @PageableDefault(size = 12) Pageable pageable,

            @Parameter(hidden = true) @AuthenticationPrincipal CustomUserDetails customUserDetails
    );
}
