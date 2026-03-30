package com.zigu.ziguwas.domains.trade.api;

import com.zigu.ziguwas.domains.trade.dto.response.MyPageReceiveResDto;
import com.zigu.ziguwas.domains.trade.dto.response.MyPageTradeListResDto;
import com.zigu.ziguwas.domains.trade.entity.TradeStatus;
import com.zigu.ziguwas.security.CustomUserDetails;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;

@Tag(name = "MyPage Trade API", description = "마이페이지 거래 관련 API")
@RequestMapping(value = "/api/v1/mypage", produces = MediaType.APPLICATION_JSON_VALUE)
public interface MyPageTradeApi {

    @Operation(
            summary = "대여 중인 물건 조회",
            description = "현재 로그인한 사용자가 빌려서 사용 중인(대여 중) 물건 목록을 조회합니다."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "조회 성공",
                    content = @Content(schema = @Schema(implementation = MyPageTradeListResDto.class))
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
    @GetMapping("/renting")
    ResponseEntity<List<MyPageTradeListResDto>> getRentingItems(
            @Parameter(hidden = true) @AuthenticationPrincipal CustomUserDetails customUserDetails,
            @RequestParam TradeStatus tradeStatus
    );

    @Operation(
            summary = "대여 해준 물건 조회",
            description = "현재 로그인한 사용자가 빌려준 물건 목록을 조회합니다."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "조회 성공",
                    content = @Content(schema = @Schema(implementation = MyPageTradeListResDto.class))
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
    @GetMapping("/renter")
    ResponseEntity<List<MyPageTradeListResDto>> getRenterItems(
            @Parameter(hidden = true) @AuthenticationPrincipal CustomUserDetails customUserDetails
    );

    @Operation(
            summary = "보낸 대여 요청 목록 조회",
            description = "내가 다른 사용자에게 보낸 대여 요청 내역을 조회합니다. 상태 필터링과 페이징이 지원됩니다."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "조회 성공",
                    content = @Content(schema = @Schema(implementation = Page.class))
            ),
            @ApiResponse(responseCode = "400", description = "잘못된 요청 파라미터"),
            @ApiResponse(responseCode = "404", description = "사용자 정보를 찾을 수 없음")
    })
    @GetMapping("/requests/sent")
    ResponseEntity<Page<MyPageTradeListResDto>> getSentRequests(
            @Parameter(hidden = true) @AuthenticationPrincipal CustomUserDetails customUserDetails,

            @Parameter(
                    name = "status",
                    description = "조회할 거래 상태 (REQUESTED: 대기, IN_PROGRESS: 수락/대여중, REJECTED: 거절). 미입력 시 전체 조회",
                    in = ParameterIn.QUERY,
                    schema = @Schema(implementation = TradeStatus.class)
            )
            @RequestParam(required = false) TradeStatus status,

            @Parameter(
                    description = "페이징 및 정렬 설정 (예: page=0&size=10&sort=tradeReqdate,desc)",
                    example = "{\"page\": 0, \"size\": 10, \"sort\": [\"tradeReqdate,desc\"]}"
            )
            Pageable pageable
    );

    @Operation(
            summary = "받은 대여 요청 목록 조회",
            description = "다른 사용자가 나에게 보낸 대여 요청 내역을 조회합니다. 상태 필터링과 페이징이 지원됩니다."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "조회 성공",
                    content = @Content(schema = @Schema(implementation = MyPageReceiveResDto.class))
            ),
            @ApiResponse(responseCode = "400", description = "잘못된 요청 파라미터"),
            @ApiResponse(responseCode = "404", description = "사용자 정보를 찾을 수 없음")
    })
    @GetMapping("/renter/request")
    ResponseEntity<Page<MyPageReceiveResDto>> getReceivedRequests(
            @Parameter(hidden = true) @AuthenticationPrincipal CustomUserDetails customUserDetails,

            @Parameter(
                    name = "status",
                    description = "조회할 거래 상태 (REQUESTED: 대기, IN_PROGRESS: 수락/대여중, REJECTED: 거절). 미입력 시 전체 조회",
                    in = ParameterIn.QUERY,
                    schema = @Schema(implementation = TradeStatus.class)
            )
            @RequestParam(required = false) TradeStatus status,

            @Parameter(
                    description = "페이징 및 정렬 설정 (예: page=0&size=10&sort=tradeReqdate,desc)",
                    example = "{\"page\": 0, \"size\": 10, \"sort\": [\"tradeReqdate,desc\"]}"
            )
            Pageable pageable
    );
}
