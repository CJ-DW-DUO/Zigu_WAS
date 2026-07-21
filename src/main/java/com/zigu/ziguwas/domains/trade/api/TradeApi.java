package com.zigu.ziguwas.domains.trade.api;

import com.zigu.ziguwas.domains.trade.dto.request.TradeOfferReqDto;
import com.zigu.ziguwas.security.CustomUserDetails;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;

@Tag(name = "Trade API", description = "거래(대여) 관련 API 입니다.")
public interface TradeApi {

    @Operation(summary = "거래 제안", description = "임차인이 특정 매물에 대해 대여 거래를 제안합니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "거래 제안 성공"),
            @ApiResponse(responseCode = "403", description = "다른 대학교 매물에 대한 거래 제안 시도",
                    content = @Content(mediaType = "application/json", examples = {
                            @ExampleObject(name = "대학 불일치", value = "{\"status\": 403, \"message\": \"해당 대학에 대한 권한이 없습니다.\"}")
                    })),
            @ApiResponse(responseCode = "404", description = "사용자 또는 아이템을 찾을 수 없음",
                    content = @Content(mediaType = "application/json", examples = {
                            @ExampleObject(name = "사용자 없음", value = "{\"status\": 404, \"message\": \"사용자를 찾을 수 없습니다.\"}"),
                            @ExampleObject(name = "아이템 없음", value = "{\"status\": 404, \"message\": \"아이템을 찾을 수 없습니다.\"}")
                    }))
    })
    ResponseEntity<?> tradeOffer(
            @Parameter(hidden = true) @AuthenticationPrincipal CustomUserDetails details,
            @Parameter(description = "대여 제안 요청 정보") @RequestBody TradeOfferReqDto dto
    );

    @Operation(summary = "대여 제안 수락", description = "임대인이 들어온 대여 제안을 수락합니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "제안 수락 성공"),
            @ApiResponse(responseCode = "400", description = "거래 상태가 요청중이 아니거나 매물이 이미 대여중임"),
            @ApiResponse(responseCode = "400", description = "임대인 권한 불일치",
                    content = @Content(mediaType = "application/json", examples = {
                            @ExampleObject(name = "임대인 불일치", value = "{\"status\": 400, \"message\": \"로그인정보와 임대인정보가 일치하지 않습니다.\"}"),
                            @ExampleObject(name = "거래 상태 오류", value = "{\"status\": 400, \"message\": \"거래가 요청상태가 아닙니다.\"}"),
                            @ExampleObject(name = "매물 대여중", value = "{\"status\": 400, \"message\": \"이미 해당 매물은 대여중입니다.\"}")
                    })),
            @ApiResponse(responseCode = "404", description = "거래 정보를 찾을 수 없음",
                    content = @Content(mediaType = "application/json", examples = {
                            @ExampleObject(value = "{\"status\": 404, \"message\": \"해당 거래내역을 찾을 수 없습니다.\"}")
                    }))
    })
    ResponseEntity<?> approveTrade(
            @Parameter(hidden = true) @AuthenticationPrincipal CustomUserDetails details,
            @Parameter(description = "수락할 거래 ID") @PathVariable Long tradeId
    );

    @Operation(summary = "대여 제안 거절", description = "임대인이 들어온 대여 제안을 거절합니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "제안 거절 성공"),
            @ApiResponse(responseCode = "400", description = "거래 상태가 요청중이 아님"),
            @ApiResponse(responseCode = "400", description = "임대인 권한 불일치",
                    content = @Content(mediaType = "application/json", examples = {
                            @ExampleObject(name = "임대인 불일치", value = "{\"status\": 400, \"message\": \"로그인정보와 임대인정보가 일치하지 않습니다.\"}"),
                            @ExampleObject(name = "거래 상태 오류", value = "{\"status\": 400, \"message\": \"거래가 요청상태가 아닙니다.\"}")
                    })),
            @ApiResponse(responseCode = "404", description = "거래 정보를 찾을 수 없음",
                    content = @Content(mediaType = "application/json", examples = {
                            @ExampleObject(value = "{\"status\": 404, \"message\": \"해당 거래내역을 찾을 수 없습니다.\"}")
                    }))
    })
    ResponseEntity<?> rejectTrade(
            @Parameter(hidden = true) @AuthenticationPrincipal CustomUserDetails details,
            @Parameter(description = "거절할 거래 ID") @PathVariable Long tradeId
    );

    @Operation(summary = "대여 반납 확인", description = "임대인이 물건을 반납받았음을 확인하여 거래를 종료합니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "반납 확인 성공"),
            @ApiResponse(responseCode = "400", description = "매물 상태 또는 거래 상태가 올바르지 않음"),
            @ApiResponse(responseCode = "400", description = "권한 또는 상태 검증 실패",
                    content = @Content(mediaType = "application/json", examples = {
                            @ExampleObject(name = "임대인 불일치", value = "{\"status\": 400, \"message\": \"로그인정보와 임대인정보가 일치하지 않습니다.\"}"),
                            @ExampleObject(name = "매물 상태 오류", value = "{\"status\": 400, \"message\": \"해당 매물이 현재 대여중이지 않습니다.\"}"),
                            @ExampleObject(name = "거래 상태 오류", value = "{\"status\": 400, \"message\": \"거래가 요청상태가 아닙니다.\"}")
                    })),
            @ApiResponse(responseCode = "404", description = "거래 정보를 찾을 수 없음",
                    content = @Content(mediaType = "application/json", examples = {
                            @ExampleObject(value = "{\"status\": 404, \"message\": \"해당 거래내역을 찾을 수 없습니다.\"}")
                    }))
    })
    ResponseEntity<?> returnTradeCheck(
            @Parameter(hidden = true) @AuthenticationPrincipal CustomUserDetails details,
            @Parameter(description = "반납 확인할 거래 ID") @PathVariable Long tradeId
    );
}
