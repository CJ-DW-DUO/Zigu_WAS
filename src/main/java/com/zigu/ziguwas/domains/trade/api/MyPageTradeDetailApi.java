package com.zigu.ziguwas.domains.trade.api;

import com.zigu.ziguwas.domains.trade.dto.response.MyPageTradeDetailResDto;
import com.zigu.ziguwas.security.CustomUserDetails;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;

@Tag(name = "MyPage Trade Detail API", description = "마이페이지 거래 상세 관련 API")
@RequestMapping(value = "/api/v1/mypage", produces = MediaType.APPLICATION_JSON_VALUE)
public interface MyPageTradeDetailApi {

    @Operation(
            summary = "빌린 물건 상세 내역 조회",
            description = "마이페이지에서 내가 빌린 특정 물건의 상세 내역(대여 일정, 금액, 상태 등)을 조회합니다."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "상세 조회 성공",
                    content = @Content(schema = @Schema(implementation = MyPageTradeDetailResDto.class))
            ),
            @ApiResponse(responseCode = "400", description = "권한 없음 또는 잘못된 요청",
                    content = @Content(examples = @ExampleObject(name = "권한 오류", value = """
                        { "status": 400, "message": "해당 거래에 대한 접근 권한이 없습니다." }
                        """))
            ),
            @ApiResponse(responseCode = "404", description = "거래 정보를 찾을 수 없음",
                    content = @Content(examples = @ExampleObject(name = "조회 실패", value = """
                        { "status": 404, "message": "존재하지 않는 거래 정보입니다." }
                        """))
            )
    })
    @GetMapping("/trade/{tradeId}")
    ResponseEntity<MyPageTradeDetailResDto> getDetailItem(
            @Parameter(description = "조회할 거래 ID", example = "1") @PathVariable Long tradeId,
            @Parameter(hidden = true) @AuthenticationPrincipal CustomUserDetails customUserDetails
    );

    @Operation(
            summary = "빌려준 물건 상세 내역 조회 (임대인용)",
            description = "마이페이지 또는 알림에서 내가 빌려준(빌려줄) 특정 물건의 상세 내역(대여 일정, 금액, 상태 등)을 조회합니다."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "상세 조회 성공",
                    content = @Content(schema = @Schema(implementation = MyPageTradeDetailResDto.class))
            ),
            @ApiResponse(responseCode = "400", description = "권한 없음 또는 잘못된 요청",
                    content = @Content(examples = @ExampleObject(name = "권한 오류", value = """
                        { "status": 400, "message": "해당 거래에 대한 접근 권한이 없습니다." }
                        """))
            ),
            @ApiResponse(responseCode = "404", description = "거래 정보를 찾을 수 없음",
                    content = @Content(examples = @ExampleObject(name = "조회 실패", value = """
                        { "status": 404, "message": "존재하지 않는 거래 정보입니다." }
                        """))
            )
    })
    @GetMapping("/renter/trade/{tradeId}")
    ResponseEntity<MyPageTradeDetailResDto> getDetailItemForRenter(
            @Parameter(description = "조회할 거래 ID", example = "1") @PathVariable Long tradeId,
            @Parameter(hidden = true) @AuthenticationPrincipal CustomUserDetails customUserDetails
    );
}
