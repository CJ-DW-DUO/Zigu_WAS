package com.zigu.ziguwas.domains.search.api;

import com.zigu.ziguwas.domains.item.dto.response.ItemListResDto;
import com.zigu.ziguwas.domains.search.dto.request.ItemSearchReqDto;
import com.zigu.ziguwas.domains.search.dto.response.ItemSearchResDto;
import com.zigu.ziguwas.domains.search.dto.response.SearchHistoryResDto;
import com.zigu.ziguwas.security.CustomUserDetails;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.ArraySchema;
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
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "Search API", description = "아이템 검색 및 검색 기록 관리 API")
@RequestMapping(value = "/api/v1/search", produces = MediaType.APPLICATION_JSON_VALUE)
public interface SearchApi {

    @Operation(summary = "아이템 검색", description = "키워드와 카테고리를 이용해 아이템을 페이징 검색하고 검색 기록을 저장합니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "검색 성공",
                    content = @Content(schema = @Schema(implementation = ItemListResDto.class))
            ),
            @ApiResponse(responseCode = "401", description = "인증 실패",
                    content = @Content(examples = @ExampleObject(value = """
                        { "status": 401, "message": "인증되지 않은 사용자 입니다." }
                        """))
            )
    })
    @GetMapping("/item")
    ResponseEntity<Page<ItemSearchResDto>> searchItems(
            @Parameter(hidden = true) @AuthenticationPrincipal CustomUserDetails userDetails,
            @ParameterObject @ModelAttribute ItemSearchReqDto reqDto,
            @Parameter(description = "페이징 설정", example = "page=0&size=12")
            @ParameterObject @PageableDefault(size = 12) Pageable pageable
    );

    @Operation(summary = "검색 기록 조회", description = "사용자의 최근 검색 기록을 최신 활동순으로 최대 20개 조회합니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "조회 성공",
                    content = @Content(array = @ArraySchema(schema = @Schema(implementation = SearchHistoryResDto.class)))
            )
    })
    @GetMapping("/history")
    ResponseEntity<List<SearchHistoryResDto>> getSearchHistory(
            @Parameter(hidden = true) @AuthenticationPrincipal CustomUserDetails userDetails
    );

    @Operation(summary = "검색 기록 전체 삭제", description = "로그인한 사용자의 모든 검색 기록을 삭제합니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "전체 삭제 완료")
    })
    @DeleteMapping("/history")
    ResponseEntity<Void> clearHistory(
            @Parameter(hidden = true) @AuthenticationPrincipal CustomUserDetails userDetails
    );

    @Operation(summary = "특정 검색 기록 삭제", description = "특정 검색 기록 하나를 삭제합니다. 본인의 기록만 삭제 가능합니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "삭제 성공"),
            @ApiResponse(responseCode = "403", description = "권한 없음 (본인의 기록이 아님)",
                    content = @Content(examples = @ExampleObject(value = """
                        { "status": 403, "message": "허용되지 않은 접근입니다." }
                        """))
            ),
            @ApiResponse(responseCode = "404", description = "기록을 찾을 수 없음 (ErrorCode: ITEM_NOT_FOUND 활용)",
                    content = @Content(examples = @ExampleObject(value = """
                        { "status": 404, "message": "아이템을 찾을 수 없습니다." }
                        """))
            )
    })
    @DeleteMapping("/history/{searchId}")
    ResponseEntity<Void> deleteHistoryItem(
            @Parameter(hidden = true) @AuthenticationPrincipal CustomUserDetails userDetails,
            @Parameter(description = "삭제할 검색 기록 ID", required = true) @PathVariable Long searchId
    );
}